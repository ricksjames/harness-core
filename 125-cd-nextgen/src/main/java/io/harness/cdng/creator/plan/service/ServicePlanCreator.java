/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.configfile.ConfigFileWrapper;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.creator.plan.environment.EnvironmentPlanCreatorHelper;
import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.cdng.creator.plan.stage.DeploymentStageNode;
import io.harness.cdng.infra.steps.InfraSectionStepParameters;
import io.harness.cdng.licenserestriction.EnforcementValidator;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.cdng.service.beans.ServiceYaml;
import io.harness.cdng.service.steps.ServiceConfigStep;
import io.harness.cdng.service.steps.ServiceConfigStepParameters;
import io.harness.cdng.service.steps.ServiceStep;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.cdng.utilities.ArtifactsUtility;
import io.harness.cdng.utilities.ConfigFileUtility;
import io.harness.cdng.utilities.ManifestsUtility;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class ServicePlanCreator extends ChildrenPlanCreator<ServiceConfig> {
  @Inject EnforcementValidator enforcementValidator;
  @Inject KryoSerializer kryoSerializer;

  @Override
  public Class<ServiceConfig> getFieldClass() {
    return ServiceConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YamlTypes.SERVICE_CONFIG, Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  public String addDependenciesForArtifacts(PlanCreationContext ctx,
      LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap, ServiceConfig actualServiceConfig) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();
    boolean isUseFromStage = actualServiceConfig.getUseFromStage() != null;
    YamlField artifactYamlField =
        ArtifactsUtility.fetchArtifactYamlFieldAndSetYamlUpdates(ctx.getCurrentField(), isUseFromStage, yamlUpdates);
    String artifactsPlanNodeId = UUIDGenerator.generateUuid();

    Map<String, ByteString> metadataDependency = prepareMetadata(artifactsPlanNodeId, actualServiceConfig);

    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(artifactsPlanNodeId, artifactYamlField);
    PlanCreationResponseBuilder artifactPlanCreationResponse = PlanCreationResponse.builder().dependencies(
        DependenciesUtils.toDependenciesProto(dependenciesMap)
            .toBuilder()
            .putDependencyMetadata(
                artifactsPlanNodeId, Dependency.newBuilder().putAllMetadata(metadataDependency).build())
            .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      artifactPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(artifactsPlanNodeId, artifactPlanCreationResponse.build());
    return artifactsPlanNodeId;
  }

  public String addDependenciesForManifests(PlanCreationContext ctx,
      LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap, ServiceConfig actualServiceConfig) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();
    boolean isUseFromStage = actualServiceConfig.getUseFromStage() != null;
    YamlField manifestsYamlField =
        ManifestsUtility.fetchManifestsYamlFieldAndSetYamlUpdates(ctx.getCurrentField(), isUseFromStage, yamlUpdates);
    String manifestsPlanNodeId = "manifests-" + UUIDGenerator.generateUuid();

    Map<String, ByteString> metadataDependency = prepareMetadata(manifestsPlanNodeId, actualServiceConfig);

    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(manifestsPlanNodeId, manifestsYamlField);
    PlanCreationResponseBuilder manifestsPlanCreationResponse = PlanCreationResponse.builder().dependencies(
        DependenciesUtils.toDependenciesProto(dependenciesMap)
            .toBuilder()
            .putDependencyMetadata(
                manifestsPlanNodeId, Dependency.newBuilder().putAllMetadata(metadataDependency).build())
            .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      manifestsPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(manifestsPlanNodeId, manifestsPlanCreationResponse.build());
    return manifestsPlanNodeId;
  }

  public String addDependenciesForConfigFiles(PlanCreationContext ctx,
      LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap, ServiceConfig actualServiceConfig) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();
    boolean isUseFromStage = actualServiceConfig.getUseFromStage() != null;
    YamlField configFilesYamlField = ConfigFileUtility.fetchConfigFilesYamlFieldAndSetYamlUpdates(
        ctx.getCurrentField(), isUseFromStage, yamlUpdates);
    String configFilesPlanNodeId = "configFiles-" + UUIDGenerator.generateUuid();

    Map<String, ByteString> metadataDependency = prepareMetadata(configFilesPlanNodeId, actualServiceConfig);

    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(configFilesPlanNodeId, configFilesYamlField);
    PlanCreationResponseBuilder configFilesPlanCreationResponse = PlanCreationResponse.builder().dependencies(
        DependenciesUtils.toDependenciesProto(dependenciesMap)
            .toBuilder()
            .putDependencyMetadata(
                configFilesPlanNodeId, Dependency.newBuilder().putAllMetadata(metadataDependency).build())
            .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      configFilesPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(configFilesPlanNodeId, configFilesPlanCreationResponse.build());
    return configFilesPlanNodeId;
  }

  /*
  TODO: currently we are using many yaml updates. For ex - if we do not have service definition and we need to call plan
  creators for either of artifacts or manifests we are using yamlUpdates which contains dummy artifact and manifests
  yaml node. The best way is to pre calculate the overridesets and override stage and create the resolved
  serviceDefinition and do the yaml updates in service plan creator.
   */

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, ServiceConfig serviceConfig) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();

    // enforcement validator
    enforcementValidator.validate(ctx.getMetadata().getAccountIdentifier(), ctx.getMetadata().getOrgIdentifier(),
        ctx.getMetadata().getProjectIdentifier(), ctx.getMetadata().getMetadata().getPipelineIdentifier(),
        ctx.getYaml(), ctx.getMetadata().getMetadata().getExecutionUuid());

    YamlField serviceConfigField = ctx.getCurrentField();

    YamlField serviceDefField = serviceConfigField.getNode().getField(YamlTypes.SERVICE_DEFINITION);
    if (serviceDefField == null || EmptyPredicate.isEmpty(serviceDefField.getNode().getUuid())) {
      throw new InvalidRequestException("ServiceDefinition node is invalid.");
    }

    planCreationResponseMap.put(serviceDefField.getNode().getUuid(),
        PlanCreationResponse.builder()
            .dependencies(getDependenciesForServiceDefinitionNode(
                serviceDefField, ctx.getDependency().getMetadataMap().get(YamlTypes.ENVIRONMENT_NODE_ID)))
            .build());

    // Fetching infraSectionParameters dependency
    ByteString infraSectionStepParams =
        ctx.getDependency().getMetadataMap().get(YamlTypes.INFRASTRUCTURE_STEP_PARAMETERS);
    InfraSectionStepParameters infraSectionStepParameters =
        (InfraSectionStepParameters) kryoSerializer.asInflatedObject(infraSectionStepParams.toByteArray());

    YamlNode serviceNode = serviceConfigField.getNode();
    String serviceConfigNodeId = serviceNode.getUuid();

    // Environment uuid
    String envNodeUuid = (String) kryoSerializer.asInflatedObject(
        ctx.getDependency().getMetadataMap().get(YamlTypes.ENVIRONMENT_NODE_ID).toByteArray());

    addEnvironmentStepNode(infraSectionStepParameters, planCreationResponseMap, kryoSerializer,
        serviceConfig.getServiceDefinition().getServiceSpec().getUuid(), envNodeUuid);
    addServiceNode(
        serviceConfig, planCreationResponseMap, serviceConfigNodeId, serviceConfig.getServiceDefinition().getUuid());
    if (shouldCreatePlanNodeForConfigFiles(actualServiceConfig)) {
      String configFilesPlanNodeId = addDependenciesForConfigFiles(ctx, planCreationResponseMap, actualServiceConfig);
      serviceSpecChildrenIds.add(configFilesPlanNodeId);
    }

    String serviceConfigNodeId = serviceNode.getUuid();
    String serviceDefinitionNodeId = addServiceDefinitionNode(actualServiceConfig, planCreationResponseMap,
        serviceConfigNodeId, serviceSpecChildrenIds, infraSectionStepParameters);
    addServiceNode(actualServiceConfig, planCreationResponseMap, serviceConfigNodeId, serviceDefinitionNodeId);

    return planCreationResponseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, ServiceConfig serviceConfig, List<String> childrenNodeIds) {
    YamlField serviceField = ctx.getCurrentField();
    YamlNode serviceNode = serviceField.getNode();

    String serviceConfigNodeId = serviceField.getNode().getUuid();
    String serviceNodeUuId = "service-" + serviceConfigNodeId;
    ServiceConfigStepParameters serviceConfigStepParameters = ServiceConfigStepParameters.builder()
                                                                  .useFromStage(serviceConfig.getUseFromStage())
                                                                  .serviceRef(serviceConfig.getServiceRef())
                                                                  .childNodeId(serviceNodeUuId)
                                                                  .build();
    return PlanNode.builder()
        .uuid(serviceConfigNodeId)
        .stepType(ServiceConfigStep.STEP_TYPE)
        .name(PlanCreatorConstants.SERVICE_NODE_NAME)
        .identifier(YamlTypes.SERVICE_CONFIG)
        .stepParameters(serviceConfigStepParameters)
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                .build())
        .adviserObtainments(getAdviserObtainmentFromMetaData(serviceNode))
        .skipExpressionChain(false)
        .build();
  }

  public Dependencies getDependenciesForServiceDefinitionNode(YamlField serviceDefField, ByteString envNodeId) {
    Map<String, YamlField> serviceDefYamlFieldMap = new HashMap<>();
    String serviceDefNodeUuid = serviceDefField.getNode().getUuid();
    serviceDefYamlFieldMap.put(serviceDefNodeUuid, serviceDefField);

    Map<String, ByteString> serviceDefDependencyMap = new HashMap<>();
    serviceDefDependencyMap.put(YamlTypes.ENVIRONMENT_NODE_ID, envNodeId);

    Dependency serviceDefDependency = Dependency.newBuilder().putAllMetadata(serviceDefDependencyMap).build();
    return DependenciesUtils.toDependenciesProto(serviceDefYamlFieldMap)
        .toBuilder()
        .putDependencyMetadata(serviceDefNodeUuid, serviceDefDependency)
        .build();
  }

  public boolean shouldCreatePlanNodeForConfigFiles(ServiceConfig actualServiceConfig) {
    List<ConfigFileWrapper> configFiles = actualServiceConfig.getServiceDefinition().getServiceSpec().getConfigFiles();

    if (EmptyPredicate.isNotEmpty(configFiles)) {
      return true;
    }

    return actualServiceConfig.getStageOverrides() != null
        && actualServiceConfig.getStageOverrides().getConfigFiles() != null
        && EmptyPredicate.isNotEmpty(actualServiceConfig.getStageOverrides().getConfigFiles());
  }

  private String addServiceNode(ServiceConfig actualServiceConfig,
      LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap, String serviceConfigNodeId,
      String serviceDefinitionNodeId) {
    ServiceStepParameters stepParameters = ServiceStepParameters.fromServiceConfig(actualServiceConfig);
    PlanNode node =
        PlanNode.builder()
            .uuid("service-" + serviceConfigNodeId)
            .stepType(ServiceStep.STEP_TYPE)
            .name(PlanCreatorConstants.SERVICE_NODE_NAME)
            .identifier(YamlTypes.SERVICE_ENTITY)
            .stepParameters(stepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                    .build())
            .adviserObtainment(
                AdviserObtainment.newBuilder()
                    .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                    .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                        OnSuccessAdviserParameters.builder().nextNodeId(serviceDefinitionNodeId).build())))
                    .build())
            .skipExpressionChain(false)
            .skipGraphType(SkipType.SKIP_TREE)
            .build();
    planCreationResponseMap.put(node.getUuid(), PlanCreationResponse.builder().node(node.getUuid(), node).build());
    return node.getUuid();
  }

  private String addEnvironmentStepNode(InfraSectionStepParameters infraSectionStepParameters,
      LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap, KryoSerializer kryoSerializer,
      String serviceSpecNodeUuid, String envNodeUuid) {
    ByteString advisorParameters = ByteString.copyFrom(
        kryoSerializer.asBytes(OnSuccessAdviserParameters.builder().nextNodeId(serviceSpecNodeUuid).build()));
    PlanNode node =
        EnvironmentPlanCreatorHelper.getPlanNode(envNodeUuid, infraSectionStepParameters, advisorParameters);
    planCreationResponseMap.put(node.getUuid(), PlanCreationResponse.builder().node(node.getUuid(), node).build());
    return node.getUuid();
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(YamlNode currentNode) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    if (currentNode != null) {
      YamlField siblingField = currentNode.nextSiblingNodeFromParentObject("infrastructure");
      if (siblingField != null && siblingField.getNode().getUuid() != null) {
        adviserObtainments.add(
            AdviserObtainment.newBuilder()
                .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                    OnSuccessAdviserParameters.builder().nextNodeId(siblingField.getNode().getUuid()).build())))
                .build());
      }
    }
    return adviserObtainments;
  }

  private ServiceConfig applyUseFromStageOverrides(ServiceConfig actualServiceConfig) {
    ServiceYaml actualServiceYaml = actualServiceConfig.getService();
    if (actualServiceYaml != null && EmptyPredicate.isEmpty(actualServiceYaml.getName())) {
      actualServiceYaml.setName(actualServiceYaml.getIdentifier());
    }

    // Apply useFromStage overrides.
    ServiceConfig serviceOverrides;
    if (actualServiceConfig.getUseFromStage() != null) {
      ServiceUseFromStage.Overrides overrides = actualServiceConfig.getUseFromStage().getOverrides();
      if (overrides != null) {
        ServiceYaml overriddenEntity =
            ServiceYaml.builder().name(overrides.getName().getValue()).description(overrides.getDescription()).build();
        serviceOverrides = ServiceConfig.builder().service(overriddenEntity).build();
        actualServiceConfig = actualServiceConfig.applyOverrides(serviceOverrides);
      }
    }
    return actualServiceConfig;
  }

  /** Method returns actual Service object by resolving useFromStage if present. */
  public ServiceConfig getActualServiceConfig(ServiceConfig serviceConfig, YamlField serviceField) {
    if (serviceConfig.getUseFromStage() == null) {
      if (serviceConfig.getServiceDefinition() == null) {
        throw new InvalidArgumentsException(
            "Either Service Definition or useFromStage should be present in the given stage");
      }
      return serviceConfig;
    }

    if (serviceConfig.getServiceDefinition() != null) {
      throw new InvalidArgumentsException("Service definition should not exist along with useFromStage");
    }

    String stage = serviceConfig.getUseFromStage().getStage();
    if (stage == null) {
      throw new InvalidRequestException("Stage identifier not present in useFromStage");
    }

    try {
      //  Add validation for not chaining of stages
      DeploymentStageNode stageElementConfig = YamlUtils.read(
          PlanCreatorUtils.getStageConfig(serviceField, stage).getNode().toString(), DeploymentStageNode.class);
      DeploymentStageConfig deploymentStage = stageElementConfig.getDeploymentStageConfig();
      if (deploymentStage != null) {
        return applyUseFromStageOverrides(serviceConfig.applyUseFromStage(deploymentStage.getServiceConfig()));
      } else {
        throw new InvalidArgumentsException("Stage identifier given in useFromStage doesn't exist");
      }
    } catch (IOException ex) {
      throw new InvalidRequestException("Cannot parse stage: " + stage);
    }
  }
}