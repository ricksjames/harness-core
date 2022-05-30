package io.harness.iterator;

import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.beans.DelegateTask.Status.runningStatuses;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.metrics.impl.DelegateMetricsServiceImpl.DELEGATE_TASK_REBROADCAST;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.persistence.HQuery.excludeAuthority;

import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofSeconds;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.delegate.task.TaskLogContext;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.metrics.intfc.DelegateMetricsService;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateTaskService;

import software.wings.beans.TaskType;
import software.wings.service.impl.DelegateTaskBroadcastHelper;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateSelectionLogsService;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Clock;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

@Singleton
@Slf4j
public class DelegateTaskRebroadcast implements MongoPersistenceIterator.Handler<DelegateTask> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<DelegateTask> persistenceProvider;
  @Inject private HPersistence persistence;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private DelegateMetricsService delegateMetricsService;
  @Inject private DelegateTaskService delegateTaskService;
  @Inject private DelegateSelectionLogsService delegateSelectionLogsService;
  @Inject private Clock clock;
  @Inject private DelegateTaskBroadcastHelper broadcastHelper;

  private static final long DELEGATE_REBROADCAST_INTERVAL = 5;
  private static long BROADCAST_INTERVAL = TimeUnit.MINUTES.toMillis(1);
  private static int MAX_BROADCAST_ROUND = 3;
  private static int BROADCAST_LIMIT = 10;

  public void registerIterators(int threadPoolSize) {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("delegateTaskRebroadcast")
            .poolSize(threadPoolSize)
            .interval(ofSeconds(5))
            .build(),
        DelegateTask.class,
        MongoPersistenceIterator.<DelegateTask, MorphiaFilterExpander<DelegateTask>>builder()
            .clazz(DelegateTask.class)
            .fieldName(DelegateTaskKeys.delegateTaskRebroadcastIteration)
            .targetInterval(ofSeconds(DELEGATE_REBROADCAST_INTERVAL))
            .acceptableExecutionTime(ofSeconds(32))
            .acceptableNoAlertDelay(ofSeconds(45))
            .filterExpander(q
                -> q.field(DelegateTaskKeys.status)
                       .equal(QUEUED)
                       .field(DelegateTaskKeys.nextBroadcast)
                       .lessThan(System.currentTimeMillis())
                       .field(DelegateTaskKeys.expiry)
                       .greaterThan(System.currentTimeMillis())
                       .field(DelegateTaskKeys.broadcastRound)
                       .lessThan(MAX_BROADCAST_ROUND))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(DelegateTask delegateTask) {
    long now = clock.millis();
    Query<DelegateTask> query = persistence.createQuery(DelegateTask.class, excludeAuthority)
                                    .filter(DelegateTaskKeys.uuid, delegateTask.getUuid());

    LinkedList<String> eligibleDelegatesList = delegateTask.getEligibleToExecuteDelegateIds();

    // add connected eligible delegates to broadcast list. Also rotate the eligibleDelegatesList list
    List<String> broadcastToDelegates = Lists.newArrayList();
    int broadcastLimit = Math.min(eligibleDelegatesList.size(), BROADCAST_LIMIT);

    Iterator<String> eligibleDelegateIdsIterator = eligibleDelegatesList.iterator();

    while (eligibleDelegateIdsIterator.hasNext() && broadcastLimit > broadcastToDelegates.size()) {
      String delegateId = eligibleDelegatesList.removeFirst();
      broadcastToDelegates.add(delegateId);
      eligibleDelegatesList.addLast(delegateId);
    }
    long nextInterval = TimeUnit.SECONDS.toMillis(DELEGATE_REBROADCAST_INTERVAL);
    int broadcastRoundCount = delegateTask.getBroadcastRound();
    Set<String> alreadyTriedDelegates =
        Optional.ofNullable(delegateTask.getAlreadyTriedDelegates()).orElse(Sets.newHashSet());

    // if all delegates got one round of rebroadcast, then increase broadcast interval & broadcastRound
    if (alreadyTriedDelegates.containsAll(delegateTask.getEligibleToExecuteDelegateIds())) {
      alreadyTriedDelegates.clear();
      broadcastRoundCount++;
      nextInterval = (long) broadcastRoundCount * BROADCAST_INTERVAL;
    }
    alreadyTriedDelegates.addAll(broadcastToDelegates);

    UpdateOperations<DelegateTask> updateOperations =
        persistence.createUpdateOperations(DelegateTask.class)
            .set(DelegateTaskKeys.lastBroadcastAt, now)
            .set(DelegateTaskKeys.broadcastCount, delegateTask.getBroadcastCount() + 1)
            .set(DelegateTaskKeys.eligibleToExecuteDelegateIds, eligibleDelegatesList)
            .set(DelegateTaskKeys.nextBroadcast, now + nextInterval)
            .set(DelegateTaskKeys.alreadyTriedDelegates, alreadyTriedDelegates)
            .set(DelegateTaskKeys.broadcastRound, broadcastRoundCount);
    delegateTask = persistence.findAndModify(query, updateOperations, HPersistence.returnNewOptions);

    if (delegateTask == null) {
      log.info("Cannot find delegate task, update failed on broadcast");
      return;
    }
    delegateTask.setBroadcastToDelegateIds(broadcastToDelegates);
    delegateSelectionLogsService.logBroadcastToDelegate(Sets.newHashSet(broadcastToDelegates), delegateTask);

    try (AutoLogContext ignore1 = new TaskLogContext(delegateTask.getUuid(), delegateTask.getData().getTaskType(),
             TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(delegateTask.getAccountId(), OVERRIDE_ERROR)) {
      log.info("IT: Rebroadcast queued task id {} on broadcast attempt: {} on round {} to {} ", delegateTask.getUuid(),
          delegateTask.getBroadcastCount(), delegateTask.getBroadcastRound()+1, delegateTask.getBroadcastToDelegateIds());
      delegateMetricsService.recordDelegateTaskMetrics(delegateTask, DELEGATE_TASK_REBROADCAST);
      broadcastHelper.rebroadcastDelegateTask(delegateTask);
    }
  }
}
