package io.harness.serializer.kryo;

import io.harness.delegate.beans.*;
import io.harness.delegate.beans.artifact.ArtifactFileMetadata;
import io.harness.delegate.beans.azure.*;
import io.harness.delegate.beans.azure.AzureMachineImageArtifactDTO.ImageType;
import io.harness.delegate.beans.azure.AzureMachineImageArtifactDTO.OSType;
import io.harness.delegate.beans.ci.*;
import io.harness.delegate.beans.ci.k8s.CIContainerStatus;
import io.harness.delegate.beans.ci.k8s.CiK8sTaskResponse;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.ci.k8s.PodStatus;
import io.harness.delegate.beans.ci.pod.*;
import io.harness.delegate.beans.ci.status.BuildStatusPushResponse;
import io.harness.delegate.beans.connector.ConnectorHeartbeatDelegateResponse;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectionTaskParams;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectionTaskResponse;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.*;
import io.harness.delegate.beans.connector.ceawsconnector.AwsCurAttributesDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsFeatures;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorTaskParams;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorTaskResponse;
import io.harness.delegate.beans.connector.docker.*;
import io.harness.delegate.beans.connector.gcpconnector.*;
import io.harness.delegate.beans.connector.jira.JiraConnectionTaskParams;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.beans.connector.jira.connection.JiraTestConnectionTaskNGResponse;
import io.harness.delegate.beans.connector.k8Connector.*;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.*;
import io.harness.delegate.beans.connector.scm.github.*;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectionTaskParams;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectionTaskResponse;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.delegate.beans.executioncapability.*;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.git.GitCommandType;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.delegate.beans.logstreaming.CommandUnitStatusProgress;
import io.harness.delegate.beans.secrets.SSHConfigValidationTaskResponse;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.command.CommandExecutionData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.exception.DelegateRetryableException;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import io.harness.delegate.task.artifacts.response.ArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.delegate.task.aws.*;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.AzureTaskParameters;
import io.harness.delegate.task.azure.AzureTaskResponse;
import io.harness.delegate.task.azure.AzureVMSSPreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceType;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppListWebAppDeploymentSlotsParameters;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppListWebAppInstancesParameters;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppListWebAppNamesParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppDeploymentSlotsResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppInstancesResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppNamesResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.DeploymentSlotData;
import io.harness.delegate.task.azure.request.*;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType;
import io.harness.delegate.task.azure.response.*;
import io.harness.delegate.task.ci.CIBuildPushParameters;
import io.harness.delegate.task.ci.CIBuildPushParameters.CIBuildPushTaskType;
import io.harness.delegate.task.ci.CIBuildStatusPushParameters;
import io.harness.delegate.task.ci.GitSCMType;
import io.harness.delegate.task.gcp.request.GcpRequest;
import io.harness.delegate.task.gcp.request.GcpValidationRequest;
import io.harness.delegate.task.gcp.response.GcpValidationTaskResponse;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchRequest;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.delegate.task.http.HttpStepResponse;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.delegate.task.http.HttpTaskParametersNg;
import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse.JiraIssueData;
import io.harness.delegate.task.k8s.*;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.delegate.task.shell.ShellScriptApprovalTaskParameters;
import io.harness.delegate.task.spotinst.request.*;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType;
import io.harness.delegate.task.spotinst.response.*;
import io.harness.delegate.task.stepstatus.*;
import io.harness.serializer.KryoRegistrar;

import software.wings.beans.TaskType;

import com.esotericsoftware.kryo.Kryo;
import org.eclipse.jgit.api.GitCommand;
import org.json.JSONArray;
import org.json.JSONObject;

public class DelegateTasksBeansKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(AlwaysFalseValidationCapability.class, 19036);
    kryo.register(AppDynamicsConnectorDTO.class, 19105);
    kryo.register(AppDynamicsConnectionTaskParams.class, 19107);
    kryo.register(AppDynamicsConnectionTaskResponse.class, 19108);
    kryo.register(ArtifactFileMetadata.class, 19034);
    kryo.register(AwsElbListener.class, 5600);
    kryo.register(AwsElbListenerRuleData.class, 19035);
    kryo.register(AwsLoadBalancerDetails.class, 19024);
    kryo.register(AwsRegionCapability.class, 19008);
    kryo.register(AzureVMSSGetVirtualMachineScaleSetParameters.class, 19075);
    kryo.register(AzureVMSSGetVirtualMachineScaleSetResponse.class, 19080);
    kryo.register(AzureVMSSListResourceGroupsNamesParameters.class, 19076);
    kryo.register(AzureVMSSListResourceGroupsNamesResponse.class, 19081);
    kryo.register(AzureVMSSListSubscriptionsParameters.class, 19077);
    kryo.register(AzureVMSSListSubscriptionsResponse.class, 19082);
    kryo.register(AzureVMSSListVirtualMachineScaleSetsParameters.class, 19078);
    kryo.register(AzureVMSSListVirtualMachineScaleSetsResponse.class, 19083);
    kryo.register(AzureVMSSTaskExecutionResponse.class, 19084);
    kryo.register(AzureVMSSTaskParameters.class, 19079);
    kryo.register(AzureVMSSTaskResponse.class, 19085);
    kryo.register(AzureVMSSTaskType.class, 19086);
    kryo.register(CapabilityType.class, 19004);
    kryo.register(ChartMuseumCapability.class, 19038);
    kryo.register(TaskType.class, 5005);
    kryo.register(CommandExecutionData.class, 5035);
    kryo.register(CommandExecutionResult.class, 5036);
    kryo.register(ConnectorValidationResult.class, 19059);
    kryo.register(CustomCommitAttributes.class, 19070);
    kryo.register(DelegateMetaInfo.class, 5372);
    kryo.register(DelegateRetryableException.class, 5521);
    kryo.register(DelegateTaskDetails.class, 19044);
    kryo.register(DelegateTaskNotifyResponseData.class, 5373);
    kryo.register(DelegateTaskPackage.class, 7150);
    kryo.register(DelegateTaskResponse.class, 5006);
    kryo.register(DelegateTaskResponse.ResponseCode.class, 5520);
    kryo.register(DirectK8sInfraDelegateConfig.class, 19102);
    kryo.register(ErrorNotifyResponseData.class, 5213);
    kryo.register(FetchType.class, 8030);
    kryo.register(GitAuthenticationDTO.class, 19063);
    kryo.register(GitAuthType.class, 19066);
    kryo.register(GitCommand.class, 19062);
    kryo.register(GitCommandExecutionResponse.class, 19067);
    kryo.register(GitCommandParams.class, 19061);
    kryo.register(GitCommandStatus.class, 19074);
    kryo.register(GitCommandType.class, 19071);
    kryo.register(GitConfigDTO.class, 19060);
    kryo.register(GitConnectionType.class, 19068);
    kryo.register(GitHTTPAuthenticationDTO.class, 19064);
    kryo.register(GitSSHAuthenticationDTO.class, 19065);
    kryo.register(GitStoreDelegateConfig.class, 19104);
    kryo.register(GitSyncConfig.class, 19069);
    kryo.register(HelmInstallationCapability.class, 19120);
    kryo.register(HttpConnectionExecutionCapability.class, 19003);
    kryo.register(HttpTaskParameters.class, 20002);
    kryo.register(K8sDeployRequest.class, 19101);
    kryo.register(K8sDeployResponse.class, 19099);
    kryo.register(K8sManifestDelegateConfig.class, 19103);
    kryo.register(K8sRollingDeployRequest.class, 19100);
    kryo.register(K8sTaskType.class, 7125);
    kryo.register(KubernetesAuthCredentialDTO.class, 19058);
    kryo.register(KubernetesAuthDTO.class, 19050);
    kryo.register(KubernetesAuthType.class, 19051);
    kryo.register(KubernetesClientKeyCertDTO.class, 19053);
    kryo.register(KubernetesClusterConfigDTO.class, 19045);
    kryo.register(KubernetesClusterDetailsDTO.class, 19049);
    kryo.register(KubernetesConnectionTaskParams.class, 19057);
    kryo.register(KubernetesConnectionTaskResponse.class, 19056);
    kryo.register(KubernetesCredentialSpecDTO.class, 19047);
    kryo.register(KubernetesCredentialType.class, 19046);
    kryo.register(KubernetesDelegateDetailsDTO.class, 19048);
    kryo.register(KubernetesOpenIdConnectDTO.class, 19055);
    kryo.register(KubernetesServiceAccountDTO.class, 19054);
    kryo.register(KubernetesUserNamePasswordDTO.class, 19052);
    kryo.register(LbDetailsForAlbTrafficShift.class, 19037);
    kryo.register(LoadBalancerDetailsForBGDeployment.class, 19031);
    kryo.register(LoadBalancerType.class, 19032);
    kryo.register(PcfManifestsPackage.class, 19033);
    kryo.register(ProcessExecutorCapability.class, 19007);
    kryo.register(RemoteMethodReturnValueData.class, 5122);
    kryo.register(ScriptType.class, 5253);
    kryo.register(SecretDetail.class, 19001);
    kryo.register(SelectorCapability.class, 19098);
    kryo.register(ShellScriptApprovalTaskParameters.class, 20001);
    kryo.register(SmbConnectionCapability.class, 19119);
    kryo.register(SocketConnectivityExecutionCapability.class, 19009);
    kryo.register(SpotInstDeployTaskParameters.class, 19018);
    kryo.register(SpotInstDeployTaskResponse.class, 19017);
    kryo.register(SpotInstGetElastigroupJsonParameters.class, 19025);
    kryo.register(SpotInstGetElastigroupJsonResponse.class, 19028);
    kryo.register(SpotInstListElastigroupInstancesParameters.class, 19026);
    kryo.register(SpotInstListElastigroupInstancesResponse.class, 19029);
    kryo.register(SpotInstListElastigroupNamesParameters.class, 19027);
    kryo.register(SpotInstListElastigroupNamesResponse.class, 19030);
    kryo.register(SpotInstSetupTaskParameters.class, 19012);
    kryo.register(SpotInstSetupTaskResponse.class, 19016);
    kryo.register(SpotInstSwapRoutesTaskParameters.class, 19023);
    kryo.register(SpotInstTaskExecutionResponse.class, 19014);
    kryo.register(SpotInstTaskParameters.class, 19011);
    kryo.register(SpotInstTaskResponse.class, 19015);
    kryo.register(SpotInstTaskType.class, 19013);
    kryo.register(SpotinstTrafficShiftAlbDeployParameters.class, 19041);
    kryo.register(SpotinstTrafficShiftAlbDeployResponse.class, 19042);
    kryo.register(SpotinstTrafficShiftAlbSetupParameters.class, 19039);
    kryo.register(SpotinstTrafficShiftAlbSetupResponse.class, 19040);
    kryo.register(SpotinstTrafficShiftAlbSwapRoutesParameters.class, 19043);
    kryo.register(SystemEnvCheckerCapability.class, 19022);
    kryo.register(TaskData.class, 19002);
    kryo.register(YamlGitConfigDTO.class, 19087);
    kryo.register(YamlGitConfigDTO.RootFolder.class, 19095);
    kryo.register(AzureVMSSPreDeploymentData.class, 19106);
    kryo.register(SplunkConnectionTaskParams.class, 19109);
    kryo.register(SplunkConnectionTaskResponse.class, 19110);
    kryo.register(SplunkConnectorDTO.class, 19111);
    kryo.register(DockerAuthCredentialsDTO.class, 19112);
    kryo.register(DockerAuthenticationDTO.class, 19113);
    kryo.register(DockerAuthType.class, 19114);
    kryo.register(DockerConnectorDTO.class, 19115);
    kryo.register(DockerUserNamePasswordDTO.class, 19116);
    kryo.register(DockerTestConnectionTaskParams.class, 19117);
    kryo.register(DockerTestConnectionTaskResponse.class, 19118);
    kryo.register(ArtifactTaskParameters.class, 19300);
    kryo.register(ArtifactTaskResponse.class, 19301);
    kryo.register(DockerArtifactDelegateRequest.class, 19302);
    kryo.register(DockerArtifactDelegateResponse.class, 19303);
    kryo.register(ArtifactTaskType.class, 19304);
    kryo.register(ArtifactDelegateResponse.class, 19305);
    kryo.register(ArtifactTaskExecutionResponse.class, 19306);
    kryo.register(ArtifactBuildDetailsNG.class, 19307);
    kryo.register(ArtifactSourceType.class, 19308);
    kryo.register(DelegateStringResponseData.class, 19309);
    kryo.register(AzureVMSSSetupTaskParameters.class, 19310);
    kryo.register(AzureVMSSListVMDataParameters.class, 19311);
    kryo.register(AzureVMSSListLoadBalancersNamesParameters.class, 19312);
    kryo.register(AzureVMSSListLoadBalancerBackendPoolsNamesParameters.class, 19313);
    kryo.register(AzureVMSSDeployTaskParameters.class, 19314);
    kryo.register(AzureLoadBalancerDetailForBGDeployment.class, 19315);
    kryo.register(AzureVMInstanceData.class, 19316);
    kryo.register(AzureVMSSDeployTaskResponse.class, 19317);
    kryo.register(AzureVMSSListLoadBalancerBackendPoolsNamesResponse.class, 19318);
    kryo.register(AzureVMSSListLoadBalancersNamesResponse.class, 19319);
    kryo.register(AzureVMSSListVMDataResponse.class, 19320);
    kryo.register(AzureVMSSSetupTaskResponse.class, 19321);
    kryo.register(AzureVMSSSwitchRoutesResponse.class, 19322);
    kryo.register(AzureVMSSSwitchRouteTaskParameters.class, 19323);
    kryo.register(GitFetchRequest.class, 19324);
    kryo.register(GitFetchFilesConfig.class, 19325);
    kryo.register(GitFetchResponse.class, 19326);
    kryo.register(TaskStatus.class, 19327);
    kryo.register(K8sRollingDeployResponse.class, 19328);
    kryo.register(StepStatusTaskParameters.class, 19329);
    kryo.register(StepStatusTaskResponseData.class, 19330);
    kryo.register(StepStatus.class, 19331);
    kryo.register(StepMapOutput.class, 19332);
    kryo.register(StepExecutionStatus.class, 19333);
    kryo.register(GitConnectionNGCapability.class, 19334);
    kryo.register(GcpRequest.RequestType.class, 19335);
    kryo.register(GcpValidationRequest.class, 19336);
    kryo.register(GcpValidationTaskResponse.class, 19337);
    kryo.register(SSHConfigValidationTaskResponse.class, 19338);
    kryo.register(AzureConfigDTO.class, 19339);
    kryo.register(AzureVMAuthDTO.class, 19340);
    kryo.register(AzureVMAuthType.class, 19341);
    kryo.register(KubernetesCredentialDTO.class, 19342);
    kryo.register(ExecutionCapability.class, 19343);
    kryo.register(JiraConnectorDTO.class, 19344);
    kryo.register(GcpConnectorDTO.class, 19345);
    kryo.register(GcpConnectorCredentialDTO.class, 19346);
    kryo.register(GcpCredentialType.class, 19347);
    kryo.register(GcpConstants.class, 19348);
    kryo.register(GcpDelegateDetailsDTO.class, 19349);
    kryo.register(GcpManualDetailsDTO.class, 19350);
    kryo.register(AwsConnectorDTO.class, 19351);
    kryo.register(AwsConstants.class, 19352);
    kryo.register(AwsCredentialDTO.class, 19353);
    kryo.register(AwsCredentialSpecDTO.class, 19354);
    kryo.register(AwsCredentialType.class, 19355);
    kryo.register(AwsDelegateTaskResponse.class, 19356);
    kryo.register(AwsInheritFromDelegateSpecDTO.class, 19357);
    kryo.register(AwsManualConfigSpecDTO.class, 19358);
    kryo.register(AwsTaskParams.class, 19359);
    kryo.register(AwsTaskType.class, 19360);
    kryo.register(AwsValidateTaskResponse.class, 19361);
    kryo.register(CrossAccountAccessDTO.class, 19362);
    kryo.register(AzureMachineImageArtifactDTO.class, 19363);
    kryo.register(GalleryImageDefinitionDTO.class, 19364);
    kryo.register(OSType.class, 19365);
    kryo.register(ImageType.class, 19366);
    kryo.register(JiraTaskNGParameters.class, 19367);
    kryo.register(JiraTaskNGResponse.class, 19368);
    kryo.register(JiraIssueData.class, 19369);
    kryo.register(JiraConnectionTaskParams.class, 19370);
    kryo.register(JiraTestConnectionTaskNGResponse.class, 19371);
    kryo.register(ConnectorType.class, 19372);
    kryo.register(JSONArray.class, 19373);
    kryo.register(JSONObject.class, 19374);
    kryo.register(CVConnectorTaskParams.class, 19375);
    kryo.register(CVConnectorTaskResponse.class, 19376);
    kryo.register(BuildStatusPushResponse.class, 19377);
    kryo.register(BuildStatusPushResponse.Status.class, 19378);
    kryo.register(CIBuildPushParameters.class, 19379);
    kryo.register(CIBuildPushTaskType.class, 19380);
    kryo.register(CIBuildStatusPushParameters.class, 19381);
    kryo.register(AzureWebAppListWebAppDeploymentSlotsParameters.class, 19382);
    kryo.register(AzureWebAppListWebAppNamesParameters.class, 19383);
    kryo.register(AzureWebAppListWebAppDeploymentSlotsResponse.class, 19384);
    kryo.register(AzureWebAppListWebAppNamesResponse.class, 19385);
    kryo.register(AzureAppServiceTaskParameters.class, 19386);
    kryo.register(AzureAppServiceTaskResponse.class, 19387);
    kryo.register(AzureTaskParameters.class, 19388);
    kryo.register(AzureTaskResponse.class, 19389);
    kryo.register(AzureAppServiceTaskType.class, 19390);
    kryo.register(AzureAppServiceType.class, 19391);
    kryo.register(AzureTaskExecutionResponse.class, 19393);
    kryo.register(CIBuildSetupTaskParams.class, 19394);
    kryo.register(CIK8BuildTaskParams.class, 19395);
    kryo.register(CIK8PodParams.class, 19396);
    kryo.register(CIBuildSetupTaskParams.Type.class, 19397);
    kryo.register(CIContainerType.class, 19398);
    kryo.register(CIK8ContainerParams.class, 19399);
    kryo.register(ContainerParams.class, 19400);
    kryo.register(ContainerResourceParams.class, 19401);
    kryo.register(PodParams.class, 19402);
    kryo.register(CIClusterType.class, 19403);
    kryo.register(ExecuteCommandTaskParams.class, 19404);
    kryo.register(K8ExecuteCommandTaskParams.class, 19405);
    kryo.register(K8ExecCommandParams.class, 19406);
    kryo.register(ExecuteCommandTaskParams.Type.class, 19407);
    kryo.register(ShellScriptType.class, 19408);
    kryo.register(CIK8CleanupTaskParams.class, 19409);
    kryo.register(ImageDetailsWithConnector.class, 19410);
    kryo.register(EncryptedVariableWithType.class, 19411);
    kryo.register(EncryptedVariableWithType.Type.class, 19412);
    kryo.register(ContainerSecrets.class, 19413);
    kryo.register(PVCParams.class, 19414);
    kryo.register(SecretVariableDTO.class, 19415);
    kryo.register(SecretVariableDTO.Type.class, 19416);
    kryo.register(SecretVariableDetails.class, 19417);
    kryo.register(ConnectorDetails.class, 193418);
    kryo.register(CIK8ServicePodParams.class, 19419);
    kryo.register(HostAliasParams.class, 19420);
    kryo.register(CiK8sTaskResponse.class, 19421);
    kryo.register(K8sTaskExecutionResponse.class, 19422);
    kryo.register(PodStatus.class, 19423);
    kryo.register(PodStatus.Status.class, 19424);
    kryo.register(CIContainerStatus.class, 19425);
    kryo.register(CIContainerStatus.Status.class, 19426);
    kryo.register(ConnectorHeartbeatDelegateResponse.class, 19427);
    kryo.register(DelegateStringProgressData.class, 19428);
    kryo.register(GitSCMType.class, 19429);
    kryo.register(EnvVariableEnum.class, 19430);
    kryo.register(AzureWebAppListWebAppInstancesParameters.class, 19431);
    kryo.register(AzureWebAppListWebAppInstancesResponse.class, 19432);
    kryo.register(CommandUnitStatusProgress.class, 19433);
    kryo.register(DockerRegistryProviderType.class, 19434);
    kryo.register(K8sBGDeployRequest.class, 19435);
    kryo.register(K8sBGDeployResponse.class, 19436);
    kryo.register(K8sApplyRequest.class, 19437);
    kryo.register(HttpTaskParametersNg.class, 19438);
    kryo.register(HttpStepResponse.class, 19439);
    kryo.register(GithubHttpCredentialsDTO.class, 19440);
    kryo.register(GithubHttpAuthenticationType.class, 19441);
    kryo.register(GithubUsernamePasswordDTO.class, 19442);
    kryo.register(GitlabConnectorDTO.class, 19443);
    kryo.register(GithubConnectorDTO.class, 19444);
    kryo.register(GithubApiAccessDTO.class, 19445);
    kryo.register(GithubSshCredentialsDTO.class, 19446);
    kryo.register(GithubApiAccessSpecDTO.class, 19447);
    kryo.register(GithubAppSpecDTO.class, 19449);
    kryo.register(GithubTokenSpecDTO.class, 19450);
    kryo.register(GithubApiAccessType.class, 19451);
    kryo.register(GithubAuthenticationDTO.class, 19452);
    kryo.register(GithubCredentialsDTO.class, 19453);
    kryo.register(CEAwsConnectorDTO.class, 19454);
    kryo.register(AwsCurAttributesDTO.class, 19455);
    kryo.register(CEAwsFeatures.class, 19456);
    kryo.register(DeploymentSlotData.class, 19457);
  }
}
