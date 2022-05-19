package io.harness.delegate.beans;

import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretReference;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SlackEncryptedWebHookURLDTO implements DecryptableEntity {
    @SecretReference private String slackEncryptedWebHookURL;
}
