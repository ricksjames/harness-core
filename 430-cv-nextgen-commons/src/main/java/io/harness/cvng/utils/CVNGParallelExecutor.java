/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cvng.utils;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.threading.ParallelExecutor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class CVNGParallelExecutor {
  @Inject @Named("cvngParallelExecutor") protected ExecutorService executorService;

  public <T> List<T> executeParallel(List<Callable<T>> callables) {
    return ParallelExecutor.executeParallel(executorService, callables);
  }
}
