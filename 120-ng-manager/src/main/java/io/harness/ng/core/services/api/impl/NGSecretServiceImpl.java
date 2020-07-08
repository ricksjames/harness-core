package io.harness.ng.core.services.api.impl;

import static io.harness.ng.core.utils.SecretUtils.getResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ng.core.remote.client.rest.SecretManagerClient;
import io.harness.ng.core.services.api.NGSecretService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class NGSecretServiceImpl implements NGSecretService {
  private final SecretManagerClient secretManagerClient;

  @Override
  public EncryptedData getSecretById(String accountId, String id) {
    return getResponse(secretManagerClient.getSecretById(id, accountId, null));
  }

  @Override
  public String createSecret(String accountId, boolean localMode, SecretText secretText) {
    return getResponse(secretManagerClient.createSecret(accountId, localMode, secretText));
  }

  @Override
  public boolean updateSecret(String accountId, String uuId, SecretText secretText) {
    return getResponse(secretManagerClient.updateSecret(accountId, uuId, secretText));
  }

  @Override
  public boolean deleteSecret(String accountId, String uuId) {
    return getResponse(secretManagerClient.deleteSecret(accountId, uuId));
  }

  @Override
  public List<EncryptedData> getSecretsByType(String accountId, SettingVariableTypes type, boolean includeDetails) {
    return getResponse(secretManagerClient.getSecretsForAccountByType(accountId, type, includeDetails));
  }
}
