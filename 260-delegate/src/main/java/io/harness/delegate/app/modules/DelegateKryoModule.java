/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.app.modules;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.harness.govern.ProviderModule;
import io.harness.serializer.AccessControlClientRegistrars;
import io.harness.serializer.CgOrchestrationRegistrars;
import io.harness.serializer.ConnectorBeansRegistrars;
import io.harness.serializer.CvNextGenCommonsRegistrars;
import io.harness.serializer.DelegateTaskRegistrars;
import io.harness.serializer.DelegateTasksBeansRegistrars;
import io.harness.serializer.FileServiceCommonsRegistrars;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.LicenseBeanRegistrar;
import io.harness.serializer.NGCoreRegistrars;
import io.harness.serializer.OutboxEventRegistrars;
import io.harness.serializer.RbacCoreRegistrars;
import io.harness.serializer.SMCoreRegistrars;
import io.harness.serializer.kryo.CgOrchestrationBeansKryoRegistrar;
import io.harness.serializer.kryo.CvNextGenCommonsBeansKryoRegistrar;
import io.harness.serializer.kryo.DelegateAgentBeansKryoRegister;
import io.harness.serializer.kryo.DelegateServiceKryoRegister;
import io.harness.serializer.kryo.NgAuthenticationServiceKryoRegistrar;
import io.harness.serializer.kryo.NotificationBeansKryoRegistrar;
import io.harness.serializer.kryo.NotificationDelegateTasksKryoRegistrar;
import io.harness.serializer.kryo.ProjectAndOrgKryoRegistrar;
import io.harness.serializer.kryo.WatcherBeansKryoRegister;
import io.serializer.registrars.NGCommonsRegistrars;

import java.util.Set;

public class DelegateKryoModule extends ProviderModule {
  @Provides
  @Singleton
  Set<Class<? extends KryoRegistrar> > registrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar> >builder()
        .addAll(CvNextGenCommonsRegistrars.kryoRegistrars)
        .addAll(ConnectorBeansRegistrars.kryoRegistrars)
        .addAll(DelegateTasksBeansRegistrars.kryoRegistrars)
        .addAll(CgOrchestrationRegistrars.kryoRegistrars)
        .add(CgOrchestrationBeansKryoRegistrar.class)
        .add(ProjectAndOrgKryoRegistrar.class)
        .addAll(NGCommonsRegistrars.kryoRegistrars)
        .addAll(NGCoreRegistrars.kryoRegistrars)
        .addAll(RbacCoreRegistrars.kryoRegistrars)
        .addAll(SMCoreRegistrars.kryoRegistrars)
        .addAll(FileServiceCommonsRegistrars.kryoRegistrars)
        .add(NotificationBeansKryoRegistrar.class)
        .add(CvNextGenCommonsBeansKryoRegistrar.class)
        .addAll(LicenseBeanRegistrar.kryoRegistrars)
        // temporary:
        .add(NotificationDelegateTasksKryoRegistrar.class)
        .add(DelegateAgentBeansKryoRegister.class)
        .add(WatcherBeansKryoRegister.class)
        .add(DelegateServiceKryoRegister.class)
        .addAll(OutboxEventRegistrars.kryoRegistrars)
        .addAll(AccessControlClientRegistrars.kryoRegistrars)
        .addAll(DelegateTaskRegistrars.kryoRegistrars)
        .add(NgAuthenticationServiceKryoRegistrar.class)
        .build();
  }
}
