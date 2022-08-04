/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.integrationstage;

import static io.harness.beans.serializer.RunTimeInputHandler.UNRESOLVED_PARAMETER;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveIntegerParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveMapParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameterWithDefaultValue;
import static io.harness.ci.commonconstants.CIExecutionConstants.CPU;
import static io.harness.ci.commonconstants.CIExecutionConstants.DEFAULT_CONTAINER_CPU_POV;
import static io.harness.ci.commonconstants.CIExecutionConstants.DEFAULT_CONTAINER_MEM_POV;
import static io.harness.ci.commonconstants.CIExecutionConstants.DRONE_WORKSPACE;
import static io.harness.ci.commonconstants.CIExecutionConstants.MEMORY;
import static io.harness.ci.commonconstants.CIExecutionConstants.STEP_PREFIX;
import static io.harness.ci.commonconstants.CIExecutionConstants.STEP_REQUEST_MEMORY_MIB;
import static io.harness.ci.commonconstants.CIExecutionConstants.STEP_REQUEST_MILLI_CPU;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.environment.pod.container.ContainerImageDetails;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.quantity.unit.DecimalQuantityUnit;
import io.harness.beans.quantity.unit.StorageQuantityUnit;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.beans.stages.IntegrationStageConfigImpl;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.stepinfo.InitializeStepInfo;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.RunTestsStepInfo;
import io.harness.beans.sweepingoutputs.StageInfraDetails;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.OSType;
import io.harness.ci.buildstate.CodebaseUtils;
import io.harness.ci.buildstate.ConnectorUtils;
import io.harness.ci.buildstate.PluginSettingUtils;
import io.harness.ci.buildstate.StepContainerUtils;
import io.harness.ci.execution.CIExecutionConfigService;
import io.harness.ci.ff.CIFeatureFlagService;
import io.harness.ci.utils.CIStepInfoUtils;
import io.harness.ci.utils.PortFinder;
import io.harness.ci.utils.QuantityUtils;
import io.harness.delegate.beans.ci.pod.CIContainerType;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ContainerResourceParams;
import io.harness.delegate.beans.ci.pod.EnvVariableEnum;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.filters.WithConnectorRef;
import io.harness.ng.core.NGAccess;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.plancreator.steps.StepGroupElementConfig;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.yaml.YamlUtils;
import io.harness.steps.matrix.StrategyExpansionData;
import io.harness.utils.TimeoutUtils;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.SecretNGVariable;
import io.harness.yaml.core.variables.StringNGVariable;
import io.harness.yaml.extended.ci.container.ContainerResource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.utils.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class K8InitializeStepUtils {
  @Inject private CIExecutionConfigService ciExecutionConfigService;
  @Inject private CIFeatureFlagService featureFlagService;
  @Inject private ConnectorUtils connectorUtils;
  @Inject private CodebaseUtils codebaseUtils;
  private final String AXA_ACCOUNT_ID = "UVxMDMhNQxOCvroqqImWdQ";

  public List<ContainerDefinitionInfo> createStepContainerDefinitions(List<ExecutionWrapperConfig> steps,
      StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, String accountId,
      OSType os, Ambiance ambiance) {
    List<ContainerDefinitionInfo> containerDefinitionInfos = new ArrayList<>();
    if (steps == null) {
      return containerDefinitionInfos;
    }

    Integer stageMemoryRequest = getStageMemoryRequest(steps, accountId);
    Integer stageCpuRequest = getStageCpuRequest(steps, accountId);

    int stepIndex = 0;
    for (ExecutionWrapperConfig executionWrapper : steps) {
      if (executionWrapper.getStep() != null && !executionWrapper.getStep().isNull()) {
        StepElementConfig stepElementConfig = IntegrationStageUtils.getStepElementConfig(executionWrapper);
        stepIndex++;
        if (stepElementConfig.getTimeout() != null && stepElementConfig.getTimeout().isExpression()) {
          throw new InvalidRequestException(
              "Timeout field must be resolved in step: " + stepElementConfig.getIdentifier());
        }

        ContainerResource containerResource = getContainerResource(stepElementConfig);
        Integer extraMemoryPerStep = Math.max(
            0, stageMemoryRequest - getContainerMemoryLimit(containerResource, "stepType", "stepId", accountId));
        Integer extraCPUPerStep =
            Math.max(0, stageCpuRequest - getContainerCpuLimit(containerResource, "stepType", "stepId", accountId));
        ContainerDefinitionInfo containerDefinitionInfo =
            createStepContainerDefinition(stepElementConfig, integrationStage, ciExecutionArgs, portFinder, stepIndex,
                accountId, os, ambiance, extraMemoryPerStep, extraCPUPerStep);
        if (containerDefinitionInfo != null) {
          containerDefinitionInfos.add(containerDefinitionInfo);
        }
      } else if (executionWrapper.getParallel() != null && !executionWrapper.getParallel().isNull()) {
        ParallelStepElementConfig parallelStepElementConfig =
            IntegrationStageUtils.getParallelStepElementConfig(executionWrapper);
        if (isNotEmpty(parallelStepElementConfig.getSections())) {
          Integer extraMemoryPerStep =
              calculateExtraMemoryForParallelStep(parallelStepElementConfig, accountId, stageMemoryRequest);
          Integer extraCPUPerStep =
              calculateExtraCPUForParallelStep(parallelStepElementConfig, accountId, stageCpuRequest);
          for (ExecutionWrapperConfig executionWrapperInParallel : parallelStepElementConfig.getSections()) {
            if (executionWrapperInParallel.getStep() == null || executionWrapperInParallel.getStep().isNull()) {
              continue;
            }

            stepIndex++;
            StepElementConfig stepElementConfig =
                IntegrationStageUtils.getStepElementConfig(executionWrapperInParallel);
            ContainerDefinitionInfo containerDefinitionInfo =
                createStepContainerDefinition(stepElementConfig, integrationStage, ciExecutionArgs, portFinder,
                    stepIndex, accountId, os, ambiance, extraMemoryPerStep, extraCPUPerStep);
            if (containerDefinitionInfo != null) {
              containerDefinitionInfos.add(containerDefinitionInfo);
            }
          }
        }
      }
    }
    return containerDefinitionInfos;
  }

  public List<ContainerDefinitionInfo> createStepContainerDefinitionsStepGroupWithFF(
      InitializeStepInfo initializeStepInfo, StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs,
      PortFinder portFinder, String accountId, OSType os, Ambiance ambiance, int stepIndex) {
    List<ExecutionWrapperConfig> steps = initializeStepInfo.getExecutionElementConfig().getSteps();
    List<ContainerDefinitionInfo> containerDefinitionInfos = new ArrayList<>();
    if (steps == null) {
      return containerDefinitionInfos;
    }

    Pair<Integer, Integer> wrapperRequests = getStageRequest(initializeStepInfo, accountId);
    Integer stageCpuRequest = wrapperRequests.getLeft();
    Integer stageMemoryRequest = wrapperRequests.getRight();

    for (ExecutionWrapperConfig executionWrapper : steps) {
      if (executionWrapper.getStep() != null && !executionWrapper.getStep().isNull()) {
        stepIndex++;
        ContainerDefinitionInfo containerDefinitionInfo = handleSingleStep(executionWrapper, integrationStage,
            ciExecutionArgs, portFinder, accountId, os, ambiance, stageMemoryRequest, stageCpuRequest, stepIndex,  null);
        if (containerDefinitionInfo != null) {
          containerDefinitionInfos.add(containerDefinitionInfo);
        }
      } else if (executionWrapper.getParallel() != null && !executionWrapper.getParallel().isNull()) {
        Integer extraMemory = calculateExtraMemory(executionWrapper, accountId, stageMemoryRequest);
        Integer extraCPU = calculateExtraCPU(executionWrapper, accountId, stageCpuRequest);
        List<ContainerDefinitionInfo> parallelDefinitionInfos = handleParallelStep(executionWrapper, integrationStage,
            ciExecutionArgs, portFinder, accountId, os, ambiance, extraMemory, extraCPU, stepIndex, null);
        if (parallelDefinitionInfos != null) {
          stepIndex += parallelDefinitionInfos.size();
          if (parallelDefinitionInfos.size() > 0) {
            containerDefinitionInfos.addAll(parallelDefinitionInfos);
          }
        }
      } else if (executionWrapper.getStepGroup() != null && !executionWrapper.getStepGroup().isNull()) {
        List<ContainerDefinitionInfo> stepGroupDefinitionInfos = handleStepGroup(executionWrapper, integrationStage,
            ciExecutionArgs, portFinder, accountId, os, ambiance, stageMemoryRequest, stageCpuRequest, stepIndex);
        if (stepGroupDefinitionInfos != null) {
          stepIndex += stepGroupDefinitionInfos.size();
          if (stepGroupDefinitionInfos.size() > 0) {
            containerDefinitionInfos.addAll(stepGroupDefinitionInfos);
          }
        }
      } else {
        throw new InvalidRequestException("Only Parallel, StepElement and StepGroup are supported");
      }
    }
    return containerDefinitionInfos;
  }

  private ContainerDefinitionInfo handleSingleStep(ExecutionWrapperConfig executionWrapper,
      StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, String accountId,
      OSType os, Ambiance ambiance, int maxAllocatableMemoryRequest, int maxAllocatableCpuRequest, int stepIndex,
      String stepGroupIdOfParent) {
    StepElementConfig stepElementConfig = IntegrationStageUtils.getStepElementConfig(executionWrapper);
    if (Strings.isNotBlank(stepGroupIdOfParent)) {
      stepElementConfig.setIdentifier(stepGroupIdOfParent + "_" + stepElementConfig.getIdentifier());
    }

    Integer extraMemoryPerStep = 0;
    Integer extraCPUPerStep = 0;

    if (stepElementConfig.getStrategy() == null) {
      extraMemoryPerStep = calculateExtraMemory(executionWrapper, accountId, maxAllocatableMemoryRequest);
      extraCPUPerStep = calculateExtraCPU(executionWrapper, accountId, maxAllocatableCpuRequest);
    }

    return createStepContainerDefinition(stepElementConfig, integrationStage, ciExecutionArgs, portFinder, stepIndex,
        accountId, os, ambiance, extraMemoryPerStep, extraCPUPerStep);
  }

  private List<ContainerDefinitionInfo> handleStepGroup(ExecutionWrapperConfig executionWrapper,
      StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, String accountId,
      OSType os, Ambiance ambiance, int maxAllocatableMemoryRequest, int maxAllocatableCpuRequest, int stepIndex) {
    List<ContainerDefinitionInfo> containerDefinitionInfos = new ArrayList<>();
    StepGroupElementConfig stepGroupElementConfig = IntegrationStageUtils.getStepGroupElementConfig(executionWrapper);
    if (isEmpty(stepGroupElementConfig.getSteps())) {
      return containerDefinitionInfos;
    }

    for (ExecutionWrapperConfig step : stepGroupElementConfig.getSteps()) {
      if (step.getStep() != null && !step.getStep().isNull()) {
        stepIndex++;
        ContainerDefinitionInfo containerDefinitionInfo = handleSingleStep(step, integrationStage, ciExecutionArgs,
            portFinder, accountId, os, ambiance,  maxAllocatableMemoryRequest, maxAllocatableCpuRequest, stepIndex,
            stepGroupElementConfig.getIdentifier());
        if (containerDefinitionInfo != null) {
          containerDefinitionInfos.add(containerDefinitionInfo);
        }
      } else if (step.getParallel() != null && !step.getParallel().isNull()) {
        int extraMemory = calculateExtraMemory(step, accountId, maxAllocatableMemoryRequest);
        int extraCpu = calculateExtraCPU(step, accountId, maxAllocatableCpuRequest);
        List<ContainerDefinitionInfo> parallelStepDefinitionInfos =
            handleParallelStep(step, integrationStage, ciExecutionArgs, portFinder, accountId, os, ambiance,
               extraMemory, extraCpu, stepIndex, stepGroupElementConfig.getIdentifier());
        if (parallelStepDefinitionInfos != null) {
          stepIndex += parallelStepDefinitionInfos.size();
          if (parallelStepDefinitionInfos.size() > 0) {
            containerDefinitionInfos.addAll(parallelStepDefinitionInfos);
          }
        }
      }
    }
    return containerDefinitionInfos;
  }

  private List<ContainerDefinitionInfo> handleParallelStep(ExecutionWrapperConfig executionWrapper,
      StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, String accountId,
      OSType os, Ambiance ambiance, int extraMemory, int extraCPU, int stepIndex, String stepGroupIdOfParent) {
    List<ContainerDefinitionInfo> containerDefinitionInfos = new ArrayList<>();
    ParallelStepElementConfig parallelStepElementConfig =
        IntegrationStageUtils.getParallelStepElementConfig(executionWrapper);
    if (isEmpty(parallelStepElementConfig.getSections())) {
      return containerDefinitionInfos;
    }

    int steps = parallelStepElementConfig.getSections().size();
    Integer extraMemoryPerStep = extraMemory / steps;
    Integer extraCPUPerStep = extraCPU / steps;

    for (ExecutionWrapperConfig executionWrapperInParallel : parallelStepElementConfig.getSections()) {
      if (executionWrapperInParallel.getStep() != null && !executionWrapperInParallel.getStep().isNull()) {
        StepElementConfig stepElementConfig = IntegrationStageUtils.getStepElementConfig(executionWrapperInParallel);
        stepIndex++;
        ContainerDefinitionInfo containerDefinitionInfo =
            handleSingleStep(executionWrapperInParallel, integrationStage, ciExecutionArgs, portFinder, accountId, os,
                ambiance, extraMemoryPerStep + getExecutionWrapperMemoryRequest(executionWrapperInParallel, accountId),
                extraCPUPerStep + getExecutionWrapperCpuRequest(executionWrapperInParallel, accountId), stepIndex,
                stepGroupIdOfParent);
        if (containerDefinitionInfo != null) {
          containerDefinitionInfos.add(containerDefinitionInfo);
        }
      } else if (executionWrapperInParallel.getStepGroup() != null
          && !executionWrapperInParallel.getStepGroup().isNull()) {
        List<ContainerDefinitionInfo> stepGroupDefinitionInfos =
            handleStepGroup(executionWrapperInParallel, integrationStage, ciExecutionArgs, portFinder, accountId, os,
                ambiance, extraMemoryPerStep + getExecutionWrapperMemoryRequest(executionWrapperInParallel, accountId),
                extraCPUPerStep + getExecutionWrapperCpuRequest(executionWrapperInParallel, accountId), stepIndex);
        if (stepGroupDefinitionInfos != null) {
          stepIndex += stepGroupDefinitionInfos.size();
          if (stepGroupDefinitionInfos.size() > 0) {
            containerDefinitionInfos.addAll(stepGroupDefinitionInfos);
          }
        }
      }
    }

    return containerDefinitionInfos;
  }

  private ContainerDefinitionInfo createStepContainerDefinition(StepElementConfig stepElement,
      StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex,
      String accountId, OSType os, Ambiance ambiance, Integer extraMemoryPerStep, Integer extraCPUPerStep) {
    if (!(stepElement.getStepSpecType() instanceof CIStepInfo)) {
      return null;
    }

    CIStepInfo ciStepInfo = (CIStepInfo) stepElement.getStepSpecType();
    validateStepType(ciStepInfo.getNonYamlInfo().getStepInfoType(), os);

    long timeout = TimeoutUtils.getTimeoutInSeconds(stepElement.getTimeout(), ciStepInfo.getDefaultTimeout());
    switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
      case RUN:
        return createRunStepContainerDefinition((RunStepInfo) ciStepInfo, integrationStage, ciExecutionArgs, portFinder,
            stepIndex, stepElement.getIdentifier(), stepElement.getName(), accountId, os, extraMemoryPerStep,
            extraCPUPerStep);
      case DOCKER:
      case ECR:
      case ACR:
      case GCR:
      case SAVE_CACHE_S3:
      case RESTORE_CACHE_S3:
      case RESTORE_CACHE_GCS:
      case SAVE_CACHE_GCS:
      case SECURITY:
      case UPLOAD_ARTIFACTORY:
      case UPLOAD_S3:
      case UPLOAD_GCS:
        return createPluginCompatibleStepContainerDefinition((PluginCompatibleStep) ciStepInfo, integrationStage,
            ciExecutionArgs, portFinder, stepIndex, stepElement.getIdentifier(), stepElement.getName(),
            stepElement.getType(), timeout, accountId, os, extraMemoryPerStep, extraCPUPerStep);
      case GIT_CLONE:
        return createGitCloneStepContainerDefinition((GitCloneStepInfo) ciStepInfo, integrationStage, ciExecutionArgs,
                portFinder, stepIndex, stepElement.getIdentifier(), stepElement.getName(), accountId, os, ambiance,
                extraMemoryPerStep, extraCPUPerStep);
      case PLUGIN:
        return createPluginStepContainerDefinition((PluginStepInfo) ciStepInfo, integrationStage, ciExecutionArgs,
            portFinder, stepIndex, stepElement.getIdentifier(), stepElement.getName(), accountId, os,
            extraMemoryPerStep, extraCPUPerStep);
      case RUN_TESTS:
        return createRunTestsStepContainerDefinition((RunTestsStepInfo) ciStepInfo, integrationStage, ciExecutionArgs,
            portFinder, stepIndex, stepElement.getIdentifier(), accountId, os, extraMemoryPerStep, extraCPUPerStep);
      default:
        return null;
    }
  }

  public void validateStepType(CIStepInfoType stepType, OSType os) {
    if (os != OSType.Windows) {
      return;
    }

    switch (stepType) {
      case DOCKER:
      case ECR:
      case ACR:
      case GCR:
        throw new CIStageExecutionException(format("%s step not allowed in windows kubernetes builds", stepType));
      default:
        return;
    }
  }

  private ContainerDefinitionInfo createPluginCompatibleStepContainerDefinition(PluginCompatibleStep stepInfo,
      StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex,
      String identifier, String stepName, String stepType, long timeout, String accountId, OSType os,
      Integer extraMemoryPerStep, Integer extraCPUPerStep) {
    Integer port = portFinder.getNextPort();

    String containerName = format("%s%d", STEP_PREFIX, stepIndex);
    Map<String, String> envVarMap = new HashMap<>();
    envVarMap.putAll(getEnvVariables(integrationStage));
    envVarMap.putAll(BuildEnvironmentUtils.getBuildEnvironmentVariables(ciExecutionArgs));
    envVarMap.putAll(
        PluginSettingUtils.getPluginCompatibleEnvVariables(stepInfo, identifier, timeout, StageInfraDetails.Type.K8));
    setEnvVariablesForHostedBuids(integrationStage, stepInfo, envVarMap);

    Integer runAsUser = resolveIntegerParameter(stepInfo.getRunAsUser(), null);

    Boolean privileged = null;
    if (CIStepInfoUtils.getPrivilegedMode(stepInfo) != null) {
      privileged = CIStepInfoUtils.getPrivilegedMode(stepInfo).getValue();
    }
    return ContainerDefinitionInfo.builder()
        .name(containerName)
        .commands(StepContainerUtils.getCommand(os))
        .args(StepContainerUtils.getArguments(port))
        .envVars(envVarMap)
        .secretVariables(getSecretVariables(integrationStage))
        .containerImageDetails(
            ContainerImageDetails.builder()
                .imageDetails(IntegrationStageUtils.getImageInfo(CIStepInfoUtils.getPluginCustomStepImage(
                    stepInfo, ciExecutionConfigService, StageInfraDetails.Type.K8, accountId)))
                .build())
        .isHarnessManagedImage(true)
        .containerResourceParams(getStepContainerResource(
            stepInfo.getResources(), stepType, identifier, accountId, extraMemoryPerStep, extraCPUPerStep))
        .ports(Arrays.asList(port))
        .containerType(CIContainerType.PLUGIN)
        .stepIdentifier(identifier)
        .stepName(stepName)
        .imagePullPolicy(RunTimeInputHandler.resolveImagePullPolicy(CIStepInfoUtils.getImagePullPolicy(stepInfo)))
        .privileged(privileged)
        .runAsUser(runAsUser)
        .build();
  }

  private void setEnvVariablesForHostedBuids(
      StageElementConfig integrationStage, PluginCompatibleStep stepInfo, Map<String, String> envVarMap) {
    IntegrationStageConfigImpl stage = (IntegrationStageConfigImpl) integrationStage.getStageType();
    if (stage != null && stage.getInfrastructure() != null
        && stage.getInfrastructure().getType() == Infrastructure.Type.KUBERNETES_HOSTED) {
      switch (stepInfo.getNonYamlInfo().getStepInfoType()) {
        case ECR:
        case ACR:
        case GCR:
        case DOCKER:
          envVarMap.put("container", "docker");
          break;
        default:
          break;
      }
    }
  }

  private ContainerDefinitionInfo createRunStepContainerDefinition(RunStepInfo runStepInfo,
      StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex,
      String identifier, String name, String accountId, OSType os, Integer extraMemoryPerStep,
      Integer extraCPUPerStep) {
    if (runStepInfo.getImage() == null) {
      throw new CIStageExecutionException("image can't be empty in k8s infrastructure");
    }

    if (runStepInfo.getConnectorRef() == null) {
      throw new CIStageExecutionException("connector ref can't be empty in k8s infrastructure");
    }

    Integer port = portFinder.getNextPort();

    String containerName = format("%s%d", STEP_PREFIX, stepIndex);
    Map<String, String> stepEnvVars = new HashMap<>();
    stepEnvVars.putAll(getEnvVariables(integrationStage));
    stepEnvVars.putAll(BuildEnvironmentUtils.getBuildEnvironmentVariables(ciExecutionArgs));
    Map<String, String> envvars =
        resolveMapParameter("envVariables", "Run", identifier, runStepInfo.getEnvVariables(), false);
    if (!isEmpty(envvars)) {
      stepEnvVars.putAll(envvars);
    }
    Integer runAsUser = resolveIntegerParameter(runStepInfo.getRunAsUser(), null);

    return ContainerDefinitionInfo.builder()
        .name(containerName)
        .commands(StepContainerUtils.getCommand(os))
        .args(StepContainerUtils.getArguments(port))
        .envVars(stepEnvVars)
        .stepIdentifier(identifier)
        .secretVariables(getSecretVariables(integrationStage))
        .containerImageDetails(ContainerImageDetails.builder()
                                   .imageDetails(IntegrationStageUtils.getImageInfo(resolveStringParameter(
                                       "Image", "Run", identifier, runStepInfo.getImage(), true)))
                                   .connectorIdentifier(resolveStringParameter(
                                       "connectorRef", "Run", identifier, runStepInfo.getConnectorRef(), true))
                                   .build())
        .containerResourceParams(getStepContainerResource(
            runStepInfo.getResources(), "Run", identifier, accountId, extraMemoryPerStep, extraCPUPerStep))
        .ports(Arrays.asList(port))
        .containerType(CIContainerType.RUN)
        .stepName(name)
        .privileged(runStepInfo.getPrivileged().getValue())
        .runAsUser(runAsUser)
        .imagePullPolicy(RunTimeInputHandler.resolveImagePullPolicy(runStepInfo.getImagePullPolicy()))
        .build();
  }

  private ContainerDefinitionInfo createRunTestsStepContainerDefinition(RunTestsStepInfo runTestsStepInfo,
      StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex,
      String identifier, String accountId, OSType os, Integer extraMemoryPerStep, Integer extraCPUPerStep) {
    Integer port = portFinder.getNextPort();

    if (runTestsStepInfo.getImage() == null) {
      throw new CIStageExecutionException("image can't be empty in k8s infrastructure");
    }

    if (runTestsStepInfo.getConnectorRef() == null) {
      throw new CIStageExecutionException("connector ref can't be empty in k8s infrastructure");
    }

    String containerName = format("%s%d", STEP_PREFIX, stepIndex);
    Map<String, String> stepEnvVars = new HashMap<>();
    stepEnvVars.putAll(getEnvVariables(integrationStage));
    stepEnvVars.putAll(BuildEnvironmentUtils.getBuildEnvironmentVariables(ciExecutionArgs));
    Map<String, String> envvars =
        resolveMapParameter("envVariables", "RunTests", identifier, runTestsStepInfo.getEnvVariables(), false);
    if (!isEmpty(envvars)) {
      stepEnvVars.putAll(envvars);
    }
    Integer runAsUser = resolveIntegerParameter(runTestsStepInfo.getRunAsUser(), null);

    return ContainerDefinitionInfo.builder()
        .name(containerName)
        .commands(StepContainerUtils.getCommand(os))
        .args(StepContainerUtils.getArguments(port))
        .envVars(stepEnvVars)
        .stepIdentifier(identifier)
        .secretVariables(getSecretVariables(integrationStage))
        .containerImageDetails(ContainerImageDetails.builder()
                                   .imageDetails(IntegrationStageUtils.getImageInfo(resolveStringParameter(
                                       "Image", "RunTest", identifier, runTestsStepInfo.getImage(), true)))
                                   .connectorIdentifier(resolveStringParameter(
                                       "connectorRef", "RunTest", identifier, runTestsStepInfo.getConnectorRef(), true))
                                   .build())
        .containerResourceParams(getStepContainerResource(
            runTestsStepInfo.getResources(), "RunTests", identifier, accountId, extraMemoryPerStep, extraCPUPerStep))
        .ports(Arrays.asList(port))
        .containerType(CIContainerType.TEST_INTELLIGENCE)
        .privileged(runTestsStepInfo.getPrivileged().getValue())
        .runAsUser(runAsUser)
        .imagePullPolicy(RunTimeInputHandler.resolveImagePullPolicy(runTestsStepInfo.getImagePullPolicy()))
        .build();
  }

  private ContainerDefinitionInfo createGitCloneStepContainerDefinition(GitCloneStepInfo gitCloneStepInfo,
      StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex,
      String identifier, String name, String accountId, OSType os, Ambiance ambiance, Integer extraMemoryPerStep,
      Integer extraCPUPerStep) {

    //Create a PluginStepInfo from the GitCloneStepInfo
    PluginStepInfo pluginStepInfo = InitializeStepUtils.createPluginStepInfo(gitCloneStepInfo, ciExecutionConfigService, accountId, os);

    //Get the Git Connector
    final String connectorRef = pluginStepInfo.getConnectorRef().getValue();
    final NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    final ConnectorDetails gitConnector = codebaseUtils.getGitConnector(ngAccess, connectorRef);

    //Set the Git Connector Reference environment variables
    final String repoName = gitCloneStepInfo.getRepoName().getValue();
    final Map<String, String> gitEnvVars = codebaseUtils.getGitEnvVariables(gitConnector, repoName);
    pluginStepInfo.getEnvVariables().putAll(gitEnvVars);

    String cloneDirectoryString = RunTimeInputHandler.resolveStringParameter("cloneDirectory", name,
            identifier, gitCloneStepInfo.getCloneDirectory(), false);
    if(isNotEmpty(cloneDirectoryString)) {
      pluginStepInfo.getEnvVariables().put(DRONE_WORKSPACE, cloneDirectoryString);
    }

    // Pass in an executionArgs without an executionSource so createPluginStepContainerDefinition method does not add
    // tag and branch variables from clone codebase, possibly conflicting with existing env variables in pluginStepInfo
    final CIExecutionArgs emptyExecutionArgs =
            CIExecutionArgs.builder().runSequence(ciExecutionArgs.getRunSequence()).build();
    ContainerDefinitionInfo pluginStepContainerDefinition = createPluginStepContainerDefinition(pluginStepInfo,
            integrationStage, emptyExecutionArgs, portFinder, stepIndex, identifier, name, accountId, os,
            extraMemoryPerStep, extraCPUPerStep);
    pluginStepContainerDefinition.setGitConnector(gitConnector);
    return pluginStepContainerDefinition;
  }

  private ContainerDefinitionInfo createPluginStepContainerDefinition(PluginStepInfo pluginStepInfo,
      StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex,
      String identifier, String name, String accountId, OSType os, Integer extraMemoryPerStep,
      Integer extraCPUPerStep) {
    Integer port = portFinder.getNextPort();

    String containerName = format("%s%d", STEP_PREFIX, stepIndex);
    Map<String, String> envVarMap = new HashMap<>();
    envVarMap.putAll(getEnvVariables(integrationStage));
    envVarMap.putAll(BuildEnvironmentUtils.getBuildEnvironmentVariables(ciExecutionArgs));
    if (!isEmpty(pluginStepInfo.getEnvVariables())) {
      envVarMap.putAll(pluginStepInfo.getEnvVariables());
    }

    Integer runAsUser = resolveIntegerParameter(pluginStepInfo.getRunAsUser(), null);

    return ContainerDefinitionInfo.builder()
        .name(containerName)
        .commands(StepContainerUtils.getCommand(os))
        .args(StepContainerUtils.getArguments(port))
        .envVars(envVarMap)
        .stepIdentifier(identifier)
        .secretVariables(getSecretVariables(integrationStage))
        .containerImageDetails(ContainerImageDetails.builder()
                                   .imageDetails(IntegrationStageUtils.getImageInfo(resolveStringParameter(
                                       "Image", "Plugin", identifier, pluginStepInfo.getImage(), true)))
                                   .connectorIdentifier(resolveStringParameter(
                                       "connectorRef", "Plugin", identifier, pluginStepInfo.getConnectorRef(), true))
                                   .build())
        .containerResourceParams(getStepContainerResource(
            pluginStepInfo.getResources(), "Plugin", identifier, accountId, extraMemoryPerStep, extraCPUPerStep))
        .isHarnessManagedImage(pluginStepInfo.isHarnessManagedImage())
        .ports(Arrays.asList(port))
        .containerType(CIContainerType.PLUGIN)
        .stepName(name)
        .privileged(pluginStepInfo.getPrivileged().getValue())
        .runAsUser(runAsUser)
        .imagePullPolicy(RunTimeInputHandler.resolveImagePullPolicy(pluginStepInfo.getImagePullPolicy()))
        .build();
  }

  private ContainerResourceParams getStepContainerResource(ContainerResource resource, String stepType, String stepId,
      String accountId, Integer extraMemoryPerStep, Integer extraCPUPerStep) {
    Integer cpuLimit;
    Integer memoryLimit;

    if (featureFlagService.isEnabled(FeatureName.CI_DISABLE_RESOURCE_OPTIMIZATION, accountId)
        || accountId.equals(AXA_ACCOUNT_ID)) {
      cpuLimit = getContainerCpuLimit(resource, stepType, stepId, accountId);
      memoryLimit = getContainerMemoryLimit(resource, stepType, stepId, accountId);
    } else {
      cpuLimit = getContainerCpuLimit(resource, stepType, stepId, accountId) + extraCPUPerStep;
      memoryLimit = getContainerMemoryLimit(resource, stepType, stepId, accountId) + extraMemoryPerStep;
    }

    return ContainerResourceParams.builder()
        .resourceRequestMilliCpu(STEP_REQUEST_MILLI_CPU)
        .resourceRequestMemoryMiB(STEP_REQUEST_MEMORY_MIB)
        .resourceLimitMilliCpu(cpuLimit)
        .resourceLimitMemoryMiB(memoryLimit)
        .build();
  }

  private Map<String, String> getEnvVariables(StageElementConfig stageElementConfig) {
    if (isEmpty(stageElementConfig.getVariables())) {
      return Collections.emptyMap();
    }

    return stageElementConfig.getVariables()
        .stream()
        .filter(customVariables -> customVariables.getType() == NGVariableType.STRING)
        .map(customVariable -> (StringNGVariable) customVariable)
        .collect(Collectors.toMap(ngVariable
            -> ngVariable.getName(),
            ngVariable
            -> resolveStringParameterWithDefaultValue("variableValue", "stage", stageElementConfig.getIdentifier(),
                ngVariable.getValue(), false, ngVariable.getDefaultValue())));
  }

  private List<SecretNGVariable> getSecretVariables(StageElementConfig stageElementConfig) {
    if (isEmpty(stageElementConfig.getVariables())) {
      return Collections.emptyList();
    }

    return stageElementConfig.getVariables()
        .stream()
        .filter(variable -> variable.getType() == NGVariableType.SECRET)
        .map(customVariable -> (SecretNGVariable) customVariable)
        .collect(Collectors.toList());
  }

  public Integer getStageMemoryRequest(List<ExecutionWrapperConfig> steps, String accountId) {
    Integer stageMemoryRequest = 0;
    for (ExecutionWrapperConfig step : steps) {
      Integer executionWrapperMemoryRequest = getExecutionWrapperMemoryRequest(step, accountId);
      stageMemoryRequest = Math.max(stageMemoryRequest, executionWrapperMemoryRequest);
    }
    return stageMemoryRequest;
  }

  /*
    Since this calculation can be a little hard to understand, here is a wiki with explanation
    https://harness.atlassian.net/wiki/spaces/~47984263/pages/21130641597/Resource+allocation+for+strategy+in+CI
  */
  public Integer getStageRequestWithStrategy(List<ExecutionWrapperConfig> steps,
      Map<String, StrategyExpansionData> strategy, String accountId, String resource) {
    return getRequestForSerialSteps(steps, strategy, accountId, resource);
  }

  public Integer getRequestForSerialSteps(List<ExecutionWrapperConfig> steps,
      Map<String, StrategyExpansionData> strategy, String accountId, String resource) {
    Integer executionWrapperRequest = 0;

    Map<String, List<ExecutionWrapperConfig>> uuidStepsMap = getUUIDStepsMap(steps);
    for (String uuid : uuidStepsMap.keySet()) {
      List<ExecutionWrapperConfig> stepsWithSameUUID = uuidStepsMap.get(uuid);
      Integer request = getResourceRequestForStepsWithUUID(stepsWithSameUUID, uuid, strategy, accountId, resource);
      executionWrapperRequest = Math.max(executionWrapperRequest, request);
    }

    // For parallel steps, as they don't have uuid field
    for (ExecutionWrapperConfig step : steps) {
      if (Strings.isNullOrBlank(step.getUuid())) {
        Integer request = getExecutionWrapperRequestWithStrategy(step, strategy, accountId, resource);
        executionWrapperRequest = Math.max(executionWrapperRequest, request);
      }
    }
    return executionWrapperRequest;
  }

  public Integer getExecutionWrapperRequestWithStrategy(ExecutionWrapperConfig executionWrapper,
      Map<String, StrategyExpansionData> strategy, String accountId, String resource) {
    Integer executionWrapperRequest = 0;

    if (executionWrapper.getStep() != null && !executionWrapper.getStep().isNull()) {
      StepElementConfig stepElementConfig = IntegrationStageUtils.getStepElementConfig(executionWrapper);
      if (resource.equals(MEMORY)) {
        executionWrapperRequest = getStepMemoryLimit(stepElementConfig, accountId);
      } else if (resource.equals(CPU)) {
        executionWrapperRequest = getStepCpuLimit(stepElementConfig, accountId);
      } else {
        throw new InvalidRequestException("Invalid resource type : " + resource);
      }
    } else if (executionWrapper.getParallel() != null && !executionWrapper.getParallel().isNull()) {
      ParallelStepElementConfig parallel = IntegrationStageUtils.getParallelStepElementConfig(executionWrapper);
      List<ExecutionWrapperConfig> steps = parallel.getSections();
      if (isNotEmpty(steps)) {
        Map<String, List<ExecutionWrapperConfig>> uuidStepsMap = getUUIDStepsMap(steps);
        for (String uuid : uuidStepsMap.keySet()) {
          List<ExecutionWrapperConfig> stepsWithSameUUID = uuidStepsMap.get(uuid);
          Integer request = getResourceRequestForStepsWithUUID(stepsWithSameUUID, uuid, strategy, accountId, resource);
          executionWrapperRequest += request;
        }
      }
    } else if (executionWrapper.getStepGroup() != null && !executionWrapper.getStepGroup().isNull()) {
      StepGroupElementConfig stepGroupElementConfig = IntegrationStageUtils.getStepGroupElementConfig(executionWrapper);

      List<ExecutionWrapperConfig> steps = stepGroupElementConfig.getSteps();
      if (isNotEmpty(steps)) {
        executionWrapperRequest = getRequestForSerialSteps(steps, strategy, accountId, resource);
      }
    } else {
      throw new InvalidRequestException("Only Parallel, StepElement and StepGroup are supported");
    }

    return executionWrapperRequest;
  }

  /*
    This method calculates the maximum resource required for a particular UUID.
   */
  private Integer getResourceRequestForStepsWithUUID(List<ExecutionWrapperConfig> steps, String uuid,
      Map<String, StrategyExpansionData> strategy, String accountId, String resource) {
    List<ExecutionWrapperConfig> sortedSteps = decreasingSortWithResource(steps, accountId, resource);
    Integer maxConcurrency = strategy.get(uuid).getMaxConcurrency();

    Integer request = 0;
    for (int i = 0; i < Math.min(maxConcurrency, sortedSteps.size()); i++) {
      request += getExecutionWrapperRequestWithStrategy(sortedSteps.get(i), strategy, accountId, resource);
    }
    return request;
  }

  public List<ExecutionWrapperConfig> decreasingSortWithResource(
      List<ExecutionWrapperConfig> steps, String accountId, String resource) {
    if (resource.equals(MEMORY)) {
      steps = decreasingSortWithMemory(steps, accountId);
    } else if (resource.equals(CPU)) {
      steps = decreasingSortWithCpu(steps, accountId);
    } else {
      throw new InvalidRequestException("Invalid resource type : " + resource);
    }
    return steps;
  }

  public Map<String, List<ExecutionWrapperConfig>> getUUIDStepsMap(List<ExecutionWrapperConfig> steps) {
    Map<String, List<ExecutionWrapperConfig>> map = new HashMap<>();
    for (ExecutionWrapperConfig step : steps) {
      if (Strings.isNotBlank(step.getUuid())) {
        if (!map.containsKey(step.getUuid())) {
          map.put(step.getUuid(), new ArrayList<>());
        }
        map.get(step.getUuid()).add(step);
      }
    }
    return map;
  }

  private Integer getExecutionWrapperMemoryRequest(ExecutionWrapperConfig executionWrapper, String accountId) {
    if (executionWrapper == null) {
      return 0;
    }

    Integer executionWrapperMemoryRequest = 0;
    if (executionWrapper.getStep() != null && !executionWrapper.getStep().isNull()) {
      StepElementConfig stepElementConfig = IntegrationStageUtils.getStepElementConfig(executionWrapper);
      executionWrapperMemoryRequest = getStepMemoryLimit(stepElementConfig, accountId);
    } else if (executionWrapper.getParallel() != null && !executionWrapper.getParallel().isNull()) {
      ParallelStepElementConfig parallel = IntegrationStageUtils.getParallelStepElementConfig(executionWrapper);
      if (isNotEmpty(parallel.getSections())) {
        for (ExecutionWrapperConfig wrapper : parallel.getSections()) {
          executionWrapperMemoryRequest += getExecutionWrapperMemoryRequest(wrapper, accountId);
        }
      }
    } else if (executionWrapper.getStepGroup() != null && !executionWrapper.getStepGroup().isNull()) {
      StepGroupElementConfig stepGroupElementConfig = IntegrationStageUtils.getStepGroupElementConfig(executionWrapper);
      for (ExecutionWrapperConfig wrapper : stepGroupElementConfig.getSteps()) {
        Integer wrapperMemoryRequest = getExecutionWrapperMemoryRequest(wrapper, accountId);
        executionWrapperMemoryRequest = Math.max(executionWrapperMemoryRequest, wrapperMemoryRequest);
      }
    } else {
      throw new InvalidRequestException("Only Parallel, StepElement and StepGroup are supported");
    }

    return executionWrapperMemoryRequest;
  }

  private Integer getStepMemoryLimit(StepElementConfig stepElement, String accountId) {
    Integer zeroMemory = 0;
    if (!(stepElement.getStepSpecType() instanceof CIStepInfo)) {
      return zeroMemory;
    }

    ContainerResource containerResource = getStepResources((CIStepInfo) stepElement.getStepSpecType());
    return getContainerMemoryLimit(containerResource, stepElement.getType(), stepElement.getIdentifier(), accountId);
  }

  private Integer getContainerMemoryLimit(
      ContainerResource resource, String stepType, String stepId, String accountID) {
    Integer memoryLimit = ciExecutionConfigService.getCiExecutionServiceConfig().getDefaultMemoryLimit();

    if (featureFlagService.isEnabled(FeatureName.CI_INCREASE_DEFAULT_RESOURCES, accountID)) {
      log.info("Increase default resources FF is enabled for accountID: {}", accountID);
      memoryLimit = DEFAULT_CONTAINER_MEM_POV;
    }

    if (resource != null && resource.getLimits() != null && resource.getLimits().getMemory() != null) {
      String memoryLimitMemoryQuantity =
          resolveStringParameter("memory", stepType, stepId, resource.getLimits().getMemory(), false);
      if (isNotEmpty(memoryLimitMemoryQuantity) && !UNRESOLVED_PARAMETER.equals(memoryLimitMemoryQuantity)) {
        memoryLimit = QuantityUtils.getStorageQuantityValueInUnit(memoryLimitMemoryQuantity, StorageQuantityUnit.Mi);
      }
    }
    return memoryLimit;
  }

  public Pair<Integer, Integer> getStageRequest(InitializeStepInfo initializeStepInfo, String accountId) {
    Integer stageCpuRequest = 0;
    Integer stageMemoryRequest = 0;

    if (isEmpty(initializeStepInfo.getStrategyExpansionMap())) {
      stageCpuRequest = getStageCpuRequest(initializeStepInfo.getExecutionElementConfig().getSteps(), accountId);
      stageMemoryRequest = getStageMemoryRequest(initializeStepInfo.getExecutionElementConfig().getSteps(), accountId);
    } else {
      stageCpuRequest = getStageRequestWithStrategy(initializeStepInfo.getExecutionElementConfig().getSteps(),
          initializeStepInfo.getStrategyExpansionMap(), accountId, CPU);
      stageMemoryRequest = getStageRequestWithStrategy(initializeStepInfo.getExecutionElementConfig().getSteps(),
          initializeStepInfo.getStrategyExpansionMap(), accountId, MEMORY);
    }

    return Pair.of(stageCpuRequest, stageMemoryRequest);
  }

  public Integer getStageCpuRequest(List<ExecutionWrapperConfig> steps, String accountId) {
    Integer stageCpuRequest = 0;
    for (ExecutionWrapperConfig step : steps) {
      Integer executionWrapperCpuRequest = getExecutionWrapperCpuRequest(step, accountId);
      stageCpuRequest = Math.max(stageCpuRequest, executionWrapperCpuRequest);
    }
    return stageCpuRequest;
  }

  public List<ExecutionWrapperConfig> decreasingSortWithMemory(List<ExecutionWrapperConfig> steps, String accountId) {
    Comparator<ExecutionWrapperConfig> decreasingSortWithMemory = (a, b) -> {
      if (getExecutionWrapperMemoryRequest(a, accountId) < getExecutionWrapperMemoryRequest(b, accountId)) {
        return 1;
      } else {
        return -1;
      }
    };
    Collections.sort(steps, decreasingSortWithMemory);

    return steps;
  }

  public List<ExecutionWrapperConfig> decreasingSortWithCpu(List<ExecutionWrapperConfig> steps, String accountId) {
    Comparator<ExecutionWrapperConfig> decreasingSortWithCpu = (a, b) -> {
      if (getExecutionWrapperCpuRequest(a, accountId) < getExecutionWrapperCpuRequest(b, accountId)) {
        return 1;
      } else {
        return -1;
      }
    };
    Collections.sort(steps, decreasingSortWithCpu);

    return steps;
  }

  private Integer getExecutionWrapperCpuRequest(ExecutionWrapperConfig executionWrapper, String accountId) {
    if (executionWrapper == null) {
      return 0;
    }

    Integer executionWrapperCpuRequest = 0;
    if (executionWrapper.getStep() != null && !executionWrapper.getStep().isNull()) {
      StepElementConfig stepElementConfig = IntegrationStageUtils.getStepElementConfig(executionWrapper);
      executionWrapperCpuRequest = getStepCpuLimit(stepElementConfig, accountId);
    } else if (executionWrapper.getParallel() != null && !executionWrapper.getParallel().isNull()) {
      ParallelStepElementConfig parallelStepElement =
          IntegrationStageUtils.getParallelStepElementConfig(executionWrapper);
      if (isNotEmpty(parallelStepElement.getSections())) {
        for (ExecutionWrapperConfig wrapper : parallelStepElement.getSections()) {
          executionWrapperCpuRequest += getExecutionWrapperCpuRequest(wrapper, accountId);
        }
      }
    } else if (executionWrapper.getStepGroup() != null && !executionWrapper.getStepGroup().isNull()) {
      StepGroupElementConfig stepGroupElementConfig = IntegrationStageUtils.getStepGroupElementConfig(executionWrapper);
      for (ExecutionWrapperConfig wrapper : stepGroupElementConfig.getSteps()) {
        Integer stepCpuRequest = getExecutionWrapperCpuRequest(wrapper, accountId);
        executionWrapperCpuRequest = Math.max(executionWrapperCpuRequest, stepCpuRequest);
      }
    } else {
      throw new InvalidRequestException("Only Parallel, StepElement and StepGroup are supported");
    }
    return executionWrapperCpuRequest;
  }

  private Integer getStepCpuLimit(StepElementConfig stepElement, String accountId) {
    Integer zeroCpu = 0;
    if (!(stepElement.getStepSpecType() instanceof CIStepInfo)) {
      return zeroCpu;
    }

    CIStepInfo ciStepInfo = (CIStepInfo) stepElement.getStepSpecType();
    switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
      case RUN:
        return getContainerCpuLimit(
            ((RunStepInfo) ciStepInfo).getResources(), stepElement.getType(), stepElement.getIdentifier(), accountId);
      case PLUGIN:
        return getContainerCpuLimit(((PluginStepInfo) ciStepInfo).getResources(), stepElement.getType(),
            stepElement.getIdentifier(), accountId);
      case RUN_TESTS:
        return getContainerCpuLimit(((RunTestsStepInfo) ciStepInfo).getResources(), stepElement.getType(),
            stepElement.getIdentifier(), accountId);
      case GCR:
      case ECR:
      case ACR:
      case DOCKER:
      case UPLOAD_ARTIFACTORY:
      case UPLOAD_GCS:
      case UPLOAD_S3:
      case RESTORE_CACHE_GCS:
      case RESTORE_CACHE_S3:
      case SAVE_CACHE_S3:
      case SAVE_CACHE_GCS:
      case SECURITY:
      case GIT_CLONE:
        return getContainerCpuLimit(((PluginCompatibleStep) ciStepInfo).getResources(), stepElement.getType(),
            stepElement.getIdentifier(), accountId);
      default:
        return zeroCpu;
    }
  }

  private Integer getContainerCpuLimit(ContainerResource resource, String stepType, String stepId, String accountID) {
    Integer cpuLimit = ciExecutionConfigService.getCiExecutionServiceConfig().getDefaultCPULimit();

    if (featureFlagService.isEnabled(FeatureName.CI_INCREASE_DEFAULT_RESOURCES, accountID)) {
      log.info("Increase default resources FF is enabled for accountID: {}", accountID);
      cpuLimit = DEFAULT_CONTAINER_CPU_POV;
    }

    if (resource != null && resource.getLimits() != null && resource.getLimits().getCpu() != null) {
      String cpuLimitQuantity = resolveStringParameter("cpu", stepType, stepId, resource.getLimits().getCpu(), false);
      if (isNotEmpty(cpuLimitQuantity) && !UNRESOLVED_PARAMETER.equals(cpuLimitQuantity)) {
        cpuLimit = QuantityUtils.getCpuQuantityValueInUnit(cpuLimitQuantity, DecimalQuantityUnit.m);
      }
    }
    return cpuLimit;
  }

  private ContainerResource getStepResources(CIStepInfo ciStepInfo) {
    switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
      case RUN:
        return ((RunStepInfo) ciStepInfo).getResources();
      case PLUGIN:
        return ((PluginStepInfo) ciStepInfo).getResources();
      case RUN_TESTS:
        return ((RunTestsStepInfo) ciStepInfo).getResources();
      case GCR:
      case ECR:
      case ACR:
      case DOCKER:
      case UPLOAD_ARTIFACTORY:
      case UPLOAD_GCS:
      case UPLOAD_S3:
      case RESTORE_CACHE_GCS:
      case RESTORE_CACHE_S3:
      case SAVE_CACHE_S3:
      case SAVE_CACHE_GCS:
      case SECURITY:
      case GIT_CLONE:
        return ((PluginCompatibleStep) ciStepInfo).getResources();
      default:
        throw new CIStageExecutionException(
            format("%s step not allowed in builds", ciStepInfo.getNonYamlInfo().getStepInfoType()));
    }
  }

  public Map<String, List<K8BuildJobEnvInfo.ConnectorConversionInfo>> getStepConnectorRefs(
      IntegrationStageConfig integrationStageConfig, Ambiance ambiance) {
    List<ExecutionWrapperConfig> executionWrappers = integrationStageConfig.getExecution().getSteps();
    if (isEmpty(executionWrappers)) {
      return Collections.emptyMap();
    }

    Map<String, List<K8BuildJobEnvInfo.ConnectorConversionInfo>> map = new HashMap<>();
    for (ExecutionWrapperConfig executionWrapperConfig : executionWrappers) {
      populateStepConnectorRefsUtil(executionWrapperConfig, ambiance, map);
    }
    return map;
  }

  public void populateStepConnectorRefsUtil(ExecutionWrapperConfig executionWrapperConfig, Ambiance ambiance,
      Map<String, List<K8BuildJobEnvInfo.ConnectorConversionInfo>> map) {
    if (executionWrapperConfig.getStep() != null && !executionWrapperConfig.getStep().isNull()) {
      StepElementConfig stepElementConfig = getStepElementConfig(executionWrapperConfig);
      map.putAll(getStepConnectorConversionInfo(stepElementConfig, ambiance));
    } else if (executionWrapperConfig.getParallel() != null && !executionWrapperConfig.getParallel().isNull()) {
      ParallelStepElementConfig parallelStepElementConfig = getParallelStepElementConfig(executionWrapperConfig);
      for (ExecutionWrapperConfig executionWrapper : parallelStepElementConfig.getSections()) {
        populateStepConnectorRefsUtil(executionWrapper, ambiance, map);
      }
    } else if (executionWrapperConfig.getStepGroup() != null && !executionWrapperConfig.getStepGroup().isNull()) {
      StepGroupElementConfig stepGroupElementConfig =
          IntegrationStageUtils.getStepGroupElementConfig(executionWrapperConfig);
      for (ExecutionWrapperConfig executionWrapper : stepGroupElementConfig.getSteps()) {
        populateStepConnectorRefsUtil(executionWrapper, ambiance, map);
      }
    }
  }

  private Map<String, List<K8BuildJobEnvInfo.ConnectorConversionInfo>> getStepConnectorConversionInfo(
      StepElementConfig stepElement, Ambiance ambiance) {
    Map<String, List<K8BuildJobEnvInfo.ConnectorConversionInfo>> map = new HashMap<>();
    if ((stepElement.getStepSpecType() instanceof PluginCompatibleStep)
        && (stepElement.getStepSpecType() instanceof WithConnectorRef)) {
      map.put(stepElement.getIdentifier(), new ArrayList<>());
      PluginCompatibleStep step = (PluginCompatibleStep) stepElement.getStepSpecType();

      String connectorRef = PluginSettingUtils.getConnectorRef(step);
      Map<EnvVariableEnum, String> envToSecretMap =
          PluginSettingUtils.getConnectorSecretEnvMap(step.getNonYamlInfo().getStepInfoType());
      map.get(stepElement.getIdentifier())
          .add(K8BuildJobEnvInfo.ConnectorConversionInfo.builder()
                   .connectorRef(connectorRef)
                   .envToSecretsMap(envToSecretMap)
                   .build());
      List<K8BuildJobEnvInfo.ConnectorConversionInfo> baseConnectorConversionInfo =
          this.getBaseImageConnectorConversionInfo(step, ambiance);
      map.get(stepElement.getIdentifier()).addAll(baseConnectorConversionInfo);
    }
    return map;
  }

  private List<K8BuildJobEnvInfo.ConnectorConversionInfo> getBaseImageConnectorConversionInfo(
      PluginCompatibleStep step, Ambiance ambiance) {
    List<String> baseConnectorRefs = PluginSettingUtils.getBaseImageConnectorRefs(step);
    List<K8BuildJobEnvInfo.ConnectorConversionInfo> baseImageConnectorConversionInfos = new ArrayList<>();
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    if (!isEmpty(baseConnectorRefs)) {
      baseImageConnectorConversionInfos =
          baseConnectorRefs.stream()
              .map(baseConnectorRef -> {
                CIStepInfoType stepInfoType;
                // get connector details
                ConnectorDetails connectorDetails = connectorUtils.getConnectorDetails(ngAccess, baseConnectorRef);
                switch (connectorDetails.getConnectorType()) {
                  case DOCKER:
                    stepInfoType = CIStepInfoType.DOCKER;
                    break;
                  default:
                    throw new IllegalStateException(
                        "Unexpected base connector: " + connectorDetails.getConnectorType());
                }
                Map<EnvVariableEnum, String> envToSecretMap = PluginSettingUtils.getConnectorSecretEnvMap(stepInfoType);
                return K8BuildJobEnvInfo.ConnectorConversionInfo.builder()
                    .connectorRef(baseConnectorRef)
                    .envToSecretsMap(envToSecretMap)
                    .build();
              })
              .collect(Collectors.toList());
    }
    return baseImageConnectorConversionInfos;
  }

  private ParallelStepElementConfig getParallelStepElementConfig(ExecutionWrapperConfig executionWrapperConfig) {
    try {
      return YamlUtils.read(executionWrapperConfig.getParallel().toString(), ParallelStepElementConfig.class);
    } catch (Exception ex) {
      throw new CIStageExecutionException("Failed to deserialize ExecutionWrapperConfig parallel node", ex);
    }
  }

  private Integer calculateExtraCPUForParallelStep(
      ParallelStepElementConfig parallelStepElementConfig, String accountId, Integer stageCpuRequest) {
    Integer executionWrapperCPURequest = 0;
    for (ExecutionWrapperConfig wrapper : parallelStepElementConfig.getSections()) {
      executionWrapperCPURequest += getExecutionWrapperCpuRequest(wrapper, accountId);
    }
    Integer extraCPUPerStep = 0;

    if (stageCpuRequest > executionWrapperCPURequest) {
      extraCPUPerStep = (stageCpuRequest - executionWrapperCPURequest) / parallelStepElementConfig.getSections().size();
    }

    return extraCPUPerStep;
  }

  private Integer calculateExtraMemoryForParallelStep(
      ParallelStepElementConfig parallelStepElementConfig, String accountId, Integer stageMemoryRequest) {
    Integer executionWrapperMemoryRequest = 0;
    for (ExecutionWrapperConfig wrapper : parallelStepElementConfig.getSections()) {
      executionWrapperMemoryRequest += getExecutionWrapperMemoryRequest(wrapper, accountId);
    }
    Integer extraMemoryPerStep = 0;

    if (stageMemoryRequest > executionWrapperMemoryRequest) {
      extraMemoryPerStep =
          (stageMemoryRequest - executionWrapperMemoryRequest) / parallelStepElementConfig.getSections().size();
    }

    return extraMemoryPerStep;
  }

  private Integer calculateExtraCPU(
      ExecutionWrapperConfig executionWrapper, String accountId, Integer maxAllocatableCpuRequest) {
    Integer executionWrapperCPURequest = getExecutionWrapperCpuRequest(executionWrapper, accountId);
    return Math.max(0, maxAllocatableCpuRequest - executionWrapperCPURequest);
  }

  private Integer calculateExtraMemory(
      ExecutionWrapperConfig executionWrapper, String accountId, Integer maxAllocatableMemoryRequest) {
    Integer executionWrapperMemoryRequest = getExecutionWrapperMemoryRequest(executionWrapper, accountId);
    return Math.max(0, maxAllocatableMemoryRequest - executionWrapperMemoryRequest);
  }

  private StepElementConfig getStepElementConfig(ExecutionWrapperConfig executionWrapperConfig) {
    try {
      return YamlUtils.read(executionWrapperConfig.getStep().toString(), StepElementConfig.class);
    } catch (Exception ex) {
      throw new CIStageExecutionException("Failed to deserialize ExecutionWrapperConfig step node", ex);
    }
  }

  private ContainerResource getContainerResource(StepElementConfig stepElement) {
    if (!(stepElement.getStepSpecType() instanceof CIStepInfo)) {
      return null;
    }

    CIStepInfo ciStepInfo = (CIStepInfo) stepElement.getStepSpecType();
    switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
      case RUN:
        return ((RunStepInfo) ciStepInfo).getResources();
      case DOCKER:
      case ECR:
      case ACR:
      case GCR:
      case SAVE_CACHE_S3:
      case RESTORE_CACHE_S3:
      case RESTORE_CACHE_GCS:
      case SAVE_CACHE_GCS:
      case SECURITY:
      case UPLOAD_ARTIFACTORY:
      case UPLOAD_S3:
      case UPLOAD_GCS:
      case GIT_CLONE:
        return ((PluginCompatibleStep) ciStepInfo).getResources();
      case PLUGIN:
        return ((PluginStepInfo) ciStepInfo).getResources();
      case RUN_TESTS:
        return ((RunTestsStepInfo) ciStepInfo).getResources();
      default:
        return null;
    }
  }
}
