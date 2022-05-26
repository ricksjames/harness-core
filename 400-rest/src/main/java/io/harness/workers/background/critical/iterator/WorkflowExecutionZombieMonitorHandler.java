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
import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.CollectionUtils;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.workers.background.WorkflowExecutionZombieProcessController;

import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.StateType;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class WorkflowExecutionZombieMonitorHandler implements Handler<WorkflowExecution> {
  private static final String PUMP_EXEC_NAME = "WorkflowExecutionZombieMonitor";

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<WorkflowExecution> persistenceProvider;
  @Inject private WingsPersistence wingsPersistence;

  public void registerIterators(int threadPoolSize) {
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
            .entityProcessController(new WorkflowExecutionZombieProcessController())
            .handler(this));
  }

  @Override
  public void handle(WorkflowExecution wfExecution) {
    List<StateExecutionInstance> stateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstanceKeys.workflowId, wfExecution.getWorkflowId())
            .filter(StateExecutionInstanceKeys.executionUuid, wfExecution.getUuid())
            .asList();

    stateExecutionInstances = CollectionUtils.emptyIfNull(stateExecutionInstances);
    sort(stateExecutionInstances);

    // WHEN THE LAST ELEMENT IS OF A ZOMBIE STATE TYPE WE MUST ABORT THE EXECUTION
    StateExecutionInstance seInstance = stateExecutionInstances.get(stateExecutionInstances.size() - 1);
    if (stateTypes().contains(seInstance.getStateType())) {
      // --
      // FORCE ABORT. HOW?
      // --
    }
  }

  /**
   * Sort state execution instances based on createAt field in ascending order. We need the most
   * recently execution instance to evaluate if it's a zombie execution.
   */
  private void sort(List<StateExecutionInstance> stateExecutionInstances) {
    stateExecutionInstances.sort((o1, o2) -> {
      if (o1.getCreatedAt() < o2.getCreatedAt()) {
        return -1;
      } else if (o1.getCreatedAt() > o2.getCreatedAt()) {
        return 1;
      } else {
        return 0;
      }
    });
  }

  private Set<String> stateTypes() {
    return Sets.newHashSet(StateType.REPEAT.name(), StateType.FORK.name(), StateType.PHASE_STEP.name(),
        StateType.PHASE.name(), StateType.SUB_WORKFLOW.name());
  }
}
