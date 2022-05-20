/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.filestore.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.serializer.FiltersRegistrars;
import io.harness.gitsync.serializer.GitSyncSdkRegistrar;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.filestore.serializer.kryo.FileStoreKryoRegistrar;
import io.harness.ng.filestore.serializer.morphia.FileStoreMorphiaRegistrar;
import io.harness.serializer.AccessControlClientRegistrars;
import io.harness.serializer.CommonsRegistrars;
import io.harness.serializer.ConnectorBeansRegistrars;
import io.harness.serializer.FileServiceCommonsRegistrars;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.LicenseBeanRegistrar;
import io.harness.serializer.NGAuditCommonsRegistrars;
import io.harness.serializer.NGCoreBeansRegistrars;
import io.harness.serializer.NGCoreClientRegistrars;
import io.harness.serializer.NGCoreRegistrars;
import io.harness.serializer.OutboxEventRegistrars;
import io.harness.serializer.PersistenceRegistrars;
import io.harness.serializer.PmsCommonsModuleRegistrars;
import io.harness.serializer.YamlBeansModuleRegistrars;

import com.google.common.collect.ImmutableSet;
import io.harness.serializer.morphia.NotificationBeansMorphiaRegistrar;
import io.serializer.registrars.NGCommonsRegistrars;
import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class FileStoreRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(CommonsRegistrars.kryoRegistrars)
          .addAll(PersistenceRegistrars.kryoRegistrars)
          .addAll(FileServiceCommonsRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(CommonsRegistrars.morphiaRegistrars)
          .addAll(PersistenceRegistrars.morphiaRegistrars)
          .addAll(FileServiceCommonsRegistrars.morphiaRegistrars)
          .add(FileStoreMorphiaRegistrar.class)
          .build();
}
