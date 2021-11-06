package io.harness.ng;

import static java.util.stream.Collectors.toSet;

import io.harness.AccessControlClientConfiguration;
import io.harness.Microservice;
import io.harness.NgIteratorsConfig;
import io.harness.accesscontrol.AccessControlAdminClientConfiguration;
import io.harness.account.AccountConfig;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cache.CacheConfig;
import io.harness.cf.CfClientConfig;
import io.harness.enforcement.client.EnforcementClientConfiguration;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.ff.FeatureFlagConfig;
import io.harness.file.FileServiceConfiguration;
import io.harness.gitsync.GitSdkConfiguration;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.lock.DistributedLockImplementation;
import io.harness.logstreaming.LogStreamingServiceConfiguration;
import io.harness.mongo.MongoConfig;
import io.harness.ng.core.NextGenConfig;
import io.harness.notification.NotificationClientConfiguration;
import io.harness.opaclient.OpaServiceConfiguration;
import io.harness.outbox.OutboxPollConfiguration;
import io.harness.redis.RedisConfig;
import io.harness.remote.CEAwsSetupConfig;
import io.harness.remote.CEAzureSetupConfig;
import io.harness.remote.CEGcpSetupConfig;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.resourcegroupclient.remote.ResourceGroupClientConfig;
import io.harness.signup.SignupNotificationConfiguration;
import io.harness.telemetry.segment.SegmentConfiguration;
import io.harness.threading.ThreadPoolConfig;
import io.harness.timescaledb.TimeScaleDBConfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.util.Strings;
import com.google.common.collect.Lists;
import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.models.Contact;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.Path;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

@Getter
@OwnedBy(HarnessTeam.PL)
@Slf4j
public class NextGenConfiguration extends Configuration {
  public static final String SERVICE_ID = "ng-manager";
  public static final String CORE_PACKAGE = "io.harness.ng.core.remote";
  public static final String INVITE_PACKAGE = "io.harness.ng.core.invites.remote";
  public static final String CONNECTOR_PACKAGE = "io.harness.connector.apis.resource";
  public static final String GITOPS_PROVIDER_RESOURCE_PACKAGE = "io.harness.gitopsprovider.resource";
  public static final String GIT_SYNC_PACKAGE = "io.harness.gitsync";
  public static final String CDNG_RESOURCES_PACKAGE = "io.harness.cdng";
  public static final String OVERLAY_INPUT_SET_RESOURCE_PACKAGE = "io.harness.ngpipeline";
  public static final String YAML_PACKAGE = "io.harness.yaml";
  public static final String FILTER_PACKAGE = "io.harness.filter";
  public static final String SIGNUP_PACKAGE = "io.harness.signup";
  public static final String MOCKSERVER_PACKAGE = "io.harness.ng.core.acl.mockserver";
  public static final String ACCOUNT_PACKAGE = "io.harness.account.resource";
  public static final String LICENSE_PACKAGE = "io.harness.licensing.api.resource";
  public static final String POLLING_PACKAGE = "io.harness.polling.resource";
  public static final String ENFORCEMENT_PACKAGE = "io.harness.enforcement.resource";
  public static final String ENFORCEMENT_CLIENT_PACKAGE = "io.harness.enforcement.client.resources";
  public static final String ARTIFACTS_PACKAGE = "io.harness.ng.core.artifacts.resources";
  public static final String AUTHENTICATION_SETTINGS_PACKAGE = "io.harness.ng.authenticationsettings.resources";
  public static final String SERVICE_PACKAGE = "io.harness.ng.core.service.resources";
  public static final String CD_OVERVIEW_PACKAGE = "io.harness.ng.overview.resource";
  public static final String ACTIVITY_HISTORY_PACKAGE = "io.harness.ng.core.activityhistory.resource";
  public static final String SERVICE_ACCOUNTS_PACKAGE = "io.harness.ng.serviceaccounts.resource";
  public static final String BUCKETS_PACKAGE = "io.harness.ng.core.buckets.resources";
  public static final String CLUSTER_GCP_PACKAGE = "io.harness.ng.core.k8s.cluster.resources.gcp";
  public static final String WEBHOOK_PACKAGE = "io.harness.ng.webhook.resources";
  public static final String ENVIRONMENT_PACKAGE = "io.harness.ng.core.environment.resources";
  public static final String USERPROFILE_PACKAGE = "io.harness.ng.userprofile.resource";
  public static final String USER_PACKAGE = "io.harness.ng.core.user.remote";
  public static final String JIRA_PACKAGE = "io.harness.ng.jira.resources";
  public static final String EXECUTION_PACKAGE = "io.harness.ng.executions.resources";
  public static final String ENTITYSETUP_PACKAGE = "io.harness.ng.core.entitysetupusage.resource";
  public static final String SCHEMA_PACKAGE = "io.harness.ng.core.schema.resource";
  public static final String DELEGATE_PACKAGE = "io.harness.ng.core.delegate.resources";
  public static final String ACCESS_CONTROL_PACKAGE = "io.harness.ng.accesscontrol.resources";
  public static final String FEEDBACK_PACKAGE = "io.harness.ng.feedback.resources";
  public static final String INSTANCE_SYNC_PACKAGE = "io.harness.ng.instancesync.resources";
  public static final String INSTANCE_NG_PACKAGE = "io.harness.ng.instance";

  @JsonProperty("swagger") private SwaggerBundleConfiguration swaggerBundleConfiguration;
  @Setter @JsonProperty("mongo") private MongoConfig mongoConfig;
  @JsonProperty("commonPoolConfig") private ThreadPoolConfig commonPoolConfig;
  @JsonProperty("pmsSdkExecutionPoolConfig") private ThreadPoolConfig pmsSdkExecutionPoolConfig;
  @JsonProperty("pmsSdkOrchestrationEventPoolConfig") private ThreadPoolConfig pmsSdkOrchestrationEventPoolConfig;
  @JsonProperty("pmsMongo") private MongoConfig pmsMongoConfig;
  @JsonProperty("allowedOrigins") private List<String> allowedOrigins = Lists.newArrayList();
  @JsonProperty("managerClientConfig") private ServiceHttpClientConfig managerClientConfig;
  @JsonProperty("grpcClient") private GrpcClientConfig grpcClientConfig;
  @JsonProperty("grpcServer") private GrpcServerConfig grpcServerConfig;
  @JsonProperty("nextGen") private NextGenConfig nextGenConfig;
  @JsonProperty("ciDefaultEntityConfiguration") private CiDefaultEntityConfiguration ciDefaultEntityConfiguration;
  @JsonProperty("ngManagerClientConfig") private ServiceHttpClientConfig ngManagerClientConfig;
  @JsonProperty("pipelineServiceClientConfig") private ServiceHttpClientConfig pipelineServiceClientConfig;
  @JsonProperty("auditClientConfig") private ServiceHttpClientConfig auditClientConfig;
  @JsonProperty("ceNextGenClientConfig") private ServiceHttpClientConfig ceNextGenClientConfig;
  @JsonProperty("eventsFramework") private EventsFrameworkConfiguration eventsFrameworkConfiguration;
  @JsonProperty("redisLockConfig") private RedisConfig redisLockConfig;
  @JsonProperty(value = "enableAuth", defaultValue = "true") private boolean enableAuth;
  @JsonProperty(value = "ngIteratorsConfig") private NgIteratorsConfig ngIteratorsConfig;
  @JsonProperty("ceAwsSetupConfig") @Deprecated private CEAwsSetupConfig ceAwsSetupConfig;
  @JsonProperty("ceAzureSetupConfig") private CEAzureSetupConfig ceAzureSetupConfig;
  @JsonProperty("ceGcpSetupConfig") private CEGcpSetupConfig ceGcpSetupConfig;
  @JsonProperty(value = "enableAudit") private boolean enableAudit;
  @JsonProperty(value = "ngAuthUIEnabled") private boolean isNGAuthUIEnabled;
  @JsonProperty("pmsSdkGrpcServerConfig") private GrpcServerConfig pmsSdkGrpcServerConfig;
  @JsonProperty("pmsGrpcClientConfig") private GrpcClientConfig pmsGrpcClientConfig;
  @JsonProperty("shouldConfigureWithPMS") private Boolean shouldConfigureWithPMS;
  @JsonProperty("accessControlClient") private AccessControlClientConfiguration accessControlClientConfiguration;
  @JsonProperty("accountConfig") private AccountConfig accountConfig;
  @JsonProperty("logStreamingServiceConfig") private LogStreamingServiceConfiguration logStreamingServiceConfig;
  private OpaServiceConfiguration opaServerConfig;
  @JsonProperty("gitSyncServerConfig") private GrpcServerConfig gitSyncGrpcServerConfig;
  @JsonProperty("gitGrpcClientConfigs") private Map<Microservice, GrpcClientConfig> gitGrpcClientConfigs;
  @JsonProperty("shouldDeployWithGitSync") private Boolean shouldDeployWithGitSync;
  @JsonProperty("notificationClient") private NotificationClientConfiguration notificationClientConfiguration;
  @JsonProperty("resourceGroupClientConfig") private ResourceGroupClientConfig resourceGroupClientConfig;
  @JsonProperty("accessControlAdminClient")
  private AccessControlAdminClientConfiguration accessControlAdminClientConfiguration;
  @JsonProperty("outboxPollConfig") private OutboxPollConfiguration outboxPollConfig;
  @JsonProperty("segmentConfiguration") private SegmentConfiguration segmentConfiguration;
  @JsonProperty("gitSdkConfiguration") private GitSdkConfiguration gitSdkConfiguration;
  @JsonProperty("fileServiceConfiguration") private FileServiceConfiguration fileServiceConfiguration;
  @JsonProperty("baseUrls") private BaseUrls baseUrls;
  @JsonProperty("cfClientConfig") private CfClientConfig cfClientConfig;
  @JsonProperty("featureFlagConfig") private FeatureFlagConfig featureFlagConfig;
  @JsonProperty("timescaledb") private TimeScaleDBConfig timeScaleDBConfig;
  @JsonProperty("enableDashboardTimescale") private Boolean enableDashboardTimescale;
  @JsonProperty("distributedLockImplementation") private DistributedLockImplementation distributedLockImplementation;
  @JsonProperty("exportMetricsToStackDriver") private boolean exportMetricsToStackDriver;
  @JsonProperty("signupNotificationConfiguration")
  private SignupNotificationConfiguration signupNotificationConfiguration;
  @JsonProperty("cacheConfig") private CacheConfig cacheConfig;
  @JsonProperty(value = "scopeAccessCheckEnabled", defaultValue = "false") private boolean isScopeAccessCheckEnabled;
  @JsonProperty("hostname") String hostname;
  @JsonProperty("basePathPrefix") String basePathPrefix;
  @JsonProperty("enforcementClientConfiguration") EnforcementClientConfiguration enforcementClientConfiguration;
  @JsonProperty("ciManagerClientConfig") ServiceHttpClientConfig ciManagerClientConfig;

  // [secondary-db]: Uncomment this and the corresponding config in yaml file if you want to connect to another database
  //  @JsonProperty("secondary-mongo") MongoConfig secondaryMongoConfig;

  public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
    SwaggerBundleConfiguration defaultSwaggerBundleConfiguration = new SwaggerBundleConfiguration() {
      @JsonIgnore
      public BeanConfig build(String urlPattern) {
        if (Strings.isNullOrEmpty(getResourcePackage())) {
          throw new IllegalStateException(
              "Resource package needs to be specified for Swagger to correctly detect annotated resources");
        }

        final BeanConfig config = new BeanConfig() {
          @Override
          public Set<Class<?>> classes() {
            ConfigurationBuilder config = new ConfigurationBuilder();
            Set<String> acceptablePackages = new HashSet<String>();

            boolean allowAllPackages = false;

            final FilterBuilder filter = new FilterBuilder();
            if (getResourcePackage() != null && !"".equals(getResourcePackage())) {
              String[] parts = getResourcePackage().split(",");
              for (String pkg : parts) {
                if (!"".equals(pkg)) {
                  acceptablePackages.add(pkg);
                  filter.includePackage(pkg);
                  config.addUrls(ClasspathHelper.forPackage(pkg));
                }
              }
            } else {
              allowAllPackages = true;
            }

            config.setExpandSuperTypes(getExpandSuperTypes());
            config.filterInputsBy(filter);
            config.setScanners(new ResourcesScanner(), new TypeAnnotationsScanner(), new SubTypesScanner());

            Set<Class<?>> classes = new HashSet<>(getResourceClasses());

            /*
             * Find concrete types annotated with @Api, but with a supertype annotated with @Path.
             * This would handle split resources where the interface has jax-rs annotations
             * and the implementing class has Swagger annotations
             */

            Set<Class<?>> output = new HashSet<Class<?>>();
            for (Class<?> cls : classes) {
              if (allowAllPackages) {
                output.add(cls);
              } else {
                for (String pkg : acceptablePackages) {
                  // startsWith allows everything within a package
                  // the dots ensures that package siblings are not considered
                  if ((cls.getPackage().getName() + ".").startsWith(pkg + ".")) {
                    output.add(cls);
                    break;
                  }
                }
              }
            }
            return output;
          }
        };
        config.setTitle(getTitle());
        config.setVersion(getVersion());
        config.setDescription(getDescription());
        config.setContact(getContact());
        config.setLicense(getLicense());
        config.setLicenseUrl(getLicenseUrl());
        config.setTermsOfServiceUrl(getTermsOfServiceUrl());
        config.setPrettyPrint(isPrettyPrint());
        config.setBasePath(("/".equals(getContextRoot()) ? "" : getContextRoot()) + urlPattern);
        config.setResourcePackage(getResourcePackage());
        config.setSchemes(getSchemes());
        config.setHost(getHost());
        config.setScan(true);

        // Assign contact email/url after scan, since BeanConfig.scan will
        // create a new info.Contact instance, thus overriding any info.Contact
        // settings prior to scan.
        if (getContactEmail() != null || getContactUrl() != null) {
          if (config.getInfo().getContact() == null) {
            config.getInfo().setContact(new Contact());
          }
          if (getContactEmail() != null) {
            config.getInfo().getContact().setEmail(getContactEmail());
          }
          if (getContactUrl() != null) {
            config.getInfo().getContact().setUrl(getContactUrl());
          }
        }

        return config;
      }
    };

    String resourcePackage = String.join(",", getUniquePackages(getResourceClasses()));
    defaultSwaggerBundleConfiguration.setResourcePackage(resourcePackage);
    defaultSwaggerBundleConfiguration.setSchemes(new String[] {"https", "http"});
    defaultSwaggerBundleConfiguration.setHost(hostname);
    defaultSwaggerBundleConfiguration.setUriPrefix(basePathPrefix);
    defaultSwaggerBundleConfiguration.setTitle("CD NextGen API Reference");
    defaultSwaggerBundleConfiguration.setVersion("2.0");

    return Optional.ofNullable(swaggerBundleConfiguration).orElse(defaultSwaggerBundleConfiguration);
  }

  public static Collection<Class<?>> getResourceClasses() {
    Instant start = Instant.now();
    Reflections reflections = new Reflections(CORE_PACKAGE, CONNECTOR_PACKAGE, GITOPS_PROVIDER_RESOURCE_PACKAGE,
        GIT_SYNC_PACKAGE, CDNG_RESOURCES_PACKAGE, OVERLAY_INPUT_SET_RESOURCE_PACKAGE, YAML_PACKAGE, FILTER_PACKAGE,
        SIGNUP_PACKAGE, MOCKSERVER_PACKAGE, ACCOUNT_PACKAGE, LICENSE_PACKAGE, POLLING_PACKAGE, ENFORCEMENT_PACKAGE,
        ENFORCEMENT_CLIENT_PACKAGE, ARTIFACTS_PACKAGE, AUTHENTICATION_SETTINGS_PACKAGE, CD_OVERVIEW_PACKAGE,
        ACTIVITY_HISTORY_PACKAGE, SERVICE_PACKAGE, SERVICE_ACCOUNTS_PACKAGE, BUCKETS_PACKAGE, CLUSTER_GCP_PACKAGE,
        WEBHOOK_PACKAGE, ENVIRONMENT_PACKAGE, USERPROFILE_PACKAGE, JIRA_PACKAGE, EXECUTION_PACKAGE, ENTITYSETUP_PACKAGE,
        SCHEMA_PACKAGE, DELEGATE_PACKAGE, ACCESS_CONTROL_PACKAGE, FEEDBACK_PACKAGE, INSTANCE_SYNC_PACKAGE,
        INVITE_PACKAGE, USER_PACKAGE, INSTANCE_NG_PACKAGE);
    Collection<Class<?>> classes = reflections.getTypesAnnotatedWith(Path.class);
    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    log.info("Getting resources by reflection started at {}, took {} milliseconds and ended at {}.", start, timeElapsed,
        finish);
    return classes;
  }

  private static Set<String> getUniquePackages(Collection<Class<?>> classes) {
    return classes.stream().map(aClass -> aClass.getPackage().getName()).collect(toSet());
  }

  public static Set<String> getUniquePackagesContainingResources() {
    return getResourceClasses().stream().map(aClass -> aClass.getPackage().getName()).collect(toSet());
  }
}
