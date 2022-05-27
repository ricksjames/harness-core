/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.workers.background.critical.iterator;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionInterruptType;
import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.CollectionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.EntityProcessController;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;

import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.StateType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class WorkflowExecutionZombieMonitorHandler
    implements Handler<WorkflowExecution>, EntityProcessController<WorkflowExecution> {
  private static final String PUMP_EXEC_NAME = "WorkflowExecutionZombieMonitor";
  private static final long MAX_RUNNING_MINUTES = 10;
  private static final Set<String> ZOMBIE_STATE_TYPES = Sets.newHashSet(StateType.REPEAT.name(), StateType.FORK.name(),
      StateType.PHASE_STEP.name(), StateType.PHASE.name(), StateType.SUB_WORKFLOW.name());

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<WorkflowExecution> persistenceProvider;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;

  public void registerIterators(int threadPoolSize) {
    log.info("Register {} using thread pool size of {}", PUMP_EXEC_NAME, threadPoolSize);

    PumpExecutorOptions opts = PumpExecutorOptions.builder()
                                   .interval(Duration.ofMinutes(10))
                                   .name(PUMP_EXEC_NAME)
                                   .poolSize(threadPoolSize)
                                   .build();

    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(opts, WorkflowExecution.class,
        MongoPersistenceIterator.<WorkflowExecution, MorphiaFilterExpander<WorkflowExecution>>builder()
            .mode(ProcessMode.PUMP)
            .clazz(WorkflowExecution.class)
            .fieldName(WorkflowExecutionKeys.nextZombieIteration)
            .filterExpander(q -> q.field(WorkflowExecutionKeys.status).in(ExecutionStatus.activeStatuses()))
            .targetInterval(Duration.ofMinutes(5))
            .acceptableNoAlertDelay(Duration.ofMinutes(1))
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .entityProcessController(this)
            .handler(this));
  }

  /**
   * The workflow execution should be running and created at least {@code #MAX_RUNNING_MINUTES} ago, then is valid to be
   * evaluated for zombie conditions.
   */
  @Override
  public boolean shouldProcessEntity(WorkflowExecution entity) {
    return entity.getCreatedAt() <= (System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(MAX_RUNNING_MINUTES));
  }

  @Override
  public void handle(WorkflowExecution wfExecution) {
    log.info("Evaluating if workflow execution {} is a zombie execution [workflowId={}]", wfExecution.getUuid(),
        wfExecution.getWorkflowId());

    List<StateExecutionInstance> stateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstanceKeys.workflowId, wfExecution.getWorkflowId())
            .filter(StateExecutionInstanceKeys.executionUuid, wfExecution.getUuid())
            .asList();

    stateExecutionInstances = CollectionUtils.emptyIfNull(stateExecutionInstances);
    sort(stateExecutionInstances);

    // WHEN THE LAST ELEMENT IS OF A ZOMBIE STATE TYPE WE MUST ABORT THE EXECUTION
    Optional<StateExecutionInstance> opt = getLastElement(stateExecutionInstances);
    opt.ifPresent(seInstance -> {
      if (isZombieState(seInstance)) {
        log.info("Trigger force abort of workflow execution {} due remains in a zombie state [currentStateType={}]",
            seInstance.getExecutionUuid(), seInstance.getStateType());

        ExecutionInterrupt executionInterrupt = new ExecutionInterrupt();
        executionInterrupt.setAppId(seInstance.getAppId());
        executionInterrupt.setExecutionUuid(seInstance.getExecutionUuid());
        executionInterrupt.setExecutionInterruptType(ExecutionInterruptType.ABORT_ALL);

        try {
          workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);

        } catch (InvalidRequestException e) {
          log.warn(String.format("Unable to honor force abort [workflowId=%s,executionUuid=%s]",
                       seInstance.getWorkflowId(), seInstance.getExecutionUuid()),
              e);
        }
      }
    });
  }

  /**
   * Sort state execution instances based on createAt field in ascending order. We need the most
   * recently execution instance to evaluate if it's a zombie execution.
   */
  @VisibleForTesting
  void sort(List<StateExecutionInstance> stateExecutionInstances) {
    stateExecutionInstances.sort((o1, o2) -> Long.compare(o1.getCreatedAt(), o2.getCreatedAt()));
  }

  private Optional<StateExecutionInstance> getLastElement(List<StateExecutionInstance> elements) {
    return elements.isEmpty() ? Optional.empty() : Optional.ofNullable(elements.get(elements.size() - 1));
  }

  @VisibleForTesting
  boolean isZombieState(StateExecutionInstance seInstance) {
    return ZOMBIE_STATE_TYPES.contains(seInstance.getStateType());
  }
}
