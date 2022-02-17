/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.spring.converters.orchestrationMap;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.OrchestrationMap;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(PIPELINE)
@Singleton
@WritingConverter
public class OrchestrationMapWriteConverter extends OrchestrationMapAbstractWriteConverter<OrchestrationMap> {
  @Inject
  public OrchestrationMapWriteConverter(KryoSerializer kryoSerializer) {
    super(kryoSerializer);
  }
}
