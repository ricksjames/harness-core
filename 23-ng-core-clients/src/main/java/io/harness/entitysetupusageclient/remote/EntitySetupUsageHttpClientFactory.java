package io.harness.entitysetupusageclient.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;

import com.google.inject.Provider;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
@OwnedBy(DX)
public class EntitySetupUsageHttpClientFactory
    extends AbstractHttpClientFactory implements Provider<EntitySetupUsageClient> {
  public EntitySetupUsageHttpClientFactory(ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    super(ngManagerClientConfig, serviceSecret, tokenGenerator, kryoConverterFactory);
  }

  @Override
  public EntitySetupUsageClient get() {
    return getRetrofit().create(EntitySetupUsageClient.class);
  }
}
