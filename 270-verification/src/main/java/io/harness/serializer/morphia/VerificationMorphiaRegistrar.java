/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer.morphia;

import io.harness.entities.AnomalousLogRecord;
import io.harness.entities.CVTask;
import io.harness.entities.TimeSeriesAnomaliesRecord;
import io.harness.entities.TimeSeriesCumulativeSums;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class VerificationMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(TimeSeriesAnomaliesRecord.class);
    set.add(TimeSeriesCumulativeSums.class);
    set.add(AnomalousLogRecord.class);
    set.add(CVTask.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // no classes to register
  }
}
