package software.wings.service.impl.yaml.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlType.ARTIFACT_SERVER;
import static software.wings.beans.yaml.YamlType.ARTIFACT_SERVER_OVERRIDE;
import static software.wings.beans.yaml.YamlType.ARTIFACT_STREAM;
import static software.wings.beans.yaml.YamlType.CLOUD_PROVIDER;
import static software.wings.beans.yaml.YamlType.CLOUD_PROVIDER_OVERRIDE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.DeploymentSpecification;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.utils.Validator;
import software.wings.verification.CVConfiguration;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author rktummala on 10/17/17
 */
@Singleton
public class YamlHelper {
  @Inject ServiceResourceService serviceResourceService;
  @Inject AppService appService;
  @Inject InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject CVConfigurationService cvConfigurationService;
  @Inject EnvironmentService environmentService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject InfrastructureMappingService infraMappingService;
  @Inject WorkflowService workflowService;
  @Inject PipelineService pipelineService;
  @Inject SettingsService settingsService;
  @Inject ApplicationManifestService applicationManifestService;
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject EntityUpdateService entityUpdateService;
  @Inject ConfigService configService;
  @Inject FeatureFlagService featureFlagService;
  @Inject TriggerService triggerService;

  public SettingAttribute getCloudProvider(String accountId, String yamlFilePath) {
    return getSettingAttribute(accountId, YamlType.CLOUD_PROVIDER, yamlFilePath);
  }

  public SettingAttribute getCloudProviderAtConnector(String accountId, String yamlFilePath) {
    return getSettingAttribute(accountId, yamlFilePath);
  }

  public SettingAttribute getArtifactServer(String accountId, String yamlFilePath) {
    return getSettingAttribute(accountId, YamlType.ARTIFACT_SERVER, yamlFilePath);
  }

  public SettingAttribute getArtifactServerAtConnector(String accountId, String yamlFilePath) {
    return getSettingAttribute(accountId, yamlFilePath);
  }

  public SettingAttribute getCollaborationProvider(String accountId, String yamlFilePath) {
    return getSettingAttribute(accountId, YamlType.COLLABORATION_PROVIDER, yamlFilePath);
  }

  public SettingAttribute getVerificationProvider(String accountId, String yamlFilePath) {
    return getSettingAttribute(accountId, YamlType.VERIFICATION_PROVIDER, yamlFilePath);
  }

  public SettingAttribute getLoadBalancerProvider(String accountId, String yamlFilePath) {
    return getSettingAttribute(accountId, YamlType.LOADBALANCER_PROVIDER, yamlFilePath);
  }

  public SettingAttribute getSettingAttribute(String accountId, YamlType yamlType, String yamlFilePath) {
    String settingAttributeName =
        extractEntityNameFromYamlPath(yamlType.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Setting Attribute name null in the given yaml file: " + yamlFilePath, settingAttributeName);
    return settingsService.getSettingAttributeByName(accountId, settingAttributeName);
  }

  public SettingAttribute getSettingAttribute(String accountId, String yamlFilePath) {
    if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
      YamlType yamlType = getSettingAttributeType(yamlFilePath);
      String settingAttributeName =
          extractParentEntityName(yamlType.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
      Validator.notNullCheck(
          "Setting Attribute name null in the given yaml file: " + yamlFilePath, settingAttributeName);
      return settingsService.getSettingAttributeByName(accountId, settingAttributeName);
    }
    return null;
  }

  public String getAppId(String accountId, String yamlFilePath) {
    Application app = getApp(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, app);
    return app.getUuid();
  }

  public Application getApp(String accountId, String yamlFilePath) {
    String appName = extractParentEntityName(YamlType.APPLICATION.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);

    Validator.notNullCheck("App name null in the given yaml file: " + yamlFilePath, appName);
    return appService.getAppByName(accountId, appName);
  }

  public String getAppName(String yamlFilePath) {
    return extractParentEntityName(YamlType.APPLICATION.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
  }

  public String getServiceId(String appId, String yamlFilePath) {
    Service service = getService(appId, yamlFilePath);
    Validator.notNullCheck("Service null in the given yaml file: " + yamlFilePath, service);
    return service.getUuid();
  }

  public Trigger getTrigger(String appId, String yamlFilePath) {
    String triggerName =
        extractEntityNameFromYamlPath(YamlType.TRIGGER.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Trigger name null in the given yaml file: " + yamlFilePath, triggerName);

    PageRequest<Trigger> pageRequest =
        aPageRequest().addFilter("appId", Operator.EQ, appId).addFilter("name", Operator.EQ, triggerName).build();

    Optional<Trigger> trigger = triggerService.list(pageRequest).getResponse().stream().findFirst();
    if (!trigger.isPresent()) {
      Validator.notNullCheck("Could not lookup trigger for the yaml file: " + yamlFilePath, triggerName);
    }

    return trigger.get();
  }

  public Service getService(String appId, String yamlFilePath) {
    String serviceName = extractParentEntityName(YamlType.SERVICE.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Service name null in the given yaml file: " + yamlFilePath, serviceName);
    return serviceResourceService.getServiceByName(appId, serviceName);
  }

  public Service getServiceOverrideFromAppManifestPath(String appId, String yamlFilePath) {
    String serviceOverrideName = extractParentEntityName(
        YamlType.APPLICATION_MANIFEST_VALUES_ENV_SERVICE_OVERRIDE.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
    if (isNotBlank(serviceOverrideName)) {
      return serviceResourceService.getServiceByName(appId, serviceOverrideName, false);
    }

    return null;
  }

  public AppManifestKind getAppManifestKindFromPath(String yamlFilePath) {
    String kind = extractParentEntityName(
        YamlType.APPLICATION_MANIFEST_VALUES_ENV_OVERRIDE.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
    String kind2 = extractParentEntityName(
        YamlType.APPLICATION_MANIFEST_VALUES_SERVICE_OVERRIDE.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
    if (isNotBlank(kind) || isNotBlank(kind2)) {
      return AppManifestKind.VALUES;
    }
    return AppManifestKind.K8S_MANIFEST;
  }

  public ApplicationManifest getApplicationManifest(String appId, String yamlFilePath) {
    Service service = null;
    Environment environment = null;

    String serviceName = extractParentEntityName(YamlType.SERVICE.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
    if (isNotBlank(serviceName)) {
      service = serviceResourceService.getServiceByName(appId, serviceName);
    } else {
      String envName =
          extractParentEntityName(YamlType.ENVIRONMENT.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
      if (isNotBlank(envName)) {
        environment = environmentService.getEnvironmentByName(appId, envName);
        service = getServiceOverrideFromAppManifestPath(appId, yamlFilePath);
      }
    }
    AppManifestKind kind = getAppManifestKindFromPath(yamlFilePath);

    String serviceId = (service == null) ? null : service.getUuid();
    String envId = (environment == null) ? null : environment.getUuid();

    return applicationManifestService.getAppManifest(appId, envId, serviceId, kind);
  }

  public ManifestFile getManifestFile(String appId, String yamlFilePath, String fileName) {
    ApplicationManifest applicationManifest = getApplicationManifest(appId, yamlFilePath);
    Validator.notNullCheck("Application Manifest Null for yaml path: " + yamlFilePath, applicationManifest);
    return applicationManifestService.getManifestFileByFileName(applicationManifest.getUuid(), fileName);
  }

  public String getServiceName(String yamlFilePath) {
    return extractParentEntityName(YamlType.SERVICE.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
  }

  public String getInfrastructureProvisionerId(String appId, String yamlFilePath) {
    InfrastructureProvisioner provisioner = getInfrastructureProvisioner(appId, yamlFilePath);
    Validator.notNullCheck("InfrastructureProvisioner null in the given yaml file: " + yamlFilePath, provisioner);
    return provisioner.getUuid();
  }

  public InfrastructureProvisioner getInfrastructureProvisioner(String accountId, String yamlFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String provisionerName = getNameFromYamlFilePath(yamlFilePath);
    Validator.notNullCheck(
        "InfrastructureProvisioner name null in the given yaml file: " + yamlFilePath, provisionerName);
    return infrastructureProvisionerService.getByName(appId, provisionerName);
  }

  public CVConfiguration getCVConfiguration(String accountId, String yamlFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String configName = getNameFromYamlFilePath(yamlFilePath);
    String envId = getEnvironmentId(appId, yamlFilePath);

    return cvConfigurationService.getConfiguration(configName, appId, envId);
  }

  public String getEnvironmentId(String appId, String yamlFilePath) {
    Environment environment = getEnvironment(appId, yamlFilePath);
    Validator.notNullCheck("Environment null in the given yaml file: " + yamlFilePath, environment);
    return environment.getUuid();
  }

  public Environment getEnvironment(String appId, String yamlFilePath) {
    String envName = extractParentEntityName(YamlType.ENVIRONMENT.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Environment name null in the given yaml file: " + yamlFilePath, envName);
    return environmentService.getEnvironmentByName(appId, envName);
  }

  public Environment getEnvironmentFromAccount(String accountId, String yamlFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String envName = extractParentEntityName(YamlType.ENVIRONMENT.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Environment name null in the given yaml file: " + yamlFilePath, envName);
    return environmentService.getEnvironmentByName(appId, envName);
  }

  public String getEnvironmentName(String yamlFilePath) {
    return extractParentEntityName(YamlType.ENVIRONMENT.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
  }

  public String getArtifactStreamName(String yamlFilePath) {
    return extractEntityNameFromYamlPath(YamlType.ARTIFACT_STREAM.getPathExpression(), yamlFilePath, PATH_DELIMITER);
  }

  public ArtifactStream getArtifactStream(String accountId, String yamlFilePath) {
    if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
      String appId = getAppId(accountId, yamlFilePath);
      Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
      String serviceId = getServiceId(appId, yamlFilePath);
      Validator.notNullCheck("Service null in the given yaml file: " + yamlFilePath, serviceId);
      String artifactStreamName =
          extractEntityNameFromYamlPath(YamlType.ARTIFACT_STREAM.getPathExpression(), yamlFilePath, PATH_DELIMITER);
      Validator.notNullCheck("Artifact stream name null in the given yaml file: " + yamlFilePath, artifactStreamName);
      return artifactStreamService.getArtifactStreamByName(appId, serviceId, artifactStreamName);
    } else {
      YamlType entityType = getEntityType(yamlFilePath);
      if (entityType.equals(ARTIFACT_STREAM)) {
        String appId = getAppId(accountId, yamlFilePath);
        Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
        String serviceId = getServiceId(appId, yamlFilePath);
        Validator.notNullCheck("Service null in the given yaml file: " + yamlFilePath, serviceId);
        String artifactStreamName =
            extractEntityNameFromYamlPath(YamlType.ARTIFACT_STREAM.getPathExpression(), yamlFilePath, PATH_DELIMITER);
        Validator.notNullCheck("Artifact stream name null in the given yaml file: " + yamlFilePath, artifactStreamName);
        return artifactStreamService.getArtifactStreamByName(appId, serviceId, artifactStreamName);
      } else {
        String artifactStreamName =
            extractEntityNameFromYamlPath(entityType.getPathExpression(), yamlFilePath, PATH_DELIMITER);
        Validator.notNullCheck("Artifact stream name null in the given yaml file: " + yamlFilePath, artifactStreamName);
        SettingAttribute settingAttribute = getSettingAttribute(accountId, yamlFilePath);
        return artifactStreamService.getArtifactStreamByName(settingAttribute.getUuid(), artifactStreamName);
      }
    }
  }

  private YamlType getEntityType(String yamlFilePath) {
    if (matchWithRegex(ARTIFACT_STREAM.getPathExpression(), yamlFilePath)) {
      return ARTIFACT_STREAM;
    }
    if (matchWithRegex(YamlType.ARTIFACT_SERVER_ARTIFACT_STREAM_OVERRIDE.getPathExpression(), yamlFilePath)) {
      return YamlType.ARTIFACT_SERVER_ARTIFACT_STREAM_OVERRIDE;
    }
    if (matchWithRegex(YamlType.CLOUD_PROVIDER_ARTIFACT_STREAM_OVERRIDE.getPathExpression(), yamlFilePath)) {
      return YamlType.CLOUD_PROVIDER_ARTIFACT_STREAM_OVERRIDE;
    }
    return null;
  }

  private YamlType getSettingAttributeType(String yamlFilePath) {
    if (matchWithRegex(YamlType.ARTIFACT_SERVER_ARTIFACT_STREAM_OVERRIDE.getPathExpression(), yamlFilePath)
        || matchWithRegex(YamlType.ARTIFACT_SERVER_OVERRIDE.getPathExpression(), yamlFilePath)) {
      return YamlType.ARTIFACT_SERVER_OVERRIDE;
    }

    if (matchWithRegex(YamlType.CLOUD_PROVIDER_ARTIFACT_STREAM_OVERRIDE.getPathExpression(), yamlFilePath)
        || matchWithRegex(CLOUD_PROVIDER_OVERRIDE.getPathExpression(), yamlFilePath)) {
      return YamlType.CLOUD_PROVIDER_OVERRIDE;
    }
    return null;
  }

  private boolean matchWithRegex(String prefixRegex, String yamlFilePath) {
    Pattern pattern = Pattern.compile(prefixRegex);
    Matcher matcher = pattern.matcher(yamlFilePath);

    // Lets use this example, i want to extract service name from the path
    // regex - Setup/Applications/*/Services/*/
    // yamlFilePath - Setup/Applications/App1/Services/service1/Commands/command1
    if (matcher.find()) {
      return true;
    }
    return false;
  }

  public YamlType getYamlTypeFromSettingAttributePath(String yamlFilePath) {
    if (matchWithRegex(ARTIFACT_SERVER.getPathExpression(), yamlFilePath)) {
      return ARTIFACT_SERVER;
    }

    if (matchWithRegex(ARTIFACT_SERVER_OVERRIDE.getPathExpression(), yamlFilePath)) {
      return ARTIFACT_SERVER_OVERRIDE;
    }
    if (matchWithRegex(CLOUD_PROVIDER.getPathExpression(), yamlFilePath)) {
      return CLOUD_PROVIDER;
    }

    if (matchWithRegex(CLOUD_PROVIDER_OVERRIDE.getPathExpression(), yamlFilePath)) {
      return CLOUD_PROVIDER_OVERRIDE;
    }
    return null;
  }

  public ServiceCommand getServiceCommand(String accountId, String yamlFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String serviceId = getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("Service null in the given yaml file: " + yamlFilePath, serviceId);
    String serviceCommandName =
        extractEntityNameFromYamlPath(YamlType.COMMAND.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Service Command name null in the given yaml file: " + yamlFilePath, serviceCommandName);
    return serviceResourceService.getCommandByName(appId, serviceId, serviceCommandName);
  }

  public ConfigFile getServiceConfigFile(String accountId, String yamlFilePath, String targetFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String serviceId = getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("Service null in the given yaml file: " + yamlFilePath, serviceId);

    return configService.get(appId, serviceId, EntityType.SERVICE, targetFilePath);
  }

  public ConfigFile getEnvironmentConfigFile(String accountId, String yamlFilePath, String targetFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String envId = getEnvironmentId(appId, yamlFilePath);
    Validator.notNullCheck("Environment null in the given yaml file: " + yamlFilePath, envId);

    return configService.get(appId, envId, EntityType.ENVIRONMENT, targetFilePath);
  }

  public Workflow getWorkflow(String accountId, String yamlFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String workflowName =
        extractEntityNameFromYamlPath(YamlType.WORKFLOW.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Workflow name null in the given yaml file: " + yamlFilePath, workflowName);
    return workflowService.readWorkflowByName(appId, workflowName);
  }

  public Workflow getWorkflowFromName(String appId, String workflowName) {
    Workflow workflow = workflowService.readWorkflowByName(appId, workflowName);
    Validator.notNullCheck("workflow name does not exist " + workflowName, workflow);
    return workflow;
  }

  public String getWorkflowName(String appId, String workflowId) {
    String workflowName = workflowService.readWorkflow(appId, workflowId).getName();
    Validator.notNullCheck("workflow name does not exist " + workflowId, workflowName);
    return workflowName;
  }

  public String getPipelineName(String appId, String pipelineId) {
    String pipelineName = pipelineService.readPipeline(appId, pipelineId, false).getName();
    Validator.notNullCheck("workflow name does not exist " + pipelineId, pipelineName);
    return pipelineName;
  }

  public String getPipelineId(String appId, String pipelineName) {
    String pipelineId = pipelineService.getPipelineByName(appId, pipelineName).getUuid();
    Validator.notNullCheck("pipeline name does not exist " + pipelineName, pipelineId);
    return pipelineId;
  }

  public Pipeline getPipeline(String accountId, String yamlFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String pipelineName =
        extractEntityNameFromYamlPath(YamlType.PIPELINE.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Pipeline name null in the given yaml file: " + yamlFilePath, pipelineName);
    return pipelineService.getPipelineByName(appId, pipelineName);
  }

  public InfrastructureMapping getInfraMapping(String accountId, String yamlFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String envId = getEnvironmentId(appId, yamlFilePath);
    Validator.notNullCheck("Env null in the given yaml file: " + yamlFilePath, envId);
    String infraMappingName =
        extractEntityNameFromYamlPath(YamlType.INFRA_MAPPING.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Inframapping with name: " + infraMappingName, infraMappingName);
    return infraMappingService.getInfraMappingByName(appId, envId, infraMappingName);
  }

  public String extractParentEntityName(String regex, String yamlFilePath, String delimiter) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(yamlFilePath);

    // Lets use this example, i want to extract service name from the path
    // regex - Setup/Applications/*/Services/*/
    // yamlFilePath - Setup/Applications/App1/Services/service1/Commands/command1
    if (matcher.find()) {
      // first extract the value that matches the pattern
      // extractedValue - Setup/Applications/App1/Services/service1/
      String extractedValue = matcher.group();

      StringBuilder stringBuilder = new StringBuilder(extractedValue);
      // strip off the last character, which would be the delimiter
      // stringBuilder - Setup/Applications/App1/Services/service1
      stringBuilder.deleteCharAt(stringBuilder.length() - 1);
      // Get the sub string between the last delimiter and the end of the string
      // result - service1
      return stringBuilder.substring(stringBuilder.lastIndexOf(delimiter) + 1, stringBuilder.length());
    }
    return null;
  }

  public String extractEntityNameFromYamlPath(String regex, String yamlFilePath, String delimiter) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(yamlFilePath);

    // Lets use this example, i want to extract service name from the path
    // regex - Setup/Applications/.[^/]*/Services/.[^/]*/.[^/]*?.yaml
    // yamlFilePath - Setup/Applications/App1/Services/service1/service1.yaml
    if (matcher.find()) {
      // first extract the value that matches the pattern
      // extractedValue = service1.yaml
      String extractedValue = matcher.group();

      StringBuilder stringBuilder = new StringBuilder(extractedValue);
      // Get the sub string between the last delimiter and the .yaml suffix at the end
      // result - service1
      return stringBuilder.substring(stringBuilder.lastIndexOf(delimiter) + 1, stringBuilder.length() - 5);
    }
    return null;
  }

  /**
   * This works for all yaml except App, Service and Environment. The yamls for those entities end with Index.yaml since
   * those entities are folder entries. In other words, you could use this method to extract name from any leaf entity.
   * @param yamlFilePath yaml file path
   * @return
   */
  public String getNameFromYamlFilePath(String yamlFilePath) {
    return extractEntityNameFromYamlPath(YamlConstants.YAML_FILE_NAME_PATTERN, yamlFilePath, PATH_DELIMITER);
  }

  public Optional<Application> getApplicationIfPresent(String accountId, String yamlFilePath) {
    Application application = getApp(accountId, yamlFilePath);
    if (application == null) {
      return Optional.empty();
    } else {
      return Optional.of(application);
    }
  }

  public Optional<Service> getServiceIfPresent(String applicationId, String yamlFilePath) {
    Service service = getService(applicationId, yamlFilePath);
    if (service != null) {
      return Optional.of(service);
    }
    return Optional.empty();
  }

  public Optional<Environment> getEnvIfPresent(String applicationId, String yamlFilePath) {
    Environment environment = getEnvironment(applicationId, yamlFilePath);
    if (environment != null) {
      return Optional.of(environment);
    }
    return Optional.empty();
  }

  public InfrastructureMapping getInfraMappingByAppIdYamlPath(String applicationId, String envId, String yamlFilePath) {
    String infraMappingName =
        extractEntityNameFromYamlPath(YamlType.INFRA_MAPPING.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Inframapping with name: " + infraMappingName, infraMappingName);
    return infraMappingService.getInfraMappingByName(applicationId, envId, infraMappingName);
  }

  public InfrastructureProvisioner getInfrastructureProvisionerByAppIdYamlPath(
      String applicationId, String yamlFilePath) {
    String provisionerName = getNameFromYamlFilePath(yamlFilePath);
    Validator.notNullCheck(
        "InfrastructureProvisioner name null in the given yaml file: " + yamlFilePath, provisionerName);
    return infrastructureProvisionerService.getByName(applicationId, provisionerName);
  }

  public Pipeline getPipelineByAppIdYamlPath(String applicationId, String yamlFilePath) {
    String pipelineName =
        extractEntityNameFromYamlPath(YamlType.PIPELINE.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Pipeline name null in the given yaml file: " + yamlFilePath, pipelineName);
    return pipelineService.getPipelineByName(applicationId, pipelineName);
  }

  public Workflow getWorkflowByAppIdYamlPath(String applicationId, String yamlFilePath) {
    String workflowName =
        extractEntityNameFromYamlPath(YamlType.WORKFLOW.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Workflow name null in the given yaml file: " + yamlFilePath, workflowName);
    return workflowService.readWorkflowByName(applicationId, workflowName);
  }

  public ArtifactStream getArtifactStream(String applicationId, String serviceId, String yamlFilePath) {
    String artifactStreamName =
        extractEntityNameFromYamlPath(YamlType.ARTIFACT_STREAM.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Artifact stream name null in the given yaml file: " + yamlFilePath, artifactStreamName);
    return artifactStreamService.getArtifactStreamByName(applicationId, serviceId, artifactStreamName);
  }

  public String getArtifactStreamName(String applicationId, String artifactStreamId) {
    return artifactStreamService.get(artifactStreamId).getName();
  }

  public String getServiceNameFromArtifactId(String applicationId, String artifactStreamId) {
    String serviceId = artifactStreamService.get(artifactStreamId).getServiceId();
    return serviceResourceService.get(applicationId, serviceId).getName();
  }

  public ArtifactStream getArtifactStreamWithName(String applicationId, String serviceName, String artifactStreamName) {
    String serviceId = serviceResourceService.getServiceByName(applicationId, serviceName).getUuid();

    return artifactStreamService.getArtifactStreamByName(applicationId, serviceId, artifactStreamName);
  }

  public DeploymentSpecification getDeploymentSpecification(String applicationId, String serviceId, String subType) {
    if ("PCF".equals(subType)) {
      return serviceResourceService.getPcfServiceSpecification(applicationId, serviceId);
    }

    if ("HELM".equals(subType)) {
      return serviceResourceService.getHelmChartSpecification(applicationId, serviceId);
    }

    if ("AMI".equals(subType)) {
      return serviceResourceService.getUserDataSpecification(applicationId, serviceId);
    }

    if ("AWS_LAMBDA".equals(subType)) {
      return serviceResourceService.getLambdaSpecification(applicationId, serviceId);
    }

    if ("ECS_SERVICE_SPEC".equals(subType)) {
      return serviceResourceService.getEcsServiceSpecification(applicationId, serviceId);
    }

    if ("ECS".equals(subType) || "KUBERNETES".equals(subType)) {
      return serviceResourceService.getContainerTaskByDeploymentType(applicationId, serviceId, subType);
    }

    return null;
  }

  public String getYamlPathForEntity(Object entity) {
    return entityUpdateService.getEntityRootFilePath(entity);
  }
}
