/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.pms.pipeline.service.PMSPipelineServiceStepHelper.LIBRARY;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.EntityNotFoundException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidYamlException;
import io.harness.exception.ScmException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.ngexception.beans.yamlschema.YamlSchemaErrorDTO;
import io.harness.exception.ngexception.beans.yamlschema.YamlSchemaErrorWrapperDTO;
import io.harness.git.model.ChangeType;
import io.harness.gitaware.helper.GitAwareContextHelper;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.common.utils.GitEntityFilePath;
import io.harness.gitsync.common.utils.GitSyncFilePathUtils;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.gitsync.scm.EntityObjectIdUtils;
import io.harness.grpc.utils.StringValueUtils;
import io.harness.pms.PmsFeatureFlagService;
import io.harness.pms.contracts.governance.GovernanceMetadata;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.governance.PipelineSaveResponse;
import io.harness.pms.helpers.PipelineCloneHelper;
import io.harness.pms.pipeline.ClonePipelineDTO;
import io.harness.pms.pipeline.CommonStepInfo;
import io.harness.pms.pipeline.ExecutionSummaryInfo;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.PipelineFilterPropertiesDto;
import io.harness.pms.pipeline.PipelineImportRequestDTO;
import io.harness.pms.pipeline.StepCategory;
import io.harness.pms.pipeline.StepPalleteFilterWrapper;
import io.harness.pms.pipeline.StepPalleteInfo;
import io.harness.pms.pipeline.StepPalleteModuleInfo;
import io.harness.pms.pipeline.mappers.PMSPipelineDtoMapper;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.repositories.pipeline.PMSPipelineRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PMSPipelineServiceImpl implements PMSPipelineService {
  @Inject private PMSPipelineRepository pmsPipelineRepository;
  @Inject private PmsSdkInstanceService pmsSdkInstanceService;
  @Inject private PMSPipelineServiceHelper pmsPipelineServiceHelper;
  @Inject private PMSPipelineServiceStepHelper pmsPipelineServiceStepHelper;
  @Inject private GitSyncSdkService gitSyncSdkService;
  @Inject private CommonStepInfo commonStepInfo;
  @Inject private PmsFeatureFlagService pmsFeatureFlagService;
  @Inject private PipelineCloneHelper pipelineCloneHelper;

  public static String CREATING_PIPELINE = "creating new pipeline";
  public static String UPDATING_PIPELINE = "updating existing pipeline";

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Pipeline [%s] under Project[%s], Organization [%s] already exists or has been deleted.";

  @Override
  public PipelineEntity create(PipelineEntity pipelineEntity) {
    try {
      PMSPipelineServiceHelper.validatePresenceOfRequiredFields(pipelineEntity.getAccountId(),
          pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier(),
          pipelineEntity.getIdentifier());

      PipelineEntity entityWithUpdatedInfo = pmsPipelineServiceHelper.updatePipelineInfo(pipelineEntity);
      PipelineEntity createdEntity;
      if (gitSyncSdkService.isGitSyncEnabled(pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(),
              pipelineEntity.getProjectIdentifier())) {
        createdEntity = pmsPipelineRepository.saveForOldGitSync(entityWithUpdatedInfo);
      } else {
        createdEntity = pmsPipelineRepository.save(entityWithUpdatedInfo);
      }
      pmsPipelineServiceHelper.sendPipelineSaveTelemetryEvent(createdEntity, CREATING_PIPELINE);
      return createdEntity;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(format(DUP_KEY_EXP_FORMAT_STRING, pipelineEntity.getIdentifier(),
                                            pipelineEntity.getProjectIdentifier(), pipelineEntity.getOrgIdentifier()),
          USER_SRE, ex);
    } catch (EventsFrameworkDownException ex) {
      log.error("Events framework is down for Pipeline Service.", ex);
      throw new InvalidRequestException("Error connecting to systems upstream", ex);

    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);

    } catch (ExplanationException | HintException | ScmException e) {
      log.error("Error while creating pipeline " + pipelineEntity.getIdentifier(), e);
      throw e;
    } catch (Exception e) {
      log.error(String.format("Error while saving pipeline [%s]", pipelineEntity.getIdentifier()), e);
      throw new InvalidRequestException(String.format(
          "Error while saving pipeline [%s]: %s", pipelineEntity.getIdentifier(), ExceptionUtils.getMessage(e)));
    }
  }

  @Override
  public PipelineSaveResponse clone(ClonePipelineDTO clonePipelineDTO, String accountId) {
    PipelineEntity sourcePipelineEntity = getSourcePipelineEntity(clonePipelineDTO, accountId);

    String sourcePipelineEntityYaml = sourcePipelineEntity.getYaml();

    String destYaml =
        pipelineCloneHelper.updatePipelineMetadataInSourceYaml(clonePipelineDTO, sourcePipelineEntityYaml, accountId);
    PipelineEntity destPipelineEntity =
        PMSPipelineDtoMapper.toPipelineEntity(accountId, clonePipelineDTO.getDestinationConfig().getOrgIdentifier(),
            clonePipelineDTO.getDestinationConfig().getProjectIdentifier(), destYaml);

    boolean isGovernanceEnabled =
        pmsFeatureFlagService.isEnabled(destPipelineEntity.getAccountId(), FeatureName.OPA_PIPELINE_GOVERNANCE);
    GovernanceMetadata destGovernanceMetadata =
        pmsPipelineServiceHelper.validatePipelineYamlAndSetTemplateRefIfAny(destPipelineEntity, isGovernanceEnabled);
    if (destGovernanceMetadata.getDeny()) {
      return PipelineSaveResponse.builder().governanceMetadata(destGovernanceMetadata).build();
    }
    PipelineEntity clonedPipelineEntity = create(destPipelineEntity);

    return PipelineSaveResponse.builder()
        .governanceMetadata(destGovernanceMetadata)
        .identifier(clonedPipelineEntity.getIdentifier())
        .build();
  }

  @NotNull
  private PipelineEntity getSourcePipelineEntity(ClonePipelineDTO clonePipelineDTO, String accountId) {
    Optional<PipelineEntity> sourcePipelineEntity =
        get(accountId, clonePipelineDTO.getSourceConfig().getOrgIdentifier(),
            clonePipelineDTO.getSourceConfig().getProjectIdentifier(),
            clonePipelineDTO.getSourceConfig().getPipelineIdentifier(), false);

    if (!sourcePipelineEntity.isPresent()) {
      log.error(String.format("Pipeline with id [%s] in org [%s] in project [%s] is not present or deleted",
          clonePipelineDTO.getSourceConfig().getPipelineIdentifier(),
          clonePipelineDTO.getSourceConfig().getOrgIdentifier(),
          clonePipelineDTO.getSourceConfig().getProjectIdentifier()));
      throw new InvalidRequestException(
          String.format("Pipeline with id [%s] in org [%s] in project [%s] is not present or deleted",
              clonePipelineDTO.getSourceConfig().getPipelineIdentifier(),
              clonePipelineDTO.getSourceConfig().getOrgIdentifier(),
              clonePipelineDTO.getSourceConfig().getProjectIdentifier()));
    }
    return sourcePipelineEntity.get();
  }

  @Override
  public Optional<PipelineEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean deleted) {
    Optional<PipelineEntity> optionalPipelineEntity =
        getWithoutPerformingValidations(accountId, orgIdentifier, projectIdentifier, identifier, deleted);
    if (!optionalPipelineEntity.isPresent()) {
      throw new EntityNotFoundException(
          PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(orgIdentifier, projectIdentifier, identifier));
    }
    PipelineEntity pipelineEntity = optionalPipelineEntity.get();
    if (pipelineEntity.getStoreType() == null || pipelineEntity.getStoreType() == StoreType.INLINE) {
      return optionalPipelineEntity;
    }
    if (EmptyPredicate.isEmpty(pipelineEntity.getData())) {
      String errorMessage = PipelineCRUDErrorResponse.errorMessageForEmptyYamlOnGit(
          orgIdentifier, projectIdentifier, identifier, GitAwareContextHelper.getBranchInRequest());
      YamlSchemaErrorWrapperDTO errorWrapperDTO =
          YamlSchemaErrorWrapperDTO.builder()
              .schemaErrors(Collections.singletonList(
                  YamlSchemaErrorDTO.builder().message(errorMessage).fqn("$.pipeline").build()))
              .build();
      throw new io.harness.yaml.validator.InvalidYamlException(errorMessage, errorWrapperDTO);
    }
    pmsPipelineServiceHelper.validatePipelineFromRemote(pipelineEntity);
    return optionalPipelineEntity;
  }

  @Override
  public Optional<PipelineEntity> getWithoutPerformingValidations(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean deleted) {
    Optional<PipelineEntity> optionalPipelineEntity;
    try {
      if (gitSyncSdkService.isGitSyncEnabled(accountId, orgIdentifier, projectIdentifier)) {
        optionalPipelineEntity =
            pmsPipelineRepository.findForOldGitSync(accountId, orgIdentifier, projectIdentifier, identifier, !deleted);
      } else {
        optionalPipelineEntity =
            pmsPipelineRepository.find(accountId, orgIdentifier, projectIdentifier, identifier, !deleted, false);
      }
    } catch (ExplanationException | HintException | ScmException e) {
      log.error(String.format("Error while retrieving pipeline [%s]", identifier), e);
      throw e;
    } catch (Exception e) {
      log.error(String.format("Error while retrieving pipeline [%s]", identifier), e);
      throw new InvalidRequestException(
          String.format("Error while retrieving pipeline [%s]: %s", identifier, ExceptionUtils.getMessage(e)));
    }
    if (!optionalPipelineEntity.isPresent()) {
      throw new EntityNotFoundException(
          PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(orgIdentifier, projectIdentifier, identifier));
    }
    return optionalPipelineEntity;
  }

  @Override
  public PipelineEntity updatePipelineYaml(PipelineEntity pipelineEntity, ChangeType changeType) {
    PMSPipelineServiceHelper.validatePresenceOfRequiredFields(pipelineEntity.getAccountId(),
        pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier());
    if (gitSyncSdkService.isGitSyncEnabled(
            pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier())) {
      return updatePipelineForOldGitSync(pipelineEntity, changeType);
    }
    return makePipelineUpdateCall(pipelineEntity, null, changeType, false);
  }

  private PipelineEntity updatePipelineForOldGitSync(PipelineEntity pipelineEntity, ChangeType changeType) {
    if (GitContextHelper.getGitEntityInfo() != null && GitContextHelper.getGitEntityInfo().isNewBranch()) {
      // sending old entity as null here because a new mongo entity will be created. If audit trail needs to be added
      // to git synced projects, a get call needs to be added here to the base branch of this pipeline update
      return makePipelineUpdateCall(pipelineEntity, null, changeType, true);
    }
    Optional<PipelineEntity> optionalOriginalEntity =
        pmsPipelineRepository.findForOldGitSync(pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(),
            pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier(), true);
    if (!optionalOriginalEntity.isPresent()) {
      throw new InvalidRequestException(PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(
          pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier()));
    }
    PipelineEntity entityToUpdate = optionalOriginalEntity.get();
    PipelineEntity tempEntity = entityToUpdate.withYaml(pipelineEntity.getYaml())
                                    .withName(pipelineEntity.getName())
                                    .withDescription(pipelineEntity.getDescription())
                                    .withTags(pipelineEntity.getTags())
                                    .withIsEntityInvalid(false)
                                    .withAllowStageExecutions(pipelineEntity.getAllowStageExecutions());

    return makePipelineUpdateCall(tempEntity, entityToUpdate, changeType, true);
  }

  @Override
  public PipelineEntity syncPipelineEntityWithGit(EntityDetailProtoDTO entityDetail) {
    IdentifierRefProtoDTO identifierRef = entityDetail.getIdentifierRef();
    String accountId = StringValueUtils.getStringFromStringValue(identifierRef.getAccountIdentifier());
    String orgId = StringValueUtils.getStringFromStringValue(identifierRef.getOrgIdentifier());
    String projectId = StringValueUtils.getStringFromStringValue(identifierRef.getProjectIdentifier());
    String pipelineId = StringValueUtils.getStringFromStringValue(identifierRef.getIdentifier());

    Optional<PipelineEntity> optionalPipelineEntity;
    try (PmsGitSyncBranchContextGuard ignored = new PmsGitSyncBranchContextGuard(null, false)) {
      optionalPipelineEntity = get(accountId, orgId, projectId, pipelineId, false);
    }
    if (!optionalPipelineEntity.isPresent()) {
      throw new InvalidRequestException(
          PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(orgId, projectId, pipelineId));
    }
    // Non Git synced Pipelines are being marked as INLINE. Marking storeType as null here so that pipelines in old git
    // sync don't have any value for storeType.
    return makePipelineUpdateCall(
        optionalPipelineEntity.get().withStoreType(null), optionalPipelineEntity.get(), ChangeType.ADD, true);
  }

  private PipelineEntity makePipelineUpdateCall(
      PipelineEntity pipelineEntity, PipelineEntity oldEntity, ChangeType changeType, boolean isOldFlow) {
    try {
      PipelineEntity entityWithUpdatedInfo = pmsPipelineServiceHelper.updatePipelineInfo(pipelineEntity);
      PipelineEntity updatedResult;
      if (isOldFlow) {
        updatedResult =
            pmsPipelineRepository.updatePipelineYamlForOldGitSync(entityWithUpdatedInfo, oldEntity, changeType);
      } else {
        updatedResult = pmsPipelineRepository.updatePipelineYaml(entityWithUpdatedInfo);
      }

      if (updatedResult == null) {
        throw new InvalidRequestException(format(
            "Pipeline [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
            pipelineEntity.getIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getOrgIdentifier()));
      }

      pmsPipelineServiceHelper.sendPipelineSaveTelemetryEvent(updatedResult, UPDATING_PIPELINE);
      return updatedResult;
    } catch (EventsFrameworkDownException ex) {
      log.error("Events framework is down for Pipeline Service.", ex);
      throw new InvalidRequestException("Error connecting to systems upstream", ex);
    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
    } catch (ExplanationException | HintException | ScmException e) {
      log.error("Error while updating pipeline " + pipelineEntity.getIdentifier(), e);
      throw e;
    } catch (Exception e) {
      log.error(String.format("Error while updating pipeline [%s]", pipelineEntity.getIdentifier()), e);
      throw new InvalidRequestException(String.format(
          "Error while updating pipeline [%s]: %s", pipelineEntity.getIdentifier(), ExceptionUtils.getMessage(e)));
    }
  }

  @Override
  public PipelineEntity updatePipelineMetadata(
      String accountId, String orgIdentifier, String projectIdentifier, Criteria criteria, Update updateOperations) {
    return pmsPipelineRepository.updatePipelineMetadata(
        accountId, orgIdentifier, projectIdentifier, criteria, updateOperations);
  }

  @Override
  public void saveExecutionInfo(
      String accountId, String orgId, String projectId, String pipelineId, ExecutionSummaryInfo executionSummaryInfo) {
    Criteria criteria =
        PMSPipelineServiceHelper.getPipelineEqualityCriteria(accountId, orgId, projectId, pipelineId, false, null);

    Update update = new Update();
    update.set(PipelineEntityKeys.executionSummaryInfo, executionSummaryInfo);
    updatePipelineMetadata(accountId, orgId, projectId, criteria, update);
  }

  @Override
  public boolean markEntityInvalid(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, String invalidYaml) {
    Optional<PipelineEntity> optionalPipelineEntity =
        get(accountIdentifier, orgIdentifier, projectIdentifier, identifier, false);
    if (!optionalPipelineEntity.isPresent()) {
      log.warn(String.format(
          "Marking pipeline [%s] as invalid failed as it does not exist or has been deleted", identifier));
      return false;
    }
    PipelineEntity existingPipeline = optionalPipelineEntity.get();
    PipelineEntity pipelineEntityUpdated = existingPipeline.withYaml(invalidYaml)
                                               .withObjectIdOfYaml(EntityObjectIdUtils.getObjectIdOfYaml(invalidYaml))
                                               .withIsEntityInvalid(true);
    pmsPipelineRepository.updatePipelineYamlForOldGitSync(pipelineEntityUpdated, existingPipeline, ChangeType.NONE);
    return true;
  }

  @Override
  public boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, Long version) {
    if (gitSyncSdkService.isGitSyncEnabled(accountId, orgIdentifier, projectIdentifier)) {
      return deleteForOldGitSync(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);
    }
    PipelineEntity deletedEntity =
        pmsPipelineRepository.delete(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);
    if (deletedEntity.getDeleted()) {
      return true;
    } else {
      throw new InvalidRequestException(
          format("Pipeline [%s] under Project[%s], Organization [%s] could not be deleted.", pipelineIdentifier,
              projectIdentifier, orgIdentifier));
    }
  }

  private boolean deleteForOldGitSync(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    Optional<PipelineEntity> optionalPipelineEntity =
        pmsPipelineRepository.findForOldGitSync(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, true);
    if (!optionalPipelineEntity.isPresent()) {
      throw new InvalidRequestException(PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(
          orgIdentifier, projectIdentifier, pipelineIdentifier));
    }

    PipelineEntity existingEntity = optionalPipelineEntity.get();
    PipelineEntity withDeleted = existingEntity.withDeleted(true);
    try {
      PipelineEntity deletedEntity = pmsPipelineRepository.deleteForOldGitSync(withDeleted);
      if (deletedEntity.getDeleted()) {
        return true;
      } else {
        throw new InvalidRequestException(
            format("Pipeline [%s] under Project[%s], Organization [%s] could not be deleted.", pipelineIdentifier,
                projectIdentifier, orgIdentifier));
      }
    } catch (Exception e) {
      log.error(String.format("Error while deleting pipeline [%s]", pipelineIdentifier), e);
      throw new InvalidRequestException(
          String.format("Error while deleting pipeline [%s]: %s", pipelineIdentifier, ExceptionUtils.getMessage(e)));
    }
  }

  @Override
  public Page<PipelineEntity> list(Criteria criteria, Pageable pageable, String accountId, String orgIdentifier,
      String projectIdentifier, Boolean getDistinctFromBranches) {
    if (Boolean.TRUE.equals(getDistinctFromBranches)
        && gitSyncSdkService.isGitSyncEnabled(accountId, orgIdentifier, projectIdentifier)) {
      return pmsPipelineRepository.findAll(criteria, pageable, accountId, orgIdentifier, projectIdentifier, true);
    }
    return pmsPipelineRepository.findAll(criteria, pageable, accountId, orgIdentifier, projectIdentifier, false);
  }

  @Override
  public String importPipelineFromRemote(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, PipelineImportRequestDTO pipelineImportRequest) {
    String importedPipeline =
        pmsPipelineRepository.importPipelineFromRemote(accountId, orgIdentifier, projectIdentifier);
    if (EmptyPredicate.isEmpty(importedPipeline)) {
      String errorMessage = PipelineCRUDErrorResponse.errorMessageForEmptyYamlOnGit(
          orgIdentifier, projectIdentifier, pipelineIdentifier, GitAwareContextHelper.getBranchInRequest());
      throw PMSPipelineServiceHelper.buildInvalidYamlException(errorMessage, importedPipeline);
    }
    YamlField pipelineYamlField;
    try {
      pipelineYamlField = YamlUtils.readTree(importedPipeline);
    } catch (IOException e) {
      String errorMessage = PipelineCRUDErrorResponse.errorMessageForNotAYAMLFile(
          GitAwareContextHelper.getBranchInRequest(), GitAwareContextHelper.getFilepathInRequest());
      throw PMSPipelineServiceHelper.buildInvalidYamlException(errorMessage, importedPipeline);
    }
    YamlField pipelineInnerField = pipelineYamlField.getNode().getField(YAMLFieldNameConstants.PIPELINE);
    if (pipelineInnerField == null) {
      String errorMessage = PipelineCRUDErrorResponse.errorMessageForNotAPipelineYAML(
          GitAwareContextHelper.getBranchInRequest(), GitAwareContextHelper.getFilepathInRequest());
      throw PMSPipelineServiceHelper.buildInvalidYamlException(errorMessage, importedPipeline);
    }
    String identifierFromGit = pipelineInnerField.getNode().getIdentifier();
    if (!pipelineIdentifier.equals(identifierFromGit)) {
      String errorMessage = PipelineCRUDErrorResponse.errorMessageForInvalidField(
          YAMLFieldNameConstants.IDENTIFIER, identifierFromGit, pipelineIdentifier);
      throw PMSPipelineServiceHelper.buildInvalidYamlException(errorMessage, importedPipeline);
    }

    String nameFromGit = pipelineInnerField.getNode().getName();
    if (!pipelineImportRequest.getPipelineName().equals(nameFromGit)) {
      String errorMessage = PipelineCRUDErrorResponse.errorMessageForInvalidField(
          YAMLFieldNameConstants.NAME, nameFromGit, pipelineImportRequest.getPipelineName());
      throw PMSPipelineServiceHelper.buildInvalidYamlException(errorMessage, importedPipeline);
    }
    if (EmptyPredicate.isNotEmpty(pipelineImportRequest.getPipelineDescription())) {
      YamlUtils.setStringValueForField(
          YAMLFieldNameConstants.DESCRIPTION, pipelineImportRequest.getPipelineDescription(), pipelineInnerField);
      try {
        importedPipeline = YamlUtils.writeYamlString(pipelineYamlField).replace("---\n", "");
      } catch (IOException e) {
        throw new UnexpectedException("Unexpected error when trying to set description");
      }
    }

    PipelineEntity pipelineEntity =
        PMSPipelineDtoMapper.toPipelineEntity(accountId, orgIdentifier, projectIdentifier, importedPipeline);
    pmsPipelineServiceHelper.validatePipelineFromRemote(pipelineEntity);
    return importedPipeline;
  }

  @Override
  public Long countAllPipelines(Criteria criteria) {
    return pmsPipelineRepository.countAllPipelines(criteria);
  }

  @Override
  public StepCategory getSteps(String module, String category, String accountId) {
    Map<String, StepPalleteInfo> serviceInstanceNameToSupportedSteps =
        pmsSdkInstanceService.getModuleNameToStepPalleteInfo();
    StepCategory stepCategory = pmsPipelineServiceStepHelper.calculateStepsForModuleBasedOnCategory(
        category, serviceInstanceNameToSupportedSteps.get(module).getStepTypes(), accountId);
    for (Map.Entry<String, StepPalleteInfo> entry : serviceInstanceNameToSupportedSteps.entrySet()) {
      if (entry.getKey().equals(module) || EmptyPredicate.isEmpty(entry.getValue().getStepTypes())) {
        continue;
      }
      stepCategory.addStepCategory(pmsPipelineServiceStepHelper.calculateStepsForCategory(
          entry.getValue().getModuleName(), entry.getValue().getStepTypes(), accountId));
    }
    return stepCategory;
  }

  @Override
  public StepCategory getStepsV2(String accountId, StepPalleteFilterWrapper stepPalleteFilterWrapper) {
    Map<String, StepPalleteInfo> serviceInstanceNameToSupportedSteps =
        pmsSdkInstanceService.getModuleNameToStepPalleteInfo();
    if (stepPalleteFilterWrapper.getStepPalleteModuleInfos().isEmpty()) {
      // Return all the steps.
      return pmsPipelineServiceStepHelper.getAllSteps(accountId, serviceInstanceNameToSupportedSteps);
    }
    StepCategory stepCategory = StepCategory.builder().name(LIBRARY).build();
    for (StepPalleteModuleInfo request : stepPalleteFilterWrapper.getStepPalleteModuleInfos()) {
      String module = request.getModule();
      String category = request.getCategory();
      StepPalleteInfo stepPalleteInfo = serviceInstanceNameToSupportedSteps.get(module);
      if (stepPalleteInfo == null) {
        continue;
      }
      List<StepInfo> stepInfoList = stepPalleteInfo.getStepTypes();
      String displayModuleName = stepPalleteInfo.getModuleName();
      if (EmptyPredicate.isEmpty(stepInfoList)) {
        continue;
      }
      StepCategory moduleCategory;
      if (EmptyPredicate.isNotEmpty(category)) {
        moduleCategory = pmsPipelineServiceStepHelper.calculateStepsForModuleBasedOnCategoryV2(
            displayModuleName, category, stepInfoList, accountId);
      } else {
        moduleCategory =
            pmsPipelineServiceStepHelper.calculateStepsForCategory(displayModuleName, stepInfoList, accountId);
      }
      stepCategory.addStepCategory(moduleCategory);
      if (request.isShouldShowCommonSteps()) {
        pmsPipelineServiceStepHelper.addStepsToStepCategory(
            moduleCategory, commonStepInfo.getCommonSteps(request.getCommonStepCategory()), accountId);
      }
    }

    return stepCategory;
  }

  // Todo: Remove only if there are no references to the pipeline
  @Override
  public boolean deleteAllPipelinesInAProject(String accountId, String orgId, String projectId) {
    Criteria criteria = pmsPipelineServiceHelper.formCriteria(
        accountId, orgId, projectId, null, PipelineFilterPropertiesDto.builder().build(), false, null, null);
    Pageable pageRequest = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, PipelineEntityKeys.lastUpdatedAt));

    Page<PipelineEntity> pipelineEntities =
        pmsPipelineRepository.findAll(criteria, pageRequest, accountId, orgId, projectId, false);
    boolean isOldGitSyncEnabled = gitSyncSdkService.isGitSyncEnabled(accountId, orgId, projectId);
    for (PipelineEntity pipelineEntity : pipelineEntities) {
      if (isOldGitSyncEnabled) {
        pmsPipelineRepository.deleteForOldGitSync(pipelineEntity.withDeleted(true));
      } else {
        pmsPipelineRepository.delete(accountId, orgId, projectId, pipelineEntity.getIdentifier());
      }
    }
    return true;
  }

  @Override
  public String fetchExpandedPipelineJSON(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    Optional<PipelineEntity> pipelineEntityOptional =
        get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!pipelineEntityOptional.isPresent()) {
      throw new InvalidRequestException(PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(
          orgIdentifier, projectIdentifier, pipelineIdentifier));
    }

    return pmsPipelineServiceHelper.fetchExpandedPipelineJSONFromYaml(
        accountId, orgIdentifier, projectIdentifier, pipelineEntityOptional.get().getYaml());
  }

  @Override
  public PipelineEntity updateGitFilePath(PipelineEntity pipelineEntity, String newFilePath) {
    Criteria criteria = PMSPipelineServiceHelper.getPipelineEqualityCriteria(pipelineEntity.getAccountId(),
        pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier(), false,
        null);

    GitEntityFilePath gitEntityFilePath = GitSyncFilePathUtils.getRootFolderAndFilePath(newFilePath);
    Update update = new Update()
                        .set(PipelineEntityKeys.filePath, gitEntityFilePath.getFilePath())
                        .set(PipelineEntityKeys.rootFolder, gitEntityFilePath.getRootFolder());
    return updatePipelineMetadata(pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(),
        pipelineEntity.getProjectIdentifier(), criteria, update);
  }
}
