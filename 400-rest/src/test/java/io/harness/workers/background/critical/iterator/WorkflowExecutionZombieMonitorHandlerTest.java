/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.workers.background.critical.iterator;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.FERNANDOD;

import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionInterruptType;
import io.harness.category.element.UnitTests;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.rule.Owner;

import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mongodb.morphia.query.Query;

@RunWith(MockitoJUnitRunner.class)
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class WorkflowExecutionZombieMonitorHandlerTest {
  private static final String WORKFLOW_ID = "workflowId";
  private static final String EXECUTION_UUID = "executionUuid";

  @InjectMocks private WorkflowExecutionZombieMonitorHandler monitorHandler;

  @Mock private MorphiaPersistenceProvider<WorkflowExecution> persistenceProvider;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private WorkflowExecutionService workflowExecutionService;

  @Test
  @Category(UnitTests.class)
  @Owner(developers = FERNANDOD)
  public void shouldNotProcessEntityWhenCreatedNotIsMoreThanMaxMinutes() {
    WorkflowExecution entity = WorkflowExecution.builder().createdAt(System.currentTimeMillis()).build();
    assertThat(monitorHandler.shouldProcessEntity(entity)).isFalse();
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = FERNANDOD)
  public void shouldProcessEntityWhenCreatedMoreThanMaxMinutes() {
    WorkflowExecution entity =
        WorkflowExecution.builder().createdAt(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10)).build();
    assertThat(monitorHandler.shouldProcessEntity(entity)).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = FERNANDOD)
  public void shouldHandleWorkflowExecutionWhenNotFoundStateExecutionInstances() {
    WorkflowExecution wfExecution = WorkflowExecution.builder().workflowId(WORKFLOW_ID).uuid(EXECUTION_UUID).build();

    Query<StateExecutionInstance> query = mock(Query.class);
    when(wingsPersistence.createQuery(StateExecutionInstance.class)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
    when(query.asList()).thenReturn(Collections.emptyList());

    monitorHandler.handle(wfExecution);

    verify(workflowExecutionService, never()).triggerExecutionInterrupt(any());
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = FERNANDOD)
  public void shouldHandleWorkflowExecutionWhenNotNotZombieState() {
    WorkflowExecution wfExecution = WorkflowExecution.builder().workflowId(WORKFLOW_ID).uuid(EXECUTION_UUID).build();
    StateExecutionInstance seInstance = aStateExecutionInstance().stateType(StateType.SHELL_SCRIPT.name()).build();

    Query<StateExecutionInstance> query = mock(Query.class);
    when(wingsPersistence.createQuery(StateExecutionInstance.class)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
    when(query.asList()).thenReturn(Collections.singletonList(seInstance));

    monitorHandler.handle(wfExecution);

    verify(workflowExecutionService, never()).triggerExecutionInterrupt(any());
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = FERNANDOD)
  public void shouldHandleWorkflowExecutionAndTriggerInterruptWhenZombieState() {
    WorkflowExecution wfExecution = WorkflowExecution.builder().workflowId(WORKFLOW_ID).uuid(EXECUTION_UUID).build();
    StateExecutionInstance seInstance = aStateExecutionInstance()
                                            .appId("APP_ID")
                                            .executionUuid(EXECUTION_UUID)
                                            .stateType(StateType.PHASE.name())
                                            .build();

    Query<StateExecutionInstance> query = mock(Query.class);
    when(wingsPersistence.createQuery(StateExecutionInstance.class)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
    when(query.asList()).thenReturn(Collections.singletonList(seInstance));

    monitorHandler.handle(wfExecution);

    ArgumentCaptor<ExecutionInterrupt> captor = ArgumentCaptor.forClass(ExecutionInterrupt.class);
    verify(workflowExecutionService).triggerExecutionInterrupt(captor.capture());

    ExecutionInterrupt value = captor.getValue();
    assertThat(value.getAppId()).isEqualTo("APP_ID");
    assertThat(value.getExecutionUuid()).isEqualTo(EXECUTION_UUID);
    assertThat(value.getExecutionInterruptType()).isEqualTo(ExecutionInterruptType.ABORT_ALL);
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = FERNANDOD)
  public void shouldVerifyNotZombieStateType() {
    Set<StateType> types = new HashSet(Arrays.asList(StateType.values()));

    // REMOVE VALID TYPES
    types.remove(StateType.REPEAT);
    types.remove(StateType.FORK);
    types.remove(StateType.PHASE_STEP);
    types.remove(StateType.PHASE);
    types.remove(StateType.SUB_WORKFLOW);

    types.forEach(item -> {
      assertThat(monitorHandler.isZombieState(aStateExecutionInstance().stateType(item.name()).build())).isFalse();
    });
  }

  @Test
  @Category(UnitTests.class)
  @Owner(developers = FERNANDOD)
  public void shouldSortStateExecutionInstances() {
    List<StateExecutionInstance> instances = new ArrayList<>();
    instances.add(aStateExecutionInstance().createdAt(4).build());
    instances.add(aStateExecutionInstance().createdAt(2).build());
    instances.add(aStateExecutionInstance().createdAt(1).build());
    instances.add(aStateExecutionInstance().createdAt(3).build());

    monitorHandler.sort(instances);

    int value = 1;
    for (StateExecutionInstance item : instances) {
      assertThat(item.getCreatedAt()).isEqualTo(value++);
    }
  }
}
