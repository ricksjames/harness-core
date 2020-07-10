package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskDetails;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.SecretDetail;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.artifact.ArtifactFileMetadata;
import io.harness.delegate.beans.connector.k8Connector.ClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskResponse;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.OpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.ServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.UserNamePasswordDTO;
import io.harness.delegate.beans.executioncapability.AlwaysFalseValidationCapability;
import io.harness.delegate.beans.executioncapability.AwsRegionCapability;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ChartMuseumCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.delegate.command.CommandExecutionData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.exception.ArtifactServerException;
import io.harness.delegate.exception.DelegateRetryableException;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.delegate.task.aws.AwsElbListenerRuleData;
import io.harness.delegate.task.aws.AwsLoadBalancerDetails;
import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.delegate.task.aws.LoadBalancerType;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.delegate.task.shell.ShellScriptApprovalTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstDeployTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstGetElastigroupJsonParameters;
import io.harness.delegate.task.spotinst.request.SpotInstListElastigroupInstancesParameters;
import io.harness.delegate.task.spotinst.request.SpotInstListElastigroupNamesParameters;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType;
import io.harness.delegate.task.spotinst.request.SpotinstTrafficShiftAlbDeployParameters;
import io.harness.delegate.task.spotinst.request.SpotinstTrafficShiftAlbSetupParameters;
import io.harness.delegate.task.spotinst.request.SpotinstTrafficShiftAlbSwapRoutesParameters;
import io.harness.delegate.task.spotinst.response.SpotInstDeployTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotInstGetElastigroupJsonResponse;
import io.harness.delegate.task.spotinst.response.SpotInstListElastigroupInstancesResponse;
import io.harness.delegate.task.spotinst.response.SpotInstListElastigroupNamesResponse;
import io.harness.delegate.task.spotinst.response.SpotInstSetupTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotinstTrafficShiftAlbDeployResponse;
import io.harness.delegate.task.spotinst.response.SpotinstTrafficShiftAlbSetupResponse;
import io.harness.serializer.KryoRegistrar;

public class DelegateTasksBeansKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(DelegateRetryableException.class, 5521);
    kryo.register(ShellScriptApprovalTaskParameters.class, 20001);
    kryo.register(HttpTaskParameters.class, 20002);
    kryo.register(ScriptType.class, 5253);
    kryo.register(AwsElbListener.class, 5600);
    kryo.register(CommandExecutionData.class, 5035);
    kryo.register(CommandExecutionResult.class, 5036);
    kryo.register(CommandExecutionStatus.class, 5037);
    kryo.register(SecretDetail.class, 19001);
    kryo.register(TaskData.class, 19002);
    kryo.register(HttpConnectionExecutionCapability.class, 19003);
    kryo.register(CapabilityType.class, 19004);
    kryo.register(DelegateMetaInfo.class, 5372);
    kryo.register(DelegateTaskNotifyResponseData.class, 5373);
    kryo.register(DelegateTaskResponse.ResponseCode.class, 5520);
    kryo.register(DelegateTaskResponse.class, 5006);
    kryo.register(ProcessExecutorCapability.class, 19007);
    kryo.register(AwsRegionCapability.class, 19008);
    kryo.register(SocketConnectivityExecutionCapability.class, 19009);
    kryo.register(SpotInstTaskParameters.class, 19011);
    kryo.register(SpotInstSetupTaskParameters.class, 19012);
    kryo.register(SpotInstTaskType.class, 19013);
    kryo.register(SpotInstTaskExecutionResponse.class, 19014);
    kryo.register(SpotInstTaskResponse.class, 19015);
    kryo.register(SpotInstSetupTaskResponse.class, 19016);
    kryo.register(SpotInstDeployTaskResponse.class, 19017);
    kryo.register(SpotInstDeployTaskParameters.class, 19018);
    kryo.register(SystemEnvCheckerCapability.class, 19022);
    kryo.register(SpotInstSwapRoutesTaskParameters.class, 19023);
    kryo.register(ErrorNotifyResponseData.class, 5213);
    kryo.register(AwsLoadBalancerDetails.class, 19024);
    kryo.register(SpotInstGetElastigroupJsonParameters.class, 19025);
    kryo.register(SpotInstListElastigroupInstancesParameters.class, 19026);
    kryo.register(SpotInstListElastigroupNamesParameters.class, 19027);
    kryo.register(SpotInstGetElastigroupJsonResponse.class, 19028);
    kryo.register(SpotInstListElastigroupInstancesResponse.class, 19029);
    kryo.register(SpotInstListElastigroupNamesResponse.class, 19030);
    kryo.register(LoadBalancerDetailsForBGDeployment.class, 19031);
    kryo.register(LoadBalancerType.class, 19032);
    kryo.register(ArtifactServerException.class, 7244);
    kryo.register(PcfManifestsPackage.class, 19033);
    kryo.register(ArtifactFileMetadata.class, 19034);
    kryo.register(AwsElbListenerRuleData.class, 19035);
    kryo.register(AlwaysFalseValidationCapability.class, 19036);
    kryo.register(LbDetailsForAlbTrafficShift.class, 19037);
    kryo.register(ChartMuseumCapability.class, 19038);
    kryo.register(SpotinstTrafficShiftAlbSetupParameters.class, 19039);
    kryo.register(SpotinstTrafficShiftAlbSetupResponse.class, 19040);
    kryo.register(SpotinstTrafficShiftAlbDeployParameters.class, 19041);
    kryo.register(SpotinstTrafficShiftAlbDeployResponse.class, 19042);
    kryo.register(SpotinstTrafficShiftAlbSwapRoutesParameters.class, 19043);
    kryo.register(DelegateTaskDetails.class, 19044);
    kryo.register(KubernetesClusterConfigDTO.class, 19045);
    kryo.register(KubernetesCredentialType.class, 19046);
    kryo.register(KubernetesCredentialDTO.class, 19047);
    kryo.register(KubernetesDelegateDetailsDTO.class, 19048);
    kryo.register(KubernetesClusterDetailsDTO.class, 19049);
    kryo.register(KubernetesAuthDTO.class, 19050);
    kryo.register(KubernetesAuthType.class, 19051);
    kryo.register(UserNamePasswordDTO.class, 19052);
    kryo.register(ClientKeyCertDTO.class, 19053);
    kryo.register(ServiceAccountDTO.class, 19054);
    kryo.register(OpenIdConnectDTO.class, 19055);
    kryo.register(KubernetesConnectionTaskResponse.class, 19056);
    kryo.register(KubernetesConnectionTaskParams.class, 19057);
    kryo.register(KubernetesAuthCredentialDTO.class, 19058);
    kryo.register(K8sTaskType.class, 7125);
  }
}
