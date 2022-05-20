/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.workers.background.critical.iterator;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofMinutes;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;

import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class WorkflowZombieMonitorHandler implements Handler<WorkflowExecution> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<WorkflowExecution> persistenceProvider;

  public void registerIterators(int threadPoolSize) {
    PumpExecutorOptions opts = PumpExecutorOptions.builder()
                                   .interval(Duration.ofMinutes(2))
                                   .name("WorkflowZombieMonitor")
                                   .poolSize(threadPoolSize)
                                   .build();
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(opts, WorkflowExecution.class,
        MongoPersistenceIterator.<WorkflowExecution, MorphiaFilterExpander<WorkflowExecution>>builder()
            .mode(PersistenceIterator.ProcessMode.PUMP)
            .clazz(WorkflowExecution.class)
            .fieldName(WorkflowExecutionKeys.nextIteration)
            .targetInterval(ofMinutes(2))
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .filterExpander(q -> q.field(WorkflowExecutionKeys.status).equal(ExecutionStatus.FAILED))
            .handler(this)
            .redistribute(true));
  }

  @Override
  public void handle(WorkflowExecution entity) {
    log.warn(">> {}", entity);
  }
}
