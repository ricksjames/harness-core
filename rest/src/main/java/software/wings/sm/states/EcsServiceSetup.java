package software.wings.sm.states;

import static com.google.common.collect.Iterables.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.api.EcsServiceElement.EcsServiceElementBuilder.anEcsServiceElement;
import static software.wings.api.EcsServiceExecutionData.Builder.anEcsServiceExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.DeploymentConfiguration;
import com.amazonaws.services.ecs.model.LogConfiguration;
import com.amazonaws.services.ecs.model.PortMapping;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.ecs.model.TransportProtocol;
import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.api.EcsServiceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Application;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.container.EcsContainerTask;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.EnumData;
import software.wings.utils.EcsConvention;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by peeyushaggarwal on 2/3/17.
 */
@Attributes(description = "Settings for AWS load balancer and roles for working with ECS Service.")
public class EcsServiceSetup extends State {
  @Attributes(title = "Elastic Load Balancer")
  @EnumData(enumDataProvider = LoadBalancerDataProvider.class)
  private String loadBalancerName;

  @Attributes(title = "Target Group")
  @EnumData(enumDataProvider = LoadBalancerTargetGroupDataProvider.class)
  private String targetGroupArn;

  @Attributes(title = "Role", description = "Arn for the role required by ECS for registering targets on ELB.")
  @EnumData(enumDataProvider = AWSRolesDataProvider.class)
  private String roleArn;

  @Inject @Transient private transient AwsClusterService awsClusterService;

  @Inject @Transient private transient SettingsService settingsService;

  @Inject @Transient private transient ServiceResourceService serviceResourceService;

  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient private transient ArtifactStreamService artifactStreamService;

  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public EcsServiceSetup(String name) {
    super(name, ECS_SERVICE_SETUP.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Artifact artifact = workflowStandardParams.getArtifactForService(serviceId);
    String imageName = fetchArtifactImageName(artifact);

    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
    if (infrastructureMapping == null || !(infrastructureMapping instanceof EcsInfrastructureMapping)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid infrastructure type");
    }

    String clusterName = ((EcsInfrastructureMapping) infrastructureMapping).getClusterName();

    Service service = serviceResourceService.get(app.getUuid(), serviceId);
    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

    EcsContainerTask ecsContainerTask = (EcsContainerTask) serviceResourceService.getContainerTaskByDeploymentType(
        app.getAppId(), serviceId, DeploymentType.ECS.name());

    if (ecsContainerTask == null) {
      ecsContainerTask = new EcsContainerTask();
      EcsContainerTask.ContainerDefinition containerDefinition = new EcsContainerTask.ContainerDefinition();
      containerDefinition.setMemory(256);
      ecsContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
    }

    String containerName = imageName.replace('/', '_');

    List<ContainerDefinition> containerDefinitions =
        ecsContainerTask.getContainerDefinitions()
            .stream()
            .map(containerDefinition -> createContainerDefinition(imageName, containerName, containerDefinition))
            .collect(Collectors.toList());

    RegisterTaskDefinitionRequest registerTaskDefinitionRequest =
        new RegisterTaskDefinitionRequest()
            .withContainerDefinitions(containerDefinitions)
            .withFamily(EcsConvention.getTaskFamily(app.getName(), service.getName(), env.getName()));

    TaskDefinition taskDefinition = awsClusterService.createTask(computeProviderSetting, registerTaskDefinitionRequest);

    /*
    SettingAttribute loadBalancerSetting = settingsService.get(loadBalancerName);

    if (loadBalancerSetting == null ||
    !loadBalancerSetting.getValue().getType().equals(SettingVariableTypes.ALB.name())) { throw new
    WingsException(ErrorCode.INVALID_REQUEST, "message", "Load balancer is not of ALB type");
    }
    ApplicationLoadBalancerConfig albConfig = (ApplicationLoadBalancerConfig) loadBalancerSetting.getValue();
    */

    String ecsServiceName = EcsConvention.getServiceName(taskDefinition.getFamily(), taskDefinition.getRevision());

    String lastEcsServiceName = lastECSService(
        computeProviderSetting, clusterName, EcsConvention.getServiceNamePrefix(taskDefinition.getFamily()));

    CreateServiceRequest createServiceRequest =
        new CreateServiceRequest()
            .withServiceName(ecsServiceName)
            .withCluster(clusterName)
            .withDesiredCount(0)
            .withDeploymentConfiguration(
                new DeploymentConfiguration().withMaximumPercent(200).withMinimumHealthyPercent(100))
            .withTaskDefinition(taskDefinition.getFamily() + ":" + taskDefinition.getRevision());
    List<com.amazonaws.services.ecs.model.Service> services =
        awsClusterService.getServices(computeProviderSetting, clusterName);

    if (!isEmpty(containerDefinitions.get(0).getPortMappings())) {
      int containerPort = ecsContainerTask.getContainerDefinitions().get(0).getPortMappings().get(0).getContainerPort();

      createServiceRequest
          .withLoadBalancers(new com.amazonaws.services.ecs.model.LoadBalancer()
                                 .withContainerName(containerName)
                                 .withContainerPort(containerPort)
                                 .withTargetGroupArn(targetGroupArn))
          .withRole(roleArn);
    }

    awsClusterService.createService(computeProviderSetting, createServiceRequest);

    EcsServiceElement ecsServiceElement = anEcsServiceElement()
                                              .withUuid(serviceId)
                                              .withName(ecsServiceName)
                                              .withOldName(lastEcsServiceName)
                                              .withClusterName(clusterName)
                                              .build();
    return anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .addContextElement(ecsServiceElement)
        .addNotifyElement(ecsServiceElement)
        .withStateExecutionData(anEcsServiceExecutionData()
                                    .withEcsClusterName(clusterName)
                                    .withEcsServiceName(ecsServiceName)
                                    .withDockerImageName(imageName)
                                    .withLoadBalancerName(loadBalancerName)
                                    .withTargetGroupArn(targetGroupArn)
                                    .withRoleArn(roleArn)
                                    .build())
        .build();
  }

  private String lastECSService(SettingAttribute computeProviderSetting, String clusterName, String serviceNamePrefix) {
    List<com.amazonaws.services.ecs.model.Service> services =
        awsClusterService.getServices(computeProviderSetting, clusterName);
    if (services == null) {
      return null;
    }
    List<com.amazonaws.services.ecs.model.Service> serviceList =
        services.stream()
            .filter(
                service -> (service.getServiceName().startsWith(serviceNamePrefix) && service.getDesiredCount() > 0))
            .collect(Collectors.toList());

    com.amazonaws.services.ecs.model.Service lastECSService = null;
    for (com.amazonaws.services.ecs.model.Service service : serviceList) {
      if (lastECSService == null || service.getCreatedAt().compareTo(lastECSService.getCreatedAt()) > 0) {
        lastECSService = service;
      }
    }
    return lastECSService != null ? lastECSService.getServiceName() : null;
  }

  /**
   * Create container definition container definition.
   *
   * @param imageName                the image name
   * @param containerName            the container name
   * @param wingsContainerDefinition the wings container definition
   * @return the container definition
   */
  private ContainerDefinition createContainerDefinition(
      String imageName, String containerName, EcsContainerTask.ContainerDefinition wingsContainerDefinition) {
    ContainerDefinition containerDefinition = new ContainerDefinition().withName(containerName).withImage(imageName);

    if (wingsContainerDefinition.getCpu() != null && wingsContainerDefinition.getMemory().intValue() > 0) {
      containerDefinition.setCpu(wingsContainerDefinition.getCpu());
    }

    if (wingsContainerDefinition.getMemory() != null && wingsContainerDefinition.getMemory().intValue() > 0) {
      containerDefinition.setMemory(wingsContainerDefinition.getMemory());
    }

    if (wingsContainerDefinition.getPortMappings() != null) {
      List<PortMapping> portMappings = wingsContainerDefinition.getPortMappings()
                                           .stream()
                                           .map(portMapping
                                               -> new PortMapping()
                                                      .withContainerPort(portMapping.getContainerPort())
                                                      .withHostPort(portMapping.getHostPort())
                                                      .withProtocol(TransportProtocol.Tcp))
                                           .collect(Collectors.toList());
      containerDefinition.setPortMappings(portMappings);
    }

    List<String> commands = Optional.ofNullable(wingsContainerDefinition.getCommands())
                                .orElse(Collections.emptyList())
                                .stream()
                                .filter(s -> isNotBlank(s))
                                .collect(Collectors.toList());
    containerDefinition.setCommand(commands);

    if (wingsContainerDefinition.getLogConfiguration() != null) {
      EcsContainerTask.LogConfiguration wingsLogConfiguration = wingsContainerDefinition.getLogConfiguration();
      LogConfiguration logConfiguration = new LogConfiguration().withLogDriver(wingsLogConfiguration.getLogDriver());
      wingsLogConfiguration.getOptions().forEach(
          logOption -> logConfiguration.addOptionsEntry(logOption.getKey(), logOption.getValue()));
      containerDefinition.setLogConfiguration(logConfiguration);
    }

    if (wingsContainerDefinition.getStorageConfigurations() != null) {
      // TODO:: fill volume amd mount points here
    }

    return containerDefinition;
  }

  /**
   * Fetch artifact image name string.
   *
   * @param artifact the artifact
   * @return the string
   */
  private String fetchArtifactImageName(Artifact artifact) {
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());

    if (!(artifactStream instanceof DockerArtifactStream)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
          artifactStream.getArtifactStreamType() + " artifact source can't be used for Containers");
    }

    DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;

    return dockerArtifactStream.getImageName();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Gets load balancer setting id.
   *
   * @return the load balancer setting id
   */
  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  /**
   * Sets load balancer setting id.
   *
   * @param loadBalancerName the load balancer setting id
   */
  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  /**
   * Getter for property 'targetGroupArn'.
   *
   * @return Value for property 'targetGroupArn'.
   */
  public String getTargetGroupArn() {
    return targetGroupArn;
  }

  /**
   * Setter for property 'targetGroupArn'.
   *
   * @param targetGroupArn Value to set for property 'targetGroupArn'.
   */
  public void setTargetGroupArn(String targetGroupArn) {
    this.targetGroupArn = targetGroupArn;
  }

  /**
   * Getter for property 'roleArn'.
   *
   * @return Value for property 'roleArn'.
   */
  public String getRoleArn() {
    return roleArn;
  }

  /**
   * Setter for property 'roleArn'.
   *
   * @param roleArn Value to set for property 'roleArn'.
   */
  public void setRoleArn(String roleArn) {
    this.roleArn = roleArn;
  }

  public static final class Builder {
    private String id;
    private String name;
    private ContextElementType requiredContextElementType;
    private String stateType;
    private boolean rollback;
    private String loadBalancerName;
    private String targetGroupArn;
    private String roleArn;

    private Builder() {}

    public static Builder anEcsServiceSetup() {
      return new Builder();
    }

    public Builder withId(String id) {
      this.id = id;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withRequiredContextElementType(ContextElementType requiredContextElementType) {
      this.requiredContextElementType = requiredContextElementType;
      return this;
    }

    public Builder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    public Builder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public Builder withLoadBalancerName(String loadBalancerName) {
      this.loadBalancerName = loadBalancerName;
      return this;
    }

    public Builder withTargetGroupArn(String targetGroupArn) {
      this.targetGroupArn = targetGroupArn;
      return this;
    }

    public Builder withRoleArn(String roleArn) {
      this.roleArn = roleArn;
      return this;
    }

    public Builder but() {
      return anEcsServiceSetup()
          .withId(id)
          .withName(name)
          .withRequiredContextElementType(requiredContextElementType)
          .withStateType(stateType)
          .withRollback(rollback)
          .withLoadBalancerName(loadBalancerName)
          .withTargetGroupArn(targetGroupArn)
          .withRoleArn(roleArn);
    }

    public EcsServiceSetup build() {
      EcsServiceSetup ecsServiceSetup = new EcsServiceSetup(name);
      ecsServiceSetup.setId(id);
      ecsServiceSetup.setRequiredContextElementType(requiredContextElementType);
      ecsServiceSetup.setStateType(stateType);
      ecsServiceSetup.setRollback(rollback);
      ecsServiceSetup.setLoadBalancerName(loadBalancerName);
      ecsServiceSetup.setTargetGroupArn(targetGroupArn);
      ecsServiceSetup.setRoleArn(roleArn);
      return ecsServiceSetup;
    }
  }
}
