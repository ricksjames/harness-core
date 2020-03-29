package software.wings.service.impl.yaml;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.exception.WingsException.USER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.microservice.NotifyEngineTarget.GENERAL;
import static io.harness.persistence.HPersistence.upsertReturnNewOptions;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.EntityType.APPLICATION;
import static software.wings.beans.yaml.GitCommandRequest.gitRequestTimeout;
import static software.wings.beans.yaml.YamlConstants.APPLICATIONS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.APPLICATION_FOLDER_PATH;
import static software.wings.beans.yaml.YamlConstants.ARTIFACT_SOURCES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.CLOUD_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.COLLABORATION_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.DEFAULTS_YAML;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;
import static software.wings.beans.yaml.YamlConstants.GLOBAL_TEMPLATE_LIBRARY_FOLDER;
import static software.wings.beans.yaml.YamlConstants.LOAD_BALANCERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.NOTIFICATION_GROUPS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER;
import static software.wings.beans.yaml.YamlConstants.VERIFICATION_PROVIDERS_FOLDER;
import static software.wings.service.impl.yaml.YamlProcessingLogContext.BRANCH_NAME;
import static software.wings.service.impl.yaml.YamlProcessingLogContext.CHANGESET_ID;
import static software.wings.service.impl.yaml.YamlProcessingLogContext.GIT_CONNECTOR_ID;
import static software.wings.service.impl.yaml.YamlProcessingLogContext.WEBHOOK_TOKEN;
import static software.wings.yaml.gitSync.YamlGitConfig.BRANCH_NAME_KEY;
import static software.wings.yaml.gitSync.YamlGitConfig.GIT_CONNECTOR_ID_KEY;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import io.fabric8.utils.Strings;
import io.harness.beans.DelegateTask;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SortOrder.OrderType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.delegate.beans.TaskData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.mongo.ProcessTimeLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.HIterator;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.JsonUtils;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.GitCommit;
import software.wings.beans.GitCommit.GitCommitKeys;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.GitConnectionErrorAlert;
import software.wings.beans.alert.GitSyncErrorAlert;
import software.wings.beans.trigger.WebhookSource;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitDiffRequest;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.GitFileChange.Builder;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.dl.WingsPersistence;
import software.wings.exception.YamlProcessingException.ChangeWithErrorMsg;
import software.wings.service.impl.AppLogContext;
import software.wings.service.impl.EntityTypeLogContext;
import software.wings.service.impl.trigger.WebhookEventUtils;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.sync.GitSyncService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.CryptoUtils;
import software.wings.yaml.YamlVersion;
import software.wings.yaml.directory.DirectoryPath;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.errorhandling.GitSyncError.GitSyncErrorKeys;
import software.wings.yaml.gitSync.GitSyncWebhook;
import software.wings.yaml.gitSync.GitSyncWebhook.GitSyncWebhookKeys;
import software.wings.yaml.gitSync.GitWebhookRequestAttributes;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;
import software.wings.yaml.gitSync.YamlGitConfig.YamlGitConfigKeys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.core.HttpHeaders;

/**
 * The type Yaml git sync service.
 */
@ValidateOnExecution
@Singleton
@Slf4j
public class YamlGitServiceImpl implements YamlGitService {
  /**
   * The constant SETUP_ENTITY_ID.
   */
  public static final String SETUP_ENTITY_ID = "setup";
  public static final List<GitCommit.Status> GIT_COMMIT_PROCESSED_STATUS =
      ImmutableList.of(GitCommit.Status.COMPLETED, GitCommit.Status.COMPLETED_WITH_ERRORS);

  public static final List<GitCommit.Status> GIT_COMMIT_ALL_STATUS_LIST = ImmutableList.<GitCommit.Status>builder()
                                                                              .addAll(GIT_COMMIT_PROCESSED_STATUS)
                                                                              .add(GitCommit.Status.FAILED)
                                                                              .add(GitCommit.Status.SKIPPED)
                                                                              .build();

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private YamlService yamlService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private SecretManager secretManager;
  @Inject private ExecutorService executorService;
  @Inject private DelegateService delegateService;
  @Inject private AlertService alertService;
  @Inject private SettingsService settingsService;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private AppService appService;
  @Inject private YamlGitService yamlGitSyncService;
  @Inject YamlHelper yamlHelper;
  @Inject private WebhookEventUtils webhookEventUtils;
  @Inject private FeatureFlagService featureFlagService;
  @Inject GitSyncService gitSyncService;

  /**
   * Gets the yaml git sync info by entityId
   *
   * @return the rest response
   */
  @Override
  public YamlGitConfig get(String accountId, String entityId, EntityType entityType) {
    return wingsPersistence.createQuery(YamlGitConfig.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter(YamlGitConfig.ENTITY_ID_KEY, entityId)
        .filter(YamlGitConfig.ENTITY_TYPE_KEY, entityType)
        .get();
  }

  @Override
  public PageResponse<YamlGitConfig> list(PageRequest<YamlGitConfig> req) {
    return wingsPersistence.query(YamlGitConfig.class, req);
  }

  @Override
  public YamlGitConfig save(YamlGitConfig ygs) {
    return save(ygs, true);
  }

  @Override
  public YamlGitConfig save(YamlGitConfig ygs, boolean performFullSync) {
    notNullCheck("application id cannot be empty", ygs.getAppId());

    ygs.setSyncMode(SyncMode.BOTH);
    YamlGitConfig yamlGitSync = wingsPersistence.saveAndGet(YamlGitConfig.class, ygs);
    if (performFullSync) {
      executorService.submit(() -> fullSync(ygs.getAccountId(), ygs.getEntityId(), ygs.getEntityType(), true));
    }

    return yamlGitSync;
  }

  @Override
  public GitConfig getGitConfig(YamlGitConfig ygs) {
    GitConfig gitConfig = null;
    if (EmptyPredicate.isNotEmpty(ygs.getGitConnectorId())) {
      SettingAttribute settingAttributeForGitConnector = settingsService.get(ygs.getGitConnectorId());
      if (settingAttributeForGitConnector == null) {
        logger.info(GIT_YAML_LOG_PREFIX + "Setting attribute deleted with connector Id [{}]", ygs.getGitConnectorId());
        return null;
      }
      gitConfig = (GitConfig) settingAttributeForGitConnector.getValue();
      if (gitConfig != null) {
        gitConfig.setBranch(ygs.getBranchName());
        if (EmptyPredicate.isNotEmpty(gitConfig.getSshSettingId())) {
          SettingAttribute settingAttributeForSshKey = getAndDecryptSettingAttribute(gitConfig.getSshSettingId());
          gitConfig.setSshSettingAttribute(settingAttributeForSshKey);
        }
      }
    } else {
      // This is to support backward compatibility. Should be removed once we move to using gitConnector completely
      if (EmptyPredicate.isNotEmpty(ygs.getSshSettingId())) {
        SettingAttribute settingAttributeForSshKey = getAndDecryptSettingAttribute(ygs.getSshSettingId());
        gitConfig = ygs.getGitConfig(settingAttributeForSshKey);
      } else {
        gitConfig = ygs.getGitConfig(null);
      }
    }

    return gitConfig;
  }

  @Override
  public SettingAttribute getAndDecryptSettingAttribute(String sshSettingId) {
    SettingAttribute settingAttributeForSshKey = settingsService.get(sshSettingId);
    if (settingAttributeForSshKey != null) {
      HostConnectionAttributes attributeValue = (HostConnectionAttributes) settingAttributeForSshKey.getValue();
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails(attributeValue, GLOBAL_APP_ID, null);
      managerDecryptionService.decrypt(attributeValue, encryptionDetails);
      return settingAttributeForSshKey;
    }

    logger.warn(GIT_YAML_LOG_PREFIX + "Could not find setting attribute");
    return null;
  }

  /**
   * Updates the yaml git sync info by object type and entitytId (uuid)
   *
   * @param ygs the yamlGitSync info
   * @return the rest response
   */
  @Override
  public YamlGitConfig update(YamlGitConfig ygs) {
    return save(ygs);
  }

  @Override
  public void fullSync(String accountId, String entityId, EntityType entityType, boolean forcePush) {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    logger.info(GIT_YAML_LOG_PREFIX + "Performing git full-sync for account [{}] and entity [{}]", accountId, entityId);

    String appId = accountId.equals(entityId) ? GLOBAL_APP_ID : entityId;
    YamlGitConfig yamlGitConfig = yamlDirectoryService.weNeedToPushChanges(accountId, appId);

    if (yamlGitConfig != null) {
      try {
        List<GitFileChange> gitFileChanges = new ArrayList<>();
        List<GitFileChange> deletedGitFileChanges = new ArrayList<>();

        if (EntityType.ACCOUNT == entityType) {
          // Handle everything except for application
          gitFileChanges = obtainAccountOnlyGitFileChanges(accountId, true);
          deletedGitFileChanges = obtainAccountOnlyGitFileChangeForDelete(accountId);

        } else if (APPLICATION == entityType) {
          // Fetch application changeSets. The reason for special handling is that with application level yamlGitConfig,
          // each app can refer to different yamlGitConfig
          Application app = appService.get(appId);
          if (app != null) {
            gitFileChanges = obtainApplicationYamlGitFileChanges(accountId, app);
            deletedGitFileChanges = asList(generateGitFileChangeForApplicationDelete(accountId, app.getName()));
          }
        }

        if (gitFileChanges.size() > 0 && forcePush) {
          for (GitFileChange gitFileChange : deletedGitFileChanges) {
            gitFileChanges.add(0, gitFileChange);
          }
        }
        YamlChangeSet yamlChangeSet = obtainYamlChangeSet(accountId, appId, gitFileChanges, forcePush);

        discardGitSyncErrorForFullSync(accountId, appId);

        yamlChangeSetService.save(yamlChangeSet);
        final long processingTimeMs = stopwatch.elapsed(MILLISECONDS);
        try (ProcessTimeLogContext ignore = new ProcessTimeLogContext(processingTimeMs, OVERRIDE_ERROR);
             EntityTypeLogContext ignore1 = new EntityTypeLogContext(entityType, entityId, accountId, OVERRIDE_ERROR)) {
          logger.info(GIT_YAML_LOG_PREFIX + "Performed git full-sync successfully");
        }
      } catch (Exception ex) {
        logger.error(GIT_YAML_LOG_PREFIX + "Failed to perform git full-sync for account {} and entity {}",
            yamlGitConfig.getAccountId(), entityId, ex);
      }
    }
  }

  @Override
  public void syncForTemplates(String accountId, String appId) {
    YamlGitConfig yamlGitConfig = yamlDirectoryService.weNeedToPushChanges(accountId, appId);
    if (yamlGitConfig != null) {
      try {
        List<GitFileChange> gitFileChanges = new ArrayList<>();
        if (GLOBAL_APP_ID.equals(appId)) {
          gitFileChanges = obtainGlobalTemplates(accountId, true);
        } else {
          Application app = appService.get(appId);
          if (app != null) {
            gitFileChanges = obtainAppTemplateChanges(accountId, app);
          }
        }
        YamlChangeSet yamlChangeSet = obtainYamlChangeSetForNonFullSync(accountId, appId, gitFileChanges, true);
        yamlChangeSetService.save(yamlChangeSet);
      } catch (Exception ex) {
        logger.error(
            GIT_YAML_LOG_PREFIX + "Failed to perform template sync for account {} and app {}", accountId, appId, ex);
      }
    } else {
      logger.info("YamlGitConfig null for app {}", appId);
    }
  }

  private List<GitFileChange> obtainAccountOnlyGitFileChangeForDelete(String accountId) {
    List<GitFileChange> gitFileChanges = new ArrayList<>();

    gitFileChanges.add(generateGitFileChangeForDelete(accountId, CLOUD_PROVIDERS_FOLDER));
    gitFileChanges.add(generateGitFileChangeForDelete(accountId, ARTIFACT_SOURCES_FOLDER));
    gitFileChanges.add(generateGitFileChangeForDelete(accountId, COLLABORATION_PROVIDERS_FOLDER));
    gitFileChanges.add(generateGitFileChangeForDelete(accountId, LOAD_BALANCERS_FOLDER));
    gitFileChanges.add(generateGitFileChangeForDelete(accountId, VERIFICATION_PROVIDERS_FOLDER));
    gitFileChanges.add(generateGitFileChangeForDelete(accountId, NOTIFICATION_GROUPS_FOLDER));
    gitFileChanges.add(generateGitFileChangeForDelete(accountId, GLOBAL_TEMPLATE_LIBRARY_FOLDER));

    gitFileChanges.add(generateGitFileChangeForDelete(accountId, DEFAULTS_YAML));

    return gitFileChanges;
  }

  private GitFileChange generateGitFileChangeForDelete(String accountId, String entity) {
    return Builder.aGitFileChange()
        .withAccountId(accountId)
        .withChangeType(ChangeType.DELETE)
        .withFilePath(SETUP_FOLDER + "/" + entity)
        .build();
  }

  private GitFileChange generateGitFileChangeForApplicationDelete(String accountId, String appName) {
    return Builder.aGitFileChange()
        .withAccountId(accountId)
        .withChangeType(ChangeType.DELETE)
        .withFilePath(SETUP_FOLDER + "/" + APPLICATIONS_FOLDER + "/" + appName)
        .build();
  }

  private YamlChangeSet obtainYamlChangeSet(
      String accountId, String appId, List<GitFileChange> gitFileChangeList, boolean forcePush) {
    return YamlChangeSet.builder()
        .accountId(accountId)
        .status(Status.QUEUED)
        .queuedOn(System.currentTimeMillis())
        .forcePush(forcePush)
        .gitFileChanges(gitFileChangeList)
        .appId(appId)
        .fullSync(true)
        .build();
  }

  private YamlChangeSet obtainYamlChangeSetForNonFullSync(
      String accountId, String appId, List<GitFileChange> gitFileChangeList, boolean forcePush) {
    return YamlChangeSet.builder()
        .accountId(accountId)
        .status(Status.QUEUED)
        .queuedOn(System.currentTimeMillis())
        .forcePush(forcePush)
        .gitFileChanges(gitFileChangeList)
        .appId(appId)
        .fullSync(false)
        .build();
  }

  @Override
  public List<GitFileChange> obtainApplicationYamlGitFileChanges(String accountId, Application app) {
    DirectoryPath directoryPath = new DirectoryPath(SETUP_FOLDER);

    FolderNode applicationsFolder = new FolderNode(
        accountId, APPLICATIONS_FOLDER, Application.class, directoryPath.add(APPLICATIONS_FOLDER), yamlGitSyncService);

    yamlDirectoryService.doApplication(app.getUuid(), false, null, applicationsFolder, directoryPath);

    List<GitFileChange> gitFileChanges = new ArrayList<>();
    gitFileChanges = yamlDirectoryService.traverseDirectory(
        gitFileChanges, accountId, applicationsFolder, SETUP_FOLDER, true, false, Optional.empty());

    return gitFileChanges;
  }

  private List<GitFileChange> obtainGlobalTemplates(String accountId, boolean includeFiles) {
    List<GitFileChange> gitFileChanges = new ArrayList<>();
    DirectoryPath directoryPath = new DirectoryPath(SETUP_FOLDER);
    FolderNode templateFolder = yamlDirectoryService.doTemplateLibrary(accountId, directoryPath.clone(), GLOBAL_APP_ID,
        GLOBAL_TEMPLATE_LIBRARY_FOLDER, YamlVersion.Type.GLOBAL_TEMPLATE_LIBRARY);
    gitFileChanges = yamlDirectoryService.traverseDirectory(
        gitFileChanges, accountId, templateFolder, SETUP_FOLDER, includeFiles, true, Optional.empty());

    return gitFileChanges;
  }

  private List<GitFileChange> obtainAppTemplateChanges(String accountId, Application app) {
    DirectoryPath directoryPath = new DirectoryPath(SETUP_FOLDER);
    directoryPath.add(APPLICATIONS_FOLDER);
    DirectoryPath appPath = directoryPath.clone();
    appPath.add(app.getName());
    FolderNode appTemplates = yamlDirectoryService.doTemplateLibraryForApp(app, appPath.clone());

    List<GitFileChange> gitFileChanges = new ArrayList<>();
    return yamlDirectoryService.traverseDirectory(
        gitFileChanges, accountId, appTemplates, appPath.getPath(), true, false, Optional.empty());
  }

  private List<YamlChangeSet> obtainAllApplicationYamlChangeSet(
      String accountId, boolean forcePush, boolean onlyGitSyncConfiguredApps) {
    List<YamlChangeSet> yamlChangeSets = new ArrayList<>();
    List<Application> apps = appService.getAppsByAccountId(accountId);

    if (isEmpty(apps)) {
      return yamlChangeSets;
    }
    for (Application app : apps) {
      if (!onlyGitSyncConfiguredApps || gitSyncConfiguredForApp(app.getAppId(), accountId)) {
        List<GitFileChange> gitFileChanges = obtainApplicationYamlGitFileChanges(accountId, app);
        yamlChangeSets.add(obtainYamlChangeSet(accountId, app.getUuid(), gitFileChanges, forcePush));
      } else {
        logger.info("Git Sync not configured for appId =[{}]. Skip generating changeset.", app.getAppId());
      }
    }

    return yamlChangeSets;
  }
  private boolean gitSyncConfiguredForApp(String appId, String accountId) {
    return yamlDirectoryService.weNeedToPushChanges(accountId, appId) != null;
  }

  @Override
  public List<GitFileChange> performFullSyncDryRun(String accountId) {
    List<GitFileChange> gitFileChanges = new ArrayList<>();

    List<YamlChangeSet> yamlChangeSets = obtainChangeSetFromFullSyncDryRun(accountId, false);
    for (YamlChangeSet yamlChangeSet : yamlChangeSets) {
      gitFileChanges.addAll(yamlChangeSet.getGitFileChanges());
    }

    return gitFileChanges;
  }

  @Override
  public List<YamlChangeSet> obtainChangeSetFromFullSyncDryRun(
      String accountId, boolean onlyGitSyncConfiguredEntities) {
    try {
      logger.info("Performing full-sync dry-run for account {}", accountId);
      List<YamlChangeSet> yamlChangeSets = new ArrayList<>();

      if (!onlyGitSyncConfiguredEntities || isGitSyncConfiguredForAccount(accountId)) {
        List<GitFileChange> gitFileChanges = obtainAccountOnlyGitFileChanges(accountId, false);
        yamlChangeSets.add(obtainYamlChangeSet(accountId, GLOBAL_APP_ID, gitFileChanges, false));
      } else {
        logger.info("Git Sync not configured for accountId =[{}]. Skip generating changeset.", accountId);
      }

      yamlChangeSets.addAll(obtainAllApplicationYamlChangeSet(accountId, false, onlyGitSyncConfiguredEntities));

      logger.info("Performed full-sync dry-run for account {}", accountId);
      return yamlChangeSets;
    } catch (Exception ex) {
      logger.error(format("Failed to perform full-sync dry-run for account %s", accountId), ex);
    }

    return new ArrayList<>();
  }
  private boolean isGitSyncConfiguredForAccount(String accountId) {
    return yamlDirectoryService.weNeedToPushChanges(accountId, GLOBAL_APP_ID) != null;
  }

  private List<GitFileChange> obtainAccountOnlyGitFileChanges(String accountId, boolean includeFiles) {
    List<GitFileChange> gitFileChanges = new ArrayList<>();

    FolderNode top = yamlDirectoryService.getDirectory(accountId, SETUP_ENTITY_ID, false, null);
    gitFileChanges = yamlDirectoryService.traverseDirectory(
        gitFileChanges, accountId, top, "", includeFiles, true, Optional.empty());

    return gitFileChanges;
  }

  @Override
  public List<String> getAllYamlErrorsForAccount(String accountId) {
    try {
      logger.info("Getting all Yaml errors for account {}", accountId);
      FolderNode top = yamlDirectoryService.getDirectory(accountId, SETUP_ENTITY_ID, false, null);
      List<GitFileChange> gitFileChanges = new ArrayList<>();
      List<String> errorLog = new ArrayList<>();
      yamlDirectoryService.traverseDirectory(gitFileChanges, accountId, top, "", false, false, Optional.of(errorLog));
      logger.info("Got all Yaml errors for account {}", accountId);
      return errorLog;
    } catch (Exception ex) {
      logger.error(format("Failed to get all Yaml errors for account %s", accountId), ex);
    }
    return new ArrayList<>();
  }

  private List<Account> getAllAccounts() {
    PageRequest<Account> request =
        aPageRequest().withLimit(UNLIMITED).addFieldsIncluded("uuid").addFilter("appId", EQ, GLOBAL_APP_ID).build();
    return accountService.list(request);
  }

  @Override
  public void performFullSyncDryRunOnAllAccounts() {
    List<Account> accounts = getAllAccounts();
    accounts.forEach(account -> performFullSyncDryRun(account.getUuid()));
  }

  @Override
  public List<String> getAllYamlErrorsForAllAccounts() {
    List<Account> accounts = getAllAccounts();
    List<String> allErrors = new ArrayList<>();
    accounts.forEach(account -> allErrors.addAll(getAllYamlErrorsForAccount(account.getUuid())));
    return allErrors;
  }

  @Override
  public void handleHarnessChangeSet(YamlChangeSet yamlChangeSet, String accountId) {
    final Stopwatch stopwatch = Stopwatch.createStarted();

    String appId = yamlChangeSet.getAppId();
    String yamlChangeSetId = yamlChangeSet.getUuid();
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AppLogContext ignore2 = new AppLogContext(appId, OVERRIDE_ERROR);
         YamlProcessingLogContext ignore3 =
             YamlProcessingLogContext.builder().changeSetId(yamlChangeSetId).build(OVERRIDE_ERROR)) {
      logger.info(GIT_YAML_LOG_PREFIX + "Started handling harness -> git changeset");

      List<GitFileChange> gitFileChanges = yamlChangeSet.getGitFileChanges();
      YamlGitConfig yamlGitConfig = yamlDirectoryService.weNeedToPushChanges(accountId, appId);
      GitConfig gitConfig = yamlGitConfig != null ? getGitConfig(yamlGitConfig) : null;

      if (yamlGitConfig == null || gitConfig == null) {
        throw new GeneralException(
            format(GIT_YAML_LOG_PREFIX
                    + "YamlGitConfig: [%s] and gitConfig: [%s]  shouldn't be null for accountId [%s] and entity [%s]",
                yamlGitConfig, gitConfig, accountId, appId),
            USER);
      }

      ensureValidNameSyntax(gitFileChanges);

      logger.info(GIT_YAML_LOG_PREFIX + "Creating COMMIT_AND_PUSH git delegate task for entity");
      String waitId = generateUuid();
      List<String> yamlChangeSetIds = new ArrayList<>();
      yamlChangeSetIds.add(yamlChangeSetId);
      DelegateTask delegateTask = DelegateTask.builder()
                                      .async(true)
                                      .accountId(accountId)
                                      .appId(GLOBAL_APP_ID)
                                      .waitId(waitId)
                                      .data(TaskData.builder()
                                                .taskType(TaskType.GIT_COMMAND.name())
                                                .parameters(new Object[] {GitCommandType.COMMIT_AND_PUSH, gitConfig,
                                                    secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, null),
                                                    GitCommitRequest.builder()
                                                        .gitFileChanges(gitFileChanges)
                                                        .forcePush(true)
                                                        .yamlChangeSetIds(yamlChangeSetIds)
                                                        .yamlGitConfig(yamlGitConfig)
                                                        .build()})
                                                .timeout(gitRequestTimeout)
                                                .build())
                                      .build();

      waitNotifyEngine.waitForAllOn(
          GENERAL, new GitCommandCallback(accountId, yamlChangeSetId, GitCommandType.COMMIT_AND_PUSH), waitId);
      final String taskId = delegateService.queueTask(delegateTask);
      try (ProcessTimeLogContext ignore4 = new ProcessTimeLogContext(stopwatch.elapsed(MILLISECONDS), OVERRIDE_ERROR)) {
        logger.info(
            GIT_YAML_LOG_PREFIX + "Successfully queued harness->git changeset for processing with delegate taskId=[{}]",
            taskId);
      }
    }
  }

  /**
   * Check filePath is valid.
   *
   * @param gitFileChanges
   */
  @VisibleForTesting
  void ensureValidNameSyntax(List<GitFileChange> gitFileChanges) {
    if (isEmpty(gitFileChanges)) {
      return;
    }
    // Get all yamlTypes having non-empty filepath prefixes (these yaml types represent different file paths)
    List<YamlType> folderYamlTypes =
        Arrays.stream(YamlType.values()).filter(yamlType -> isNotEmpty(yamlType.getPathExpression())).collect(toList());

    // make sure, all filepaths to be synced with git are in proper format
    // e.g. Setup/Application/app_name/index.yaml is valid one, but
    // Setup/Application/app/name/index.yaml is invalid. (this case is happening id app was names as "app/name")
    // we do not want to allow this scenario.
    gitFileChanges.forEach(gitFileChange
        -> matchPathPrefix(gitFileChange.getFilePath().charAt(0) == '/' ? gitFileChange.getFilePath().substring(1)
                                                                        : gitFileChange.getFilePath(),
            folderYamlTypes));
  }

  private void matchPathPrefix(String filePath, List<YamlType> folderYamlTypes) {
    // only check for file and not directories

    if (Pattern.compile(YamlType.MANIFEST_FILE.getPathExpression()).matcher(filePath).matches()
        || Pattern.compile(YamlType.MANIFEST_FILE_VALUES_ENV_OVERRIDE.getPathExpression()).matcher(filePath).matches()
        || Pattern.compile(YamlType.MANIFEST_FILE_VALUES_ENV_SERVICE_OVERRIDE.getPathExpression())
               .matcher(filePath)
               .matches()
        || Pattern.compile(YamlType.MANIFEST_FILE_PCF_OVERRIDE_ENV_OVERRIDE.getPathExpression())
               .matcher(filePath)
               .matches()
        || Pattern.compile(YamlType.MANIFEST_FILE_PCF_OVERRIDE_ENV_SERVICE_OVERRIDE.getPathExpression())
               .matcher(filePath)
               .matches()) {
      return;
    }

    if (filePath.endsWith(YamlConstants.YAML_EXTENSION)) {
      if (folderYamlTypes.stream().noneMatch(
              yamlType -> Pattern.compile(yamlType.getPathExpression()).matcher(filePath).matches())) {
        throw new WingsException(
            "Invalid entity name, entity can not contain / in the name. Caused invalid file path: " + filePath, USER);
      }
    }
  }

  @Override
  public String validateAndQueueWebhookRequest(
      String accountId, String webhookToken, String yamlWebHookPayload, HttpHeaders headers) {
    final Stopwatch startedStopWatch = Stopwatch.createStarted();
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         YamlProcessingLogContext ignore2 =
             YamlProcessingLogContext.builder().webhookToken(webhookToken).build(OVERRIDE_ERROR)) {
      logger.info(GIT_YAML_LOG_PREFIX + "Started processing webhook request");
      List<SettingAttribute> settingAttributes =
          wingsPersistence.createQuery(SettingAttribute.class)
              .filter(ACCOUNT_ID_KEY, accountId)
              .filter(SettingAttribute.VALUE_TYPE_KEY, SettingVariableTypes.GIT.name())
              .asList();

      if (isEmpty(settingAttributes)) {
        logger.info(GIT_YAML_LOG_PREFIX + "Git connector not found for account");
        throw new InvalidRequestException("Git connector not found with webhook token " + webhookToken, USER);
      }

      String gitConnectorId = null;
      for (SettingAttribute settingAttribute : settingAttributes) {
        SettingValue settingValue = settingAttribute.getValue();

        if (settingValue instanceof GitConfig && webhookToken.equals(((GitConfig) settingValue).getWebhookToken())) {
          gitConnectorId = settingAttribute.getUuid();
          break;
        }
      }

      if (isEmpty(gitConnectorId)) {
        throw new InvalidRequestException("Git connector not found with webhook token " + webhookToken, USER);
      }

      boolean gitPingEvent = webhookEventUtils.isGitPingEvent(headers);
      if (gitPingEvent) {
        logger.info(GIT_YAML_LOG_PREFIX + "Ping event found. Skip processing");
        return "Found ping event. Only push events are supported";
      }

      final String branchName = obtainBranchFromPayload(yamlWebHookPayload, headers);

      if (isEmpty(branchName)) {
        logger.info(GIT_YAML_LOG_PREFIX + "Branch not found. webhookToken: {}, yamlWebHookPayload: {}, headers: {}",
            webhookToken, yamlWebHookPayload, headers);
        throw new InvalidRequestException("Branch not found from webhook payload", USER);
      }

      String headCommitId = obtainCommitIdFromPayload(yamlWebHookPayload, headers);

      if (isNotEmpty(headCommitId) && isCommitAlreadyProcessed(accountId, headCommitId)) {
        logger.info(GIT_YAML_LOG_PREFIX + "CommitId: [{}] already processed.", headCommitId);
        return "Commit already processed";
      }

      logger.info(GIT_YAML_LOG_PREFIX + " Found branch name =[{}], headCommitId=[{}]", branchName, headCommitId);
      YamlChangeSet yamlChangeSet =
          YamlChangeSet.builder()
              .appId(GLOBAL_APP_ID)
              .accountId(accountId)
              .gitToHarness(true)
              .status(Status.QUEUED)
              .gitWebhookRequestAttributes(GitWebhookRequestAttributes.builder()
                                               .webhookBody(yamlWebHookPayload)
                                               .gitConnectorId(gitConnectorId)
                                               .webhookHeaders(convertHeadersToJsonString(headers))
                                               .branchName(branchName)
                                               .headCommitId(headCommitId)
                                               .build())
              .gitFileChanges(new ArrayList<>())
              .build();
      final YamlChangeSet savedYamlChangeSet = yamlChangeSetService.save(yamlChangeSet);
      try (ProcessTimeLogContext ignore3 =
               new ProcessTimeLogContext(startedStopWatch.elapsed(MILLISECONDS), OVERRIDE_ERROR)) {
        logger.info(
            GIT_YAML_LOG_PREFIX + "Successfully accepted webhook request for processing as yamlChangeSetId=[{}]",
            savedYamlChangeSet.getUuid());
      }

      return "Successfully accepted webhook request for processing";
    }
  }

  private String convertHeadersToJsonString(HttpHeaders headers) {
    try {
      return JsonUtils.asJson(headers.getRequestHeaders());
    } catch (Exception ex) {
      logger.warn("Failed to convert request headers in json string", ex);
      return null;
    }
  }

  @Override
  public void handleGitChangeSet(YamlChangeSet yamlChangeSet, String accountId) {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    GitWebhookRequestAttributes gitWebhookRequestAttributes = yamlChangeSet.getGitWebhookRequestAttributes();
    String gitConnectorId = gitWebhookRequestAttributes.getGitConnectorId();
    String branchName = gitWebhookRequestAttributes.getBranchName();
    String headCommitId = gitWebhookRequestAttributes.getHeadCommitId();

    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         YamlProcessingLogContext ignore3 =
             getYamlProcessingLogContext(gitConnectorId, branchName, null, yamlChangeSet.getUuid())) {
      logger.info(
          GIT_YAML_LOG_PREFIX + "Started handling Git -> harness changeset with headCommit Id =[{}]", headCommitId);

      if (isNotEmpty(headCommitId) && isCommitAlreadyProcessed(accountId, headCommitId)) {
        logger.info(GIT_YAML_LOG_PREFIX + "CommitId: [{}] already processed.", headCommitId);
        yamlChangeSetService.updateStatus(accountId, yamlChangeSet.getUuid(), Status.SKIPPED);
        return;
      }

      List<YamlGitConfig> yamlGitConfigs = wingsPersistence.createQuery(YamlGitConfig.class)
                                               .filter(ACCOUNT_ID_KEY, accountId)
                                               .filter(GIT_CONNECTOR_ID_KEY, gitConnectorId)
                                               .filter(BRANCH_NAME_KEY, branchName)
                                               .asList();
      if (isEmpty(yamlGitConfigs)) {
        logger.info(GIT_YAML_LOG_PREFIX + "Git sync configuration not found");
        throw new InvalidRequestException("Git sync configuration not found with branch " + branchName, USER);
      }

      YamlGitConfig yamlGitConfig = yamlGitConfigs.get(0);
      List<String> yamlGitConfigIds = yamlGitConfigs.stream().map(YamlGitConfig::getUuid).collect(toList());
      final GitCommit lastProcessedGitCommitId = fetchLastProcessedGitCommitId(accountId, yamlGitConfigIds);

      final String processedCommit = lastProcessedGitCommitId == null ? null : lastProcessedGitCommitId.getCommitId();
      logger.info(GIT_YAML_LOG_PREFIX + "Last processed git commit found =[{}]", processedCommit);

      String waitId = generateUuid();
      GitConfig gitConfig = getGitConfig(yamlGitConfig);
      DelegateTask delegateTask = DelegateTask.builder()
                                      .async(true)
                                      .accountId(accountId)
                                      .appId(GLOBAL_APP_ID)
                                      .waitId(waitId)
                                      .data(TaskData.builder()
                                                .taskType(TaskType.GIT_COMMAND.name())
                                                .parameters(new Object[] {GitCommandType.DIFF, gitConfig,
                                                    secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, null),
                                                    GitDiffRequest.builder()
                                                        .lastProcessedCommitId(processedCommit)
                                                        .endCommitId(getEndCommitId(headCommitId, accountId))
                                                        .yamlGitConfig(yamlGitConfig)
                                                        .build(),
                                                    true /*excludeFilesOutsideSetupFolder */})
                                                .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                                .build())
                                      .build();

      waitNotifyEngine.waitForAllOn(
          GENERAL, new GitCommandCallback(accountId, yamlChangeSet.getUuid(), GitCommandType.DIFF), waitId);
      final String taskId = delegateService.queueTask(delegateTask);
      try (ProcessTimeLogContext ignore2 = new ProcessTimeLogContext(stopwatch.elapsed(MILLISECONDS), OVERRIDE_ERROR)) {
        logger.info(
            GIT_YAML_LOG_PREFIX + "Successfully queued git->harness changeset for processing with delegate taskId=[{}]",
            taskId);
      }

    } catch (Exception ex) {
      logger.error(format(GIT_YAML_LOG_PREFIX + "Unexpected error while processing git->harness changeset [%s]",
                       yamlChangeSet.getUuid()),
          ex);
      yamlChangeSetService.updateStatus(accountId, yamlChangeSet.getUuid(), Status.SKIPPED);
    }
  }
  private String getEndCommitId(String headCommitId, String accountId) {
    if (isEmpty(headCommitId)) {
      logger.warn("headCommitId cannot be deciphered from payload. Using HEAD for taking diff");
    }
    return isNotEmpty(headCommitId) ? headCommitId : null;
  }
  private YamlProcessingLogContext getYamlProcessingLogContext(
      String gitConnectorId, String branch, String webhookToken, String yamlChangeSetId) {
    return new YamlProcessingLogContext(NullSafeImmutableMap.<String, String>builder()
                                            .putIfNotNull(GIT_CONNECTOR_ID, gitConnectorId)
                                            .putIfNotNull(BRANCH_NAME, branch)
                                            .putIfNotNull(WEBHOOK_TOKEN, webhookToken)
                                            .putIfNotNull(CHANGESET_ID, yamlChangeSetId)
                                            .build(),
        OVERRIDE_ERROR);
  }

  @Override
  public boolean isCommitAlreadyProcessed(String accountId, String headCommit) {
    final Query<GitCommit> query = wingsPersistence.createQuery(GitCommit.class)
                                       .filter(GitCommitKeys.accountId, accountId)
                                       .filter(GitCommitKeys.commitId, headCommit)
                                       .field(GitCommitKeys.status)
                                       .in(GIT_COMMIT_ALL_STATUS_LIST);

    final GitCommit gitCommit = query.get();
    if (gitCommit != null) {
      logger.info(GIT_YAML_LOG_PREFIX + "Commit [id:{}] already processed [status:{}] on [date:{}] mode:[{}]",
          gitCommit.getCommitId(), gitCommit.getStatus(), gitCommit.getLastUpdatedAt(),
          gitCommit.getYamlChangeSet().isGitToHarness());
      return true;
    }
    return false;
  }
  private List<GitCommit.Status> getProcessedGitCommitStatusList(String accountId) {
    return GIT_COMMIT_PROCESSED_STATUS;
  }

  @Override
  public GitSyncWebhook getWebhook(String entityId, String accountId) {
    GitSyncWebhook gsw = wingsPersistence.createQuery(GitSyncWebhook.class)
                             .filter(GitSyncWebhookKeys.entityId, entityId)
                             .filter(GitSyncWebhookKeys.accountId, accountId)
                             .get();

    if (gsw != null) {
      return gsw;
    } else {
      // create a new GitSyncWebhook, save to Mongo and return it
      String newWebhookToken = CryptoUtils.secureRandAlphaNumString(40);
      gsw = GitSyncWebhook.builder().accountId(accountId).entityId(entityId).webhookToken(newWebhookToken).build();
      return wingsPersistence.saveAndGet(GitSyncWebhook.class, gsw);
    }
  }

  @Override
  public GitCommit saveCommit(GitCommit gitCommit) {
    return wingsPersistence.saveAndGet(GitCommit.class, gitCommit);
  }

  @Override
  public void processFailedChanges(
      String accountId, Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap, boolean gitToHarness) {
    if (failedYamlFileChangeMap.size() > 0) {
      failedYamlFileChangeMap.values().forEach(changeWithErrorMsg
          -> upsertGitSyncErrors(changeWithErrorMsg.getChange(), changeWithErrorMsg.getErrorMsg(), false));
      alertService.openAlert(
          accountId, GLOBAL_APP_ID, AlertType.GitSyncError, getGitSyncErrorAlert(accountId, gitToHarness));
    }
  }

  @Override
  public void raiseAlertForGitFailure(String accountId, String appId, ErrorCode errorCode, String errorMessage) {
    if (ErrorCode.GIT_DIFF_COMMIT_NOT_IN_ORDER == errorCode) {
      return;
    }
    if (ErrorCode.GIT_CONNECTION_ERROR == errorCode) {
      alertService.openAlert(
          accountId, appId, AlertType.GitConnectionError, getGitConnectionErrorAlert(accountId, errorMessage));
    } else {
      alertService.openAlert(
          accountId, appId, AlertType.GitSyncError, getGitSyncErrorAlert(accountId, errorMessage, false));
    }
  }

  @Override
  public void closeAlertForGitFailureIfOpen(String accountId, String appId, AlertType alertType, AlertData alertData) {
    alertService.closeAlert(accountId, appId, alertType, alertData);
  }

  private GitSyncErrorAlert getGitSyncErrorAlert(String accountId, boolean gitToHarness) {
    return GitSyncErrorAlert.builder()
        .accountId(accountId)
        .message("Unable to process changes from Git")
        .gitToHarness(gitToHarness)
        .build();
  }

  private GitSyncErrorAlert getGitSyncErrorAlert(String accountId, String errorMessage, boolean gitToHarness) {
    return GitSyncErrorAlert.builder().accountId(accountId).message(errorMessage).gitToHarness(gitToHarness).build();
  }

  private GitConnectionErrorAlert getGitConnectionErrorAlert(String accountId, String message) {
    return GitConnectionErrorAlert.builder().accountId(accountId).message(message).build();
  }

  @Override
  public <T extends Change> void upsertGitSyncErrors(T failedChange, String errorMessage, boolean fullSyncPath) {
    Query<GitSyncError> failedQuery = wingsPersistence.createQuery(GitSyncError.class)
                                          .filter(GitSyncError.ACCOUNT_ID_KEY, failedChange.getAccountId())
                                          .filter(GitSyncErrorKeys.yamlFilePath, failedChange.getFilePath());
    GitFileChange failedGitFileChange = (GitFileChange) failedChange;
    String failedCommitId = failedGitFileChange.getCommitId() != null ? failedGitFileChange.getCommitId() : "";
    String appId = obtainAppIdFromGitFileChange(failedChange.getAccountId(), failedChange.getFilePath());
    logger.info(String.format("Upsert git sync issue for file: %s", failedChange.getFilePath()));

    UpdateOperations<GitSyncError> failedUpdateOperations =
        wingsPersistence.createUpdateOperations(GitSyncError.class)
            .setOnInsert(GitSyncError.ID_KEY, generateUuid())
            .set(GitSyncError.ACCOUNT_ID_KEY, failedChange.getAccountId())
            .set("yamlFilePath", failedChange.getFilePath())
            .set("gitCommitId", failedCommitId)
            .set("changeType", failedChange.getChangeType().name())
            .set("failureReason",
                errorMessage != null ? errorMessage : "Reason could not be captured. Logs might have some info")
            .set("fullSyncPath", fullSyncPath)
            .set(APP_ID_KEY, appId);

    populateGitDetails(failedUpdateOperations, failedGitFileChange);

    final GitSyncError gitSyncError = failedQuery.get();

    // git sync error already exists
    if (gitSyncError != null) {
      // if fix got triggered from Git, it will come in through a new and valid commit id
      if (StringUtils.isNotBlank(failedCommitId)) {
        failedUpdateOperations.set("yamlContent", failedChange.getFileContent());
        failedUpdateOperations.unset("lastAttemptedYaml");
      }
      // if fix got triggered from UI, commit id will remain the same
      else {
        failedUpdateOperations.set("lastAttemptedYaml", failedChange.getFileContent());
      }
    }
    // if it's a new git sync error
    else {
      failedUpdateOperations.set("yamlContent", failedChange.getFileContent());
    }

    failedQuery.project(GitSyncError.ID_KEY, true);
    wingsPersistence.upsert(failedQuery, failedUpdateOperations, upsertReturnNewOptions);
  }
  private void populateGitDetails(
      UpdateOperations<GitSyncError> failedUpdateOperations, GitFileChange failedGitFileChange) {
    final YamlGitConfig yamlGitConfig = failedGitFileChange.getYamlGitConfig();
    if (yamlGitConfig != null) {
      final String gitConnectorId = Strings.emptyIfNull(yamlGitConfig.getGitConnectorId());
      final String branchName = Strings.emptyIfNull(yamlGitConfig.getBranchName());
      failedUpdateOperations.set(GitSyncErrorKeys.gitConnectorId, gitConnectorId);
      failedUpdateOperations.set(GitSyncErrorKeys.branchName, branchName);
      failedUpdateOperations.set(GitSyncErrorKeys.yamlGitConfigId, yamlGitConfig.getUuid());
    }
  }

  private String obtainAppIdFromGitFileChange(String accountId, String yamlFilePath) {
    String appId = GLOBAL_APP_ID;

    // Fetch appName from yamlPath, e.g. Setup/Applications/App1/Services/S1/index.yaml -> App1,
    // Setup/Artifact Servers/server.yaml -> null
    String appName = yamlHelper.getAppName(yamlFilePath);
    if (StringUtils.isNotBlank(appName)) {
      Application app = appService.getAppByName(accountId, appName);
      if (app != null) {
        appId = app.getUuid();
      }
    }

    return appId;
  }

  @Override
  public void removeGitSyncErrors(String accountId, List<GitFileChange> gitFileChangeList, boolean gitToHarness) {
    List<String> yamlFilePathList = gitFileChangeList.stream().map(GitFileChange::getFilePath).collect(toList());
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    query.field(GitSyncErrorKeys.yamlFilePath).in(yamlFilePathList);
    wingsPersistence.delete(query);
    closeAlertIfApplicable(accountId, gitToHarness);
  }

  @Override
  public RestResponse<List<GitSyncError>> listGitSyncErrors(String accountId) {
    PageRequest<GitSyncError> pageRequest = aPageRequest()
                                                .addFilter("accountId", EQ, accountId)
                                                .withLimit("500")
                                                .addOrder(GitSyncError.CREATED_AT_KEY, OrderType.ASC)
                                                .build();
    PageResponse<GitSyncError> response = wingsPersistence.query(GitSyncError.class, pageRequest);
    return RestResponse.Builder.aRestResponse().withResource(response.getResponse()).build();
  }

  @Override
  public long getGitSyncErrorCount(String accountId) {
    return wingsPersistence.createQuery(GitSyncError.class).filter(GitSyncError.ACCOUNT_ID_KEY, accountId).count();
  }

  @Override
  public RestResponse discardGitSyncError(String accountId, String errorId) {
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    query.filter("_id", errorId);
    wingsPersistence.delete(query);
    closeAlertIfApplicable(accountId, false);
    return RestResponse.Builder.aRestResponse().build();
  }

  @Override
  public RestResponse discardGitSyncErrorForFilePath(String accountId, String yamlFilePath) {
    Query<GitSyncError> query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    query.filter(GitSyncErrorKeys.yamlFilePath, yamlFilePath);
    wingsPersistence.delete(query);
    closeAlertIfApplicable(accountId, false);
    return RestResponse.Builder.aRestResponse().build();
  }

  @Override
  public RestResponse discardGitSyncErrorsForGivenPaths(String accountId, List<String> yamlFilePaths) {
    Query<GitSyncError> query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    query.field(GitSyncErrorKeys.yamlFilePath).in(yamlFilePaths);
    wingsPersistence.delete(query);
    closeAlertIfApplicable(accountId, false);
    return RestResponse.Builder.aRestResponse().build();
  }

  @Override
  public RestResponse discardAllGitSyncError(String accountId) {
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    wingsPersistence.delete(query);
    closeAlertIfApplicable(accountId, false);
    return RestResponse.Builder.aRestResponse().build();
  }

  @Override
  public RestResponse discardGitSyncErrorForFullSync(String accountId, String appId) {
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    query.filter("fullSyncPath", true);
    query.filter(APP_ID_KEY, appId);
    wingsPersistence.delete(query);
    closeAlertIfApplicable(accountId, false);
    return RestResponse.Builder.aRestResponse().build();
  }

  private void closeAlertIfApplicable(String accountId, boolean gitToHarness) {
    if (getGitSyncErrorCount(accountId) == 0) {
      alertService.closeAlert(
          accountId, GLOBAL_APP_ID, AlertType.GitSyncError, getGitSyncErrorAlert(accountId, gitToHarness));
    }
  }

  @Override
  public boolean checkApplicationChange(GitFileChange gitFileChange) {
    return StringUtils.startsWith(gitFileChange.getFilePath(), APPLICATION_FOLDER_PATH);
  }

  @Override
  public String obtainAppNameFromGitFileChange(GitFileChange gitFileChange) {
    String filePath = gitFileChange.getFilePath();

    String appFolderPath = APPLICATION_FOLDER_PATH + PATH_DELIMITER;
    String appPath = filePath.substring(appFolderPath.length());
    return appPath.substring(0, appPath.indexOf('/'));
  }

  @Override
  public void delete(String accountId, String entityId, EntityType entityType) {
    YamlGitConfig yamlGitConfig = get(accountId, entityId, entityType);
    if (yamlGitConfig == null) {
      return;
    }

    wingsPersistence.delete(yamlGitConfig);
  }

  private String obtainCommitIdFromPayload(String yamlWebHookPayload, HttpHeaders headers) {
    if (headers == null) {
      logger.info("Empty header found");
      return null;
    }

    WebhookSource webhookSource = webhookEventUtils.obtainWebhookSource(headers);
    webhookEventUtils.validatePushEvent(webhookSource, headers);

    Map<String, Object> payLoadMap =
        JsonUtils.asObject(yamlWebHookPayload, new TypeReference<Map<String, Object>>() {});

    return webhookEventUtils.obtainCommitId(webhookSource, headers, payLoadMap);
  }

  private String obtainBranchFromPayload(String yamlWebHookPayload, HttpHeaders headers) {
    if (headers == null) {
      logger.info("Empty header found");
      return null;
    }

    WebhookSource webhookSource = webhookEventUtils.obtainWebhookSource(headers);
    webhookEventUtils.validatePushEvent(webhookSource, headers);

    Map<String, Object> payLoadMap;
    try {
      payLoadMap = JsonUtils.asObject(yamlWebHookPayload, new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      logger.info("Webhook payload: " + yamlWebHookPayload, ex);
      throw new InvalidRequestException(
          "Failed to parse the webhook payload. Error " + ExceptionUtils.getMessage(ex), USER);
    }

    return webhookEventUtils.obtainBranchName(webhookSource, headers, payLoadMap);
  }
  @Override
  @SuppressWarnings("PMD.AvoidCatchingThrowable")
  public void asyncFullSyncForEntireAccount(String accountId) {
    logger.info(GIT_YAML_LOG_PREFIX + "Triggered async full git sync for account {}", accountId);
    executorService.submit(() -> {
      try {
        fullSyncForEntireAccount(accountId);
      } catch (WingsException ex) {
        ExceptionLogger.logProcessedMessages(ex, MANAGER, logger);
      } catch (Throwable e) {
        logger.error("Exception while performing async full git sync for account {}", accountId, e);
      }
    });
  }

  @Override
  @SuppressWarnings("PMD.AvoidCatchingThrowable")
  public void fullSyncForEntireAccount(String accountId) {
    try {
      logger.info(GIT_YAML_LOG_PREFIX + "Performing full sync for account {}", accountId);

      // Perform fullsync for account level entities
      fullSync(accountId, accountId, EntityType.ACCOUNT, false);

      try (HIterator<Application> apps = new HIterator<>(
               wingsPersistence.createQuery(Application.class).filter(ACCOUNT_ID_KEY, accountId).fetch())) {
        for (Application application : apps) {
          fullSync(accountId, application.getUuid(), APPLICATION, false);
        }
      }
      logger.info(GIT_YAML_LOG_PREFIX + "Performed full sync for account {}", accountId);
    } catch (Throwable t) {
      // any thread that faces an error should continue to perform full sync for other accountIds
      // if possible.
      logger.error("Error occured in full sync for account {}", accountId, t);
    }
  }

  private GitCommit fetchLastProcessedGitCommitId(String accountId, List<String> yamlGitConfigIds) {
    // After MultiGit support gitCommit record would have list of yamlGitConfigs.

    GitCommit gitCommit = wingsPersistence.createQuery(GitCommit.class)
                              .filter(ACCOUNT_ID_KEY, accountId)
                              .field(GitCommitKeys.status)
                              .in(getProcessedGitCommitStatusList(accountId))
                              .field(GitCommitKeys.yamlGitConfigIds)
                              .hasAnyOf(yamlGitConfigIds)
                              .order("-lastUpdatedAt")
                              .get();

    // This is to handle the old git commit records which doesn't have yamlGitConfigId
    if (gitCommit == null) {
      gitCommit = wingsPersistence.createQuery(GitCommit.class)
                      .filter(ACCOUNT_ID_KEY, accountId)
                      .filter(GitCommitKeys.yamlGitConfigId, yamlGitConfigIds.get(0))
                      .field(GitCommitKeys.status)
                      .in(getProcessedGitCommitStatusList(accountId))
                      .order("-lastUpdatedAt")
                      .get();
    }

    return gitCommit;
  }

  @Override
  public boolean retainYamlGitConfigsOfSelectedGitConnectorsAndDeleteRest(
      String accountId, List<String> selectedGitConnectors) {
    if (EmptyPredicate.isNotEmpty(selectedGitConnectors)) {
      // Delete yamlGitConfig documents whose gitConnectorId is not among
      // the list of selected git connectors
      wingsPersistence.delete(wingsPersistence.createQuery(YamlGitConfig.class)
                                  .filter(YamlGitConfig.ACCOUNT_ID_KEY, accountId)
                                  .field(YamlGitConfig.GIT_CONNECTOR_ID_KEY)
                                  .hasNoneOf(selectedGitConnectors));
      return true;
    }
    return false;
  }

  @Override
  public RestResponse discardGitSyncErrorsForGivenIds(String accountId, List<String> errorIds) {
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    query.field("_id").in(errorIds);
    wingsPersistence.delete(query);
    closeAlertIfApplicable(accountId, false);
    return RestResponse.Builder.aRestResponse().build();
  }

  @Override
  public List<GitSyncError> getActiveGitToHarnessSyncErrors(
      String accountId, String gitConnectorId, String branchName, long fromTimestamp) {
    //  get all app ids sharing same repo and branch name
    final Set<String> allowedAppIdSet = appIdsSharingRepoBranch(accountId, gitConnectorId, branchName);
    if (isEmpty(allowedAppIdSet)) {
      return Collections.emptyList();
    }
    final Query<GitSyncError> query = wingsPersistence.createQuery(GitSyncError.class)
                                          .filter(ACCOUNT_ID_KEY, accountId)
                                          .filter(GitSyncErrorKeys.fullSyncPath, Boolean.FALSE)
                                          .filter(GitSyncErrorKeys.branchName, branchName)
                                          .filter(GitSyncErrorKeys.gitConnectorId, gitConnectorId)
                                          .field(CreatedAtAware.CREATED_AT_KEY)
                                          .greaterThan(fromTimestamp);

    query.or(query.criteria(GitSyncErrorKeys.status).doesNotExist(),
        query.criteria(GitSyncErrorKeys.status).equal(GitSyncErrorStatus.ACTIVE));

    final List<GitSyncError> gitSyncErrorList = emptyIfNull(query.asList());

    return gitSyncErrorList.stream()
        .filter(gitSyncError -> errorOfAllowedApp(gitSyncError, allowedAppIdSet) || isNewAppError(gitSyncError))
        .collect(toList());
  }

  private boolean isNewAppError(GitSyncError error) {
    return GLOBAL_APP_ID.equals(error.getAppId())
        && error.getYamlFilePath().startsWith(SETUP_FOLDER + PATH_DELIMITER + APPLICATIONS_FOLDER);
  }
  private boolean errorOfAllowedApp(GitSyncError error, Set<String> allowedAppIdSet) {
    return allowedAppIdSet.contains(error.getAppId());
  }

  private Set<String> appIdsSharingRepoBranch(String accountId, String gitConnectorId, String branchName) {
    final List<YamlGitConfig> yamlGitConfigList = wingsPersistence.createQuery(YamlGitConfig.class)
                                                      .project(YamlGitConfigKeys.entityId, true)
                                                      .project(YamlGitConfigKeys.entityType, true)
                                                      .filter(ACCOUNT_ID_KEY, accountId)
                                                      .filter(YamlGitConfigKeys.enabled, Boolean.TRUE)
                                                      .filter(YamlGitConfigKeys.gitConnectorId, gitConnectorId)
                                                      .filter(YamlGitConfigKeys.branchName, branchName)
                                                      .asList();

    return emptyIfNull(yamlGitConfigList)
        .stream()
        .map(
            yamlGitConfig -> yamlGitConfig.getEntityType() == APPLICATION ? yamlGitConfig.getEntityId() : GLOBAL_APP_ID)
        .collect(Collectors.toSet());
  }
}
