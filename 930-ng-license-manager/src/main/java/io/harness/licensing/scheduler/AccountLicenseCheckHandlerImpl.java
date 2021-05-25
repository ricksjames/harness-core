package io.harness.licensing.scheduler;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.licensing.LicenseConfiguration;
import io.harness.licensing.ModuleType;
import io.harness.licensing.entities.account.AccountLicense;
import io.harness.licensing.entities.account.AccountLicense.AccountLicenseKeys;
import io.harness.licensing.scheduler.modules.TransactionAggregator;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class AccountLicenseCheckHandlerImpl implements AccountLicenseCheckHandler {
  private final MongoTemplate mongoTemplate;
  private final PersistenceIteratorFactory persistenceIteratorFactory;
  private final LicenseConfiguration licenseConfiguration;
  private final Map<ModuleType, TransactionAggregator> aggregatorMap;
  private final LicenseCheckProcessor licenseCheckProcessor;

  @Override
  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder().name("AccountLicenseCheck").poolSize(2).interval(ofSeconds(30)).build(),
        AccountLicenseCheckHandler.class,
        MongoPersistenceIterator.<AccountLicense, SpringFilterExpander>builder()
            .clazz(AccountLicense.class)
            .fieldName(AccountLicenseKeys.licenseCheckIteration)
            .targetInterval(ofMinutes(licenseConfiguration.getAccountLicenseCheckJobFrequencyInMinutes()))
            .acceptableNoAlertDelay(ofMinutes(60))
            .acceptableExecutionTime(ofSeconds(15))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }

  @Override
  public void handle(AccountLicense entity) {
    long currentTime = Instant.now().toEpochMilli();
    CheckResult defaultResult = CheckResult.newDefaultResult();
    if (entity.getModuleLicenses() != null) {
      CheckResult result = entity.getModuleLicenses()
                               .values()
                               .stream()
                               .map(moduleLicense
                                   -> licenseCheckProcessor.checkExpiry(entity.getUuid(), currentTime, moduleLicense,
                                       aggregatorMap.get(moduleLicense.getModuleType())))
                               .reduce(defaultResult, (v1, v2) -> v1.combine(v2));

      // update allInactive flag on accountLicense
      if (result.isAllInactive() != entity.isAllInactive()) {
        entity.setAllInactive(result.isAllInactive());
        result.setUpdated(true);
      }

      if (result.isUpdated()) {
        AccountLicense latestLicense = mongoTemplate.findById(entity.getUuid(), AccountLicense.class);
        // update iteration because entity queried before the iteration was set
        entity.setLicenseCheckIteration(latestLicense.getLicenseCheckIteration());
        mongoTemplate.save(entity);
      }
    }
    log.info("Account license check for {} complete, used {} milliseconds", entity.getUuid(),
        Instant.now().toEpochMilli() - currentTime);
  }
}
