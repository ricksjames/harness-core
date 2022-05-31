/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.integrationstage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.environment.pod.container.ContainerImageDetails;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.quantity.unit.DecimalQuantityUnit;
import io.harness.beans.quantity.unit.StorageQuantityUnit;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.RunTestsStepInfo;
import io.harness.beans.sweepingoutputs.StageInfraDetails;
import io.harness.beans.yaml.extended.infrastrucutre.OSType;
import io.harness.ci.utils.QuantityUtils;
import io.harness.delegate.beans.ci.pod.CIContainerType;
import io.harness.delegate.beans.ci.pod.ContainerResourceParams;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.execution.CIExecutionConfigService;
import io.harness.ff.CIFeatureFlagService;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.stateutils.buildstate.PluginSettingUtils;
import io.harness.stateutils.buildstate.providers.StepContainerUtils;
import io.harness.steps.CIStepInfoUtils;
import io.harness.util.PortFinder;
import io.harness.utils.TimeoutUtils;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.SecretNGVariable;
import io.harness.yaml.core.variables.StringNGVariable;
import io.harness.yaml.extended.ci.container.ContainerResource;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.harness.beans.serializer.RunTimeInputHandler.UNRESOLVED_PARAMETER;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveIntegerParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveMapParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameterWithDefaultValue;
import static io.harness.common.CIExecutionConstants.DEFAULT_CONTAINER_CPU_POV;
import static io.harness.common.CIExecutionConstants.DEFAULT_CONTAINER_MEM_POV;
import static io.harness.common.CIExecutionConstants.STEP_PREFIX;
import static io.harness.common.CIExecutionConstants.STEP_REQUEST_MEMORY_MIB;
import static io.harness.common.CIExecutionConstants.STEP_REQUEST_MILLI_CPU;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class K8InitializeStepUtils {
    @Inject
    private CIExecutionConfigService ciExecutionConfigService;
    @Inject private CIFeatureFlagService featureFlagService;

    public List<ContainerDefinitionInfo> createStepContainerDefinitions(List<ExecutionWrapperConfig> steps,
                                                                         StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, String accountId,
                                                                         OSType os) {
        List<ContainerDefinitionInfo> containerDefinitionInfos = new ArrayList<>();
        if (steps == null) {
            return containerDefinitionInfos;
        }

        int stepIndex = 0;
        for (ExecutionWrapperConfig executionWrapper : steps) {
            if (executionWrapper.getStep() != null && !executionWrapper.getStep().isNull()) {
                StepElementConfig stepElementConfig = IntegrationStageUtils.getStepElementConfig(executionWrapper);
                stepIndex++;
                if (stepElementConfig.getTimeout() != null && stepElementConfig.getTimeout().isExpression()) {
                    throw new InvalidRequestException(
                            "Timeout field must be resolved in step: " + stepElementConfig.getIdentifier());
                }

                ContainerDefinitionInfo containerDefinitionInfo = createStepContainerDefinition(
                        stepElementConfig, integrationStage, ciExecutionArgs, portFinder, stepIndex, accountId, os);
                if (containerDefinitionInfo != null) {
                    containerDefinitionInfos.add(containerDefinitionInfo);
                }
            } else if (executionWrapper.getParallel() != null && !executionWrapper.getParallel().isNull()) {
                ParallelStepElementConfig parallelStepElementConfig =
                        IntegrationStageUtils.getParallelStepElementConfig(executionWrapper);
                if (isNotEmpty(parallelStepElementConfig.getSections())) {
                    for (ExecutionWrapperConfig executionWrapperInParallel : parallelStepElementConfig.getSections()) {
                        if (executionWrapperInParallel.getStep() == null || executionWrapperInParallel.getStep().isNull()) {
                            continue;
                        }

                        stepIndex++;
                        StepElementConfig stepElementConfig =
                                IntegrationStageUtils.getStepElementConfig(executionWrapperInParallel);
                        ContainerDefinitionInfo containerDefinitionInfo = createStepContainerDefinition(
                                stepElementConfig, integrationStage, ciExecutionArgs, portFinder, stepIndex, accountId, os);
                        if (containerDefinitionInfo != null) {
                            containerDefinitionInfos.add(containerDefinitionInfo);
                        }
                    }
                }
            }
        }
        return containerDefinitionInfos;
    }

    private ContainerDefinitionInfo createStepContainerDefinition(StepElementConfig stepElement,
                                                                  StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex,
                                                                  String accountId, OSType os) {
        if (!(stepElement.getStepSpecType() instanceof CIStepInfo)) {
            return null;
        }

        CIStepInfo ciStepInfo = (CIStepInfo) stepElement.getStepSpecType();
        validateStepType(ciStepInfo.getNonYamlInfo().getStepInfoType(), os);

        long timeout = TimeoutUtils.getTimeoutInSeconds(stepElement.getTimeout(), ciStepInfo.getDefaultTimeout());
        switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
            case RUN:
                return createRunStepContainerDefinition((RunStepInfo) ciStepInfo, integrationStage, ciExecutionArgs, portFinder,
                        stepIndex, stepElement.getIdentifier(), stepElement.getName(), accountId, os);
            case DOCKER:
            case ECR:
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
                        stepElement.getType(), timeout, accountId, os);
            case PLUGIN:
                return createPluginStepContainerDefinition((PluginStepInfo) ciStepInfo, integrationStage, ciExecutionArgs,
                        portFinder, stepIndex, stepElement.getIdentifier(), stepElement.getName(), accountId, os);
            case RUN_TESTS:
                return createRunTestsStepContainerDefinition((RunTestsStepInfo) ciStepInfo, integrationStage, ciExecutionArgs,
                        portFinder, stepIndex, stepElement.getIdentifier(), accountId, os);
            default:
                return null;
        }
    }

    private void validateStepType(CIStepInfoType stepType, OSType os) {
        if (os != OSType.Windows) {
            return;
        }

        switch (stepType) {
            case DOCKER:
            case ECR:
            case GCR:
                throw new CIStageExecutionException(format("%s step not allowed in windows kubernetes builds", stepType));
            default:
                return;
        }
    }

    private ContainerDefinitionInfo createPluginCompatibleStepContainerDefinition(PluginCompatibleStep stepInfo,
                                                                                  StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex,
                                                                                  String identifier, String stepName, String stepType, long timeout, String accountId, OSType os) {
        Integer port = portFinder.getNextPort();

        String containerName = format("%s%d", STEP_PREFIX, stepIndex);
        Map<String, String> envVarMap = new HashMap<>();
        envVarMap.putAll(getEnvVariables(integrationStage));
        envVarMap.putAll(BuildEnvironmentUtils.getBuildEnvironmentVariables(ciExecutionArgs));
        envVarMap.putAll(PluginSettingUtils.getPluginCompatibleEnvVariables(stepInfo, identifier, timeout, StageInfraDetails.Type.K8));
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
                                .imageDetails(IntegrationStageUtils.getImageInfo(
                                        CIStepInfoUtils.getPluginCustomStepImage(stepInfo, ciExecutionConfigService, StageInfraDetails.Type.K8, accountId)))
                                .build())
                .isHarnessManagedImage(true)
                .containerResourceParams(getStepContainerResource(stepInfo.getResources(), stepType, identifier, accountId))
                .ports(Arrays.asList(port))
                .containerType(CIContainerType.PLUGIN)
                .stepIdentifier(identifier)
                .stepName(stepName)
                .imagePullPolicy(RunTimeInputHandler.resolveImagePullPolicy(CIStepInfoUtils.getImagePullPolicy(stepInfo)))
                .privileged(privileged)
                .runAsUser(runAsUser)
                .build();
    }

    private ContainerDefinitionInfo createRunStepContainerDefinition(RunStepInfo runStepInfo,
                                                                     StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex,
                                                                     String identifier, String name, String accountId, OSType os) {
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
                .containerResourceParams(getStepContainerResource(runStepInfo.getResources(), "Run", identifier, accountId))
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
                                                                          String identifier, String accountId, OSType os) {
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
                .containerResourceParams(
                        getStepContainerResource(runTestsStepInfo.getResources(), "RunTests", identifier, accountId))
                .ports(Arrays.asList(port))
                .containerType(CIContainerType.TEST_INTELLIGENCE)
                .privileged(runTestsStepInfo.getPrivileged().getValue())
                .runAsUser(runAsUser)
                .imagePullPolicy(RunTimeInputHandler.resolveImagePullPolicy(runTestsStepInfo.getImagePullPolicy()))
                .build();
    }

    private ContainerDefinitionInfo createPluginStepContainerDefinition(PluginStepInfo pluginStepInfo,
                                                                        StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex,
                                                                        String identifier, String name, String accountId, OSType os) {
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
                .containerResourceParams(
                        getStepContainerResource(pluginStepInfo.getResources(), "Plugin", identifier, accountId))
                .isHarnessManagedImage(pluginStepInfo.isHarnessManagedImage())
                .ports(Arrays.asList(port))
                .containerType(CIContainerType.PLUGIN)
                .stepName(name)
                .privileged(pluginStepInfo.getPrivileged().getValue())
                .runAsUser(runAsUser)
                .imagePullPolicy(RunTimeInputHandler.resolveImagePullPolicy(pluginStepInfo.getImagePullPolicy()))
                .build();
    }

    private ContainerResourceParams getStepContainerResource(
            ContainerResource resource, String stepType, String stepId, String accountId) {
        return ContainerResourceParams.builder()
                .resourceRequestMilliCpu(STEP_REQUEST_MILLI_CPU)
                .resourceRequestMemoryMiB(STEP_REQUEST_MEMORY_MIB)
                .resourceLimitMilliCpu(getContainerCpuLimit(resource, stepType, stepId, accountId))
                .resourceLimitMemoryMiB(getContainerMemoryLimit(resource, stepType, stepId, accountId))
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

    public Integer getStageCpuRequest(List<ExecutionWrapperConfig> steps, String accountId) {
        Integer stageCpuRequest = 0;
        for (ExecutionWrapperConfig step : steps) {
            Integer executionWrapperCpuRequest = getExecutionWrapperCpuRequest(step, accountId);
            stageCpuRequest = Math.max(stageCpuRequest, executionWrapperCpuRequest);
        }
        return stageCpuRequest;
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
            case DOCKER:
            case UPLOAD_ARTIFACTORY:
            case UPLOAD_GCS:
            case UPLOAD_S3:
            case RESTORE_CACHE_GCS:
            case RESTORE_CACHE_S3:
            case SAVE_CACHE_S3:
            case SAVE_CACHE_GCS:
            case SECURITY:
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
            case DOCKER:
            case UPLOAD_ARTIFACTORY:
            case UPLOAD_GCS:
            case UPLOAD_S3:
            case RESTORE_CACHE_GCS:
            case RESTORE_CACHE_S3:
            case SAVE_CACHE_S3:
            case SAVE_CACHE_GCS:
            case SECURITY:
                return ((PluginCompatibleStep) ciStepInfo).getResources();
            default:
                throw new CIStageExecutionException(format("%s step not allowed in builds", ciStepInfo.getNonYamlInfo().getStepInfoType()));
        }
    }
}
