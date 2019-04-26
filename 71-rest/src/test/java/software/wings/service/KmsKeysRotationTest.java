package software.wings.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.queue.Queue;
import io.harness.security.encryption.EncryptionType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.api.KmsTransitionEvent;
import software.wings.beans.Account;
import software.wings.beans.KmsConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.KmsTransitionEventListener;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by rsingh on 7/20/18.
 */
@Ignore
@Integration
@Slf4j
public class KmsKeysRotationTest extends WingsBaseTest {
  private static final String AWS_ACCESS_KEY = "aws_access_key";
  private static final String AWS_SECRET_KEY = "aws_secret_key";
  private static final String AWS_SECRET_ARN = "aws_secret_arn";
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Inject private KmsService kmsService;
  @Inject private SecretManagementDelegateService secretManagementDelegateService;
  @Inject private SecretManager secretManager;
  @Inject private Queue<KmsTransitionEvent> transitionKmsQueue;
  @Inject private WingsPersistence wingsPersistence;
  private KmsTransitionEventListener transitionEventListener;

  @Before
  public void setup() throws IOException, NoSuchFieldException, IllegalAccessException {
    initMocks(this);
    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(secretManagementDelegateService);
    FieldUtils.writeField(kmsService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(secretManager, "kmsService", kmsService, true);
    FieldUtils.writeField(wingsPersistence, "secretManager", secretManager, true);
  }

  @Test
  @Category(UnitTests.class)
  public void rotateAwsKeysSameARN() throws Exception {
    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(GLOBAL_ACCOUNT_ID, false);
    assertEquals(1, kmsConfigs.size());
    KmsConfig kmsConfig = kmsConfigs.iterator().next();
    assertNotNull(kmsConfig);
    String newAccessKey = System.getenv(AWS_ACCESS_KEY);
    if (isEmpty(newAccessKey)) {
      newAccessKey = System.getProperty(AWS_ACCESS_KEY);
    }
    assertFalse("access key must be provided", isEmpty(newAccessKey));

    String newSecretKey = System.getenv(AWS_SECRET_KEY);
    if (isEmpty(newSecretKey)) {
      newSecretKey = System.getProperty(AWS_SECRET_KEY);
    }
    assertFalse("secret key must be provided", isEmpty(newSecretKey));

    kmsConfig.setAccessKey(newAccessKey);
    kmsConfig.setSecretKey(newSecretKey);
    try {
      kmsService.saveKmsConfig(GLOBAL_ACCOUNT_ID, kmsConfig);
    } catch (WingsException e) {
      logger.error("Key rotation failed, {}", e.getParams());
      fail();
    }
    logger.info("Key rotation done");
  }

  @Test
  @Category(UnitTests.class)
  public void rotateAwsKeysAndArn() throws Exception {
    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(GLOBAL_ACCOUNT_ID, false);
    assertEquals(1, kmsConfigs.size());
    KmsConfig oldKmsConfig = kmsConfigs.iterator().next();
    String kmsName = oldKmsConfig.getName();
    assertNotNull(oldKmsConfig);
    String newAccessKey = System.getenv(AWS_ACCESS_KEY);
    if (isEmpty(newAccessKey)) {
      newAccessKey = System.getProperty(AWS_ACCESS_KEY);
    }
    assertFalse("access key must be provided", isEmpty(newAccessKey));

    String newSecretKey = System.getenv(AWS_SECRET_KEY);
    if (isEmpty(newSecretKey)) {
      newSecretKey = System.getProperty(AWS_SECRET_KEY);
    }
    assertFalse("secret key must be provided", isEmpty(newSecretKey));

    String newSecretArn = System.getenv(AWS_SECRET_ARN);
    if (isEmpty(newSecretArn)) {
      newSecretArn = System.getProperty(AWS_SECRET_ARN);
    }
    assertFalse("arn must be provided", isEmpty(newSecretArn));

    String newKmsConfigId = kmsService.saveKmsConfig(GLOBAL_ACCOUNT_ID,
        KmsConfig.builder()
            .name("rotatedKMS-" + generateUuid())
            .accessKey(newAccessKey)
            .secretKey(newSecretKey)
            .kmsArn(newSecretArn)
            .build());
    Thread listenerThread = startTransitionListener();
    Query<Account> query = wingsPersistence.createQuery(Account.class);

    long secretsToMigrate =
        wingsPersistence.createQuery(EncryptedData.class).filter("kmsId", oldKmsConfig.getUuid()).count();
    logger.info("total of {} will be migrated", secretsToMigrate);
    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        Account account = records.next();
        logger.info("rotating kms for {} id {}", account.getAccountName(), account.getUuid());

        secretManager.transitionSecrets(
            account.getUuid(), EncryptionType.KMS, oldKmsConfig.getUuid(), EncryptionType.KMS, newKmsConfigId);

        long remainingSecrets;
        do {
          remainingSecrets = wingsPersistence.createQuery(EncryptedData.class)
                                 .filter("accountId", account.getUuid())
                                 .filter("kmsId", oldKmsConfig.getUuid())
                                 .count();
          if (remainingSecrets > 0) {
            logger.info("for {} with id {} still {} non migrated secrets are present. Will wait.",
                account.getAccountName(), account.getUuid(), remainingSecrets);
            Thread.sleep(5000);
          }
        } while (remainingSecrets > 0);
        logger.info("rotation of kms for {} id {} is done", account.getAccountName(), account.getUuid());
      }
      logger.info("All secrets migrated to new kms");
      List<Key<EncryptedData>> oldSecrets =
          wingsPersistence.createQuery(EncryptedData.class).filter("kmsId", oldKmsConfig.getUuid()).asKeyList();
      assertEquals("remaining non migrated secrets " + oldSecrets, 0, oldSecrets.size());
      logger.info("total migrated secrets are {}",
          wingsPersistence.createQuery(EncryptedData.class).filter("kmsId", newKmsConfigId).asKeyList());
      kmsService.deleteKmsConfig(GLOBAL_ACCOUNT_ID, oldKmsConfig.getUuid());
      wingsPersistence.updateField(KmsConfig.class, newKmsConfigId, "name", kmsName);
      wingsPersistence.updateField(KmsConfig.class, newKmsConfigId, "isDefault", false);
    } finally {
      stopTransitionListener(listenerThread);
    }
  }

  private Thread startTransitionListener() throws IllegalAccessException {
    transitionEventListener = new KmsTransitionEventListener();
    FieldUtils.writeField(transitionEventListener, "timer", new ScheduledThreadPoolExecutor(1), true);
    FieldUtils.writeField(transitionEventListener, "queueController", new ConfigurationController(1), true);
    FieldUtils.writeField(transitionEventListener, "queue", transitionKmsQueue, true);
    FieldUtils.writeField(transitionEventListener, "secretManager", secretManager, true);

    Thread eventListenerThread = new Thread(() -> transitionEventListener.run());
    eventListenerThread.start();
    return eventListenerThread;
  }

  private void stopTransitionListener(Thread thread) throws InterruptedException {
    transitionEventListener.shutDown();
    thread.join();
  }
}
