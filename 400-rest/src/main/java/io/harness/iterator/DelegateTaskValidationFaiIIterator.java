package io.harness.iterator;

import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static software.wings.service.impl.DelegateSelectionLogsServiceImpl.TASK_VALIDATION_FAILED;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.joining;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.TaskLogContext;
import io.harness.exception.FailureType;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AutoLogContext;
import io.harness.metrics.intfc.DelegateMetricsService;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateTaskService;

import software.wings.beans.TaskType;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateSelectionLogsService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Singleton
@Slf4j
public class DelegateTaskValidationFaiIIterator implements MongoPersistenceIterator.Handler<DelegateTask> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<DelegateTask> persistenceProvider;
  @Inject private HPersistence persistence;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private DelegateMetricsService delegateMetricsService;
  @Inject private DelegateTaskService delegateTaskService;
  @Inject private DelegateSelectionLogsService delegateSelectionLogsService;
  @Inject private Clock clock;

  private static final long DELEGATE_TASK_VALIDATION_FAIL_TIMEOUT = 30;
  private static final long VALIDATION_TIMEOUT = TimeUnit.MINUTES.toMillis(2);

  public void registerIterators(int threadPoolSize) {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name(this.getClass().getName())
            .poolSize(threadPoolSize)
            .interval(ofSeconds(5))
            .build(),
        DelegateTask.class,
        MongoPersistenceIterator.<DelegateTask, MorphiaFilterExpander<DelegateTask>>builder()
            .clazz(DelegateTask.class)
            .fieldName(DelegateTaskKeys.delegateTaskFailValidationIteration)
            .targetInterval(ofSeconds(DELEGATE_TASK_VALIDATION_FAIL_TIMEOUT))
            .acceptableExecutionTime(ofSeconds(31))
            .acceptableNoAlertDelay(ofSeconds(62))
            .filterExpander(q
                -> q.field(DelegateTaskKeys.status)
                       .equal(QUEUED)
                       .field(DelegateTaskKeys.validationStartedAt)
                       .lessThan(System.currentTimeMillis() - VALIDATION_TIMEOUT))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }
  @Override
  public void handle(DelegateTask delegateTask) {
    if (delegateTask.getValidationCompleteDelegateIds().containsAll(delegateTask.getEligibleToExecuteDelegateIds())) {
      log.info(
          "Found delegate task {} with validation completed by all delegates but not assigned", delegateTask.getUuid());
      try (AutoLogContext ignore = new TaskLogContext(delegateTask.getUuid(), delegateTask.getData().getTaskType(),
               TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR)) {
        // Check whether a whitelisted delegate is connected
        List<String> whitelistedDelegates = assignDelegateService.connectedWhitelistedDelegates(delegateTask);
        if (isNotEmpty(whitelistedDelegates)) {
          log.info("Waiting for task {} to be acquired by a whitelisted delegate: {}", delegateTask.getUuid(),
              whitelistedDelegates);
          return;
        }
        String errorMessage = generateValidationError(delegateTask);
        log.info("Failing task {} due to validation error, {}", delegateTask.getUuid(), errorMessage);

        String capabilitiesFailErrorMessage = TASK_VALIDATION_FAILED + generateCapabilitiesMessage(delegateTask);
        delegateSelectionLogsService.logTaskValidationFailed(delegateTask, capabilitiesFailErrorMessage);

        DelegateResponseData response;
        if (delegateTask.getData().isAsync()) {
          response = ErrorNotifyResponseData.builder()
                         .failureTypes(EnumSet.of(FailureType.DELEGATE_PROVISIONING))
                         .errorMessage(errorMessage)
                         .build();
        } else {
          response =
              RemoteMethodReturnValueData.builder().exception(new InvalidRequestException(errorMessage, USER)).build();
        }
        Query<DelegateTask> taskQuery = persistence.createQuery(DelegateTask.class)
                                            .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                            .filter(DelegateTaskKeys.uuid, delegateTask.getUuid());

        delegateTaskService.handleResponse(delegateTask, taskQuery,
            DelegateTaskResponse.builder()
                .accountId(delegateTask.getAccountId())
                .response(response)
                .responseCode(DelegateTaskResponse.ResponseCode.OK)
                .build());
      }
    }
  }

  private String generateValidationError(DelegateTask delegateTask) {
    final String capabilities = generateCapabilitiesMessage(delegateTask);
    return format(
        "No eligible delegate was able to confirm that it has the capability to perform [ %s ]", capabilities);
  }

  private String generateCapabilitiesMessage(final DelegateTask delegateTask) {
    final List<ExecutionCapability> executionCapabilities = delegateTask.getExecutionCapabilities();
    final StringBuilder stringBuilder = new StringBuilder("");

    if (isNotEmpty(executionCapabilities)) {
      stringBuilder.append(
          (executionCapabilities.size() > 4 ? executionCapabilities.subList(0, 4) : executionCapabilities)
              .stream()
              .map(ExecutionCapability::fetchCapabilityBasis)
              .collect(joining(", ")));
      if (executionCapabilities.size() > 4) {
        stringBuilder.append(", and ").append(executionCapabilities.size() - 4).append(" more...");
      }
    }
    return stringBuilder.toString();
  }
}
