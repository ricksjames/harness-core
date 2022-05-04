package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.core.service.entity.ServiceEntity;

import java.util.Set;

public class NGEntitiesMorphiaRegistrar implements MorphiaRegistrar  {
    @Override
    public void registerClasses(Set<Class> set) {
        set.add(ServiceEntity.class);
    }

    @Override
    public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    }
}
