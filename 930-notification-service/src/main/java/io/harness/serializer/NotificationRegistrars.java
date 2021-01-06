package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.DelegateTasksBeansKryoRegister;
import io.harness.serializer.kryo.NotificationSenderKryoRegistrar;
import io.harness.serializer.morphia.NotificationSenderMorphiaRegistrar;

import com.google.common.collect.ImmutableSet;

public class NotificationRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .add(NotificationSenderKryoRegistrar.class)
          .add(DelegateTasksBeansKryoRegister.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().add(NotificationSenderMorphiaRegistrar.class).build();
}
