package io.harness.stateutils.buildstate;

import static io.harness.common.CIExecutionConstants.ACCESS_KEY_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.BUCKET_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.BUCKET_MINIO_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.ENDPOINT_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.ENDPOINT_MINIO_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.SECRET_KEY_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.SETUP_TASK_ARGS;
import static io.harness.common.CIExecutionConstants.SH_COMMAND;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider.ContainerKind.ADDON_CONTAINER;
import static io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider.ContainerKind.LITE_ENGINE_CONTAINER;
import static java.util.stream.Collectors.toList;
import static software.wings.common.CICommonPodConstants.MOUNT_PATH;
import static software.wings.common.CICommonPodConstants.STEP_EXEC;
import static software.wings.common.CICommonPodConstants.STEP_EXEC_WORKING_DIR;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ambiance.Ambiance;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.exception.InvalidRequestException;
import io.harness.managerclient.ManagerCIResource;
import io.harness.network.SafeHttpCall;
import io.harness.references.SweepingOutputRefObject;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8ContainerParams.CIK8ContainerParamsBuilder;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.ContainerSecrets;
import software.wings.beans.ci.pod.EncryptedVariableWithType;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Singleton
@Slf4j
public class K8BuildSetupUtils {
  @Inject private ManagerCIResource managerCIResource;
  @Inject private LiteEngineTaskUtils liteEngineTaskUtils;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;

  public RestResponse<K8sTaskExecutionResponse> executeCISetupTask(
      BuildEnvSetupStepInfo buildEnvSetupStepInfo, Ambiance ambiance) {
    try {
      K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
          ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());

      final String namespace = k8PodDetails.getNamespace();
      final String clusterName = k8PodDetails.getClusterName();
      PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) buildEnvSetupStepInfo.getBuildJobEnvInfo());

      Set<String> publishStepConnectorIdentifier =
          ((K8BuildJobEnvInfo) buildEnvSetupStepInfo.getBuildJobEnvInfo()).getPublishStepConnectorIdentifier();

      // TODO Use k8 connector from element input
      return SafeHttpCall.execute(managerCIResource.createK8PodTask(clusterName,
          buildEnvSetupStepInfo.getGitConnectorIdentifier(), buildEnvSetupStepInfo.getBranchName(),
          getPodParams(podSetupInfo, namespace, SH_COMMAND, Collections.singletonList(SETUP_TASK_ARGS),
              publishStepConnectorIdentifier)));

    } catch (Exception e) {
      logger.error("build state execution failed", e);
    }
    return null;
  }

  public RestResponse<K8sTaskExecutionResponse> executeK8sCILiteEngineTask(
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance) {
    K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
        ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());
    final String namespace = k8PodDetails.getNamespace();
    final String clusterName = k8PodDetails.getClusterName();

    try {
      PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo());

      List<String> command = liteEngineTaskUtils.getLiteEngineCommand();
      List<String> arguments = liteEngineTaskUtils.getLiteEngineArguments(liteEngineTaskStepInfo);
      Set<String> publishStepConnectorIdentifier =
          ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo()).getPublishStepConnectorIdentifier();

      // TODO Use k8 connector from element input
      return SafeHttpCall.execute(managerCIResource.createK8PodTask(clusterName,
          liteEngineTaskStepInfo.getGitConnectorIdentifier(), liteEngineTaskStepInfo.getBranchName(),
          getPodParams(podSetupInfo, namespace, command, arguments, publishStepConnectorIdentifier)));
    } catch (Exception e) {
      logger.error("lite engine task state execution failed", e);
    }
    return null;
  }

  public CIK8PodParams<CIK8ContainerParams> getPodParams(PodSetupInfo podSetupInfo, String namespace,
      List<String> commands, List<String> args, Set<String> publishStepConnectorIdentifier) {
    Map<String, String> map = new HashMap<>();
    map.put(STEP_EXEC, MOUNT_PATH);

    // user input container with custom entry point
    List<CIK8ContainerParams> containerParams =
        podSetupInfo.getPodSetupParams()
            .getContainerDefinitionInfos()
            .stream()
            .map(containerDefinitionInfo
                -> CIK8ContainerParams.builder()
                       .name(containerDefinitionInfo.getName())
                       .containerResourceParams(containerDefinitionInfo.getContainerResourceParams())
                       .containerType(CIContainerType.STEP_EXECUTOR)
                       .envVars(getCIExecutorEnvVariables(containerDefinitionInfo))
                       .containerSecrets(ContainerSecrets.builder()
                                             .encryptedSecrets(getSecretEnvVars(containerDefinitionInfo))
                                             .build())
                       .commands(commands)
                       .args(args)
                       .imageDetailsWithConnector(
                           ImageDetailsWithConnector.builder()
                               .imageDetails(containerDefinitionInfo.getContainerImageDetails().getImageDetails())
                               .connectorName(
                                   containerDefinitionInfo.getContainerImageDetails().getConnectorIdentifier())
                               .build())
                       .volumeToMountPath(map)
                       .build())
            .collect(toList());

    CIK8ContainerParamsBuilder addOnCik8ContainerParamsBuilder =
        InternalContainerParamsProvider.getContainerParams(ADDON_CONTAINER);

    addOnCik8ContainerParamsBuilder.containerSecrets(
        ContainerSecrets.builder()
            .publishArtifactEncryptedValues(getPublishArtifactEncryptedValues(publishStepConnectorIdentifier))
            .build());
    // include addon container
    containerParams.add(addOnCik8ContainerParamsBuilder.build());

    return CIK8PodParams.<CIK8ContainerParams>builder()
        .name(podSetupInfo.getName())
        .namespace(namespace)
        .stepExecVolumeName(STEP_EXEC)
        .stepExecWorkingDir(STEP_EXEC_WORKING_DIR)
        .containerParamsList(containerParams)
        .initContainerParamsList(Collections.singletonList(
            InternalContainerParamsProvider.getContainerParams(LITE_ENGINE_CONTAINER).build()))
        .build();
  }

  private Map<String, EncryptableSettingWithEncryptionDetails> getPublishArtifactEncryptedValues(
      Set<String> publishStepConnectorIdentifier) {
    Map<String, EncryptableSettingWithEncryptionDetails> publishArtifactEncryptedValues = new HashMap<>();

    if (isNotEmpty(publishStepConnectorIdentifier)) {
      // TODO Harsh Fetch connector encrypted values once connector APIs will be ready
      for (String connectorIdentifier : publishStepConnectorIdentifier) {
        publishArtifactEncryptedValues.put(connectorIdentifier, null);
      }
    }
    return publishArtifactEncryptedValues;
  }

  @NotNull
  private PodSetupInfo getPodSetupInfo(K8BuildJobEnvInfo k8BuildJobEnvInfo) {
    // Supporting single pod currently
    Optional<PodSetupInfo> podSetupInfoOpt =
        k8BuildJobEnvInfo.getPodsSetupInfo().getPodSetupInfoList().stream().findFirst();
    if (!podSetupInfoOpt.isPresent()) {
      throw new InvalidRequestException("Pod setup info can not be empty");
    }
    return podSetupInfoOpt.get();
  }

  @NotNull
  private Map<String, EncryptedVariableWithType> getSecretEnvVars(ContainerDefinitionInfo containerDefinitionInfo) {
    Map<String, EncryptedVariableWithType> envSecretVars = new HashMap<>();
    if (isNotEmpty(containerDefinitionInfo.getEncryptedSecrets())) {
      envSecretVars.putAll(containerDefinitionInfo.getEncryptedSecrets()); // Put customer input env variables
    }
    // Put Harness internal env variable like that of minio
    // TODO Replace null with encrypted values once cdng secret apis are ready
    envSecretVars.put(ACCESS_KEY_MINIO_VARIABLE, null);
    envSecretVars.put(SECRET_KEY_MINIO_VARIABLE, null);

    return envSecretVars;
  }

  @NotNull
  private Map<String, String> getCIExecutorEnvVariables(ContainerDefinitionInfo containerDefinitionInfo) {
    Map<String, String> envVars = new HashMap<>();
    if (isNotEmpty(containerDefinitionInfo.getEnvVars())) {
      envVars.putAll(containerDefinitionInfo.getEnvVars()); // Put customer input env variables
    }
    // Put Harness internal env variable like that of minio
    envVars.put(ENDPOINT_MINIO_VARIABLE, ENDPOINT_MINIO_VARIABLE_VALUE);
    envVars.put(BUCKET_MINIO_VARIABLE, BUCKET_MINIO_VARIABLE_VALUE);
    return envVars;
  }
}
