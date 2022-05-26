/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.workers.background;

import io.harness.mongo.EntityProcessController;

import software.wings.beans.WorkflowExecution;

import java.util.concurrent.TimeUnit;

public class WorkflowExecutionZombieProcessController implements EntityProcessController<WorkflowExecution> {
  private static final long MAX_RUNNING_MINUTES = 10;

  @Override
  public boolean shouldProcessEntity(WorkflowExecution entity) {
    return entity.getCreatedAt() <= (System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(MAX_RUNNING_MINUTES));
  }
}
