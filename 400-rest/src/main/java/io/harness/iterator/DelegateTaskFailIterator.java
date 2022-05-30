package io.harness.iterator;

import static io.harness.beans.DelegateTask.Status.runningStatuses;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.TaskFailureReason.EXPIRED;
import static io.harness.metrics.impl.DelegateMetricsServiceImpl.DELEGATE_TASK_EXPIRED;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.joining;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.metrics.intfc.DelegateMetricsService;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateTaskService;

import software.wings.service.intfc.AssignDelegateService;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DelegateTaskFailIterator implements MongoPersistenceIterator.Handler<DelegateTask> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<DelegateTask> persistenceProvider;
  @Inject private HPersistence persistence;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private DelegateMetricsService delegateMetricsService;
  @Inject private DelegateTaskService delegateTaskService;

  private static final long DELEGATE_TASK_FAIL_TIMEOUT = 30;
  private static final String unknownErrorMessage =
      "Unable to determine proper error as delegate task could not be deserialized.";

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
            .fieldName(DelegateTask.DelegateTaskKeys.delegateTaskFailIteration)
            .targetInterval(ofSeconds(DELEGATE_TASK_FAIL_TIMEOUT))
            .acceptableExecutionTime(ofSeconds(31))
            .acceptableNoAlertDelay(ofSeconds(62))
            .filterExpander(q
                -> q.field(DelegateTask.DelegateTaskKeys.status)
                       .in(runningStatuses())
                       .field(DelegateTask.DelegateTaskKeys.expiry)
                       .lessThan(currentTimeMillis()))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }
  @Override
  public void handle(DelegateTask delegateTask) throws IllegalArgumentException {
    boolean deleted = persistence.delete(delegateTask);
    if (deleted) {
      delegateMetricsService.recordDelegateTaskMetrics(delegateTask, DELEGATE_TASK_EXPIRED);
      String errorMessage = assignDelegateService.getActiveDelegateAssignmentErrorMessage(EXPIRED, delegateTask);
      if (isEmpty(errorMessage)) {
        errorMessage = unknownErrorMessage;
      }
      delegateTaskService.handleResponse(delegateTask, null,
          DelegateTaskResponse.builder()
              .accountId(delegateTask.getAccountId())
              .responseCode(DelegateTaskResponse.ResponseCode.FAILED)
              .response(ErrorNotifyResponseData.builder().expired(true).errorMessage(errorMessage).build())
              .build());
    }
  }
}
