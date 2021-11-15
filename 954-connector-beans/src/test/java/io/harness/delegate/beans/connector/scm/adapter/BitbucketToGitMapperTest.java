package io.harness.delegate.beans.connector.scm.adapter;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.scm.GitAuthType.SSH;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.ACCOUNT;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.REPO;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketAuthentication;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketSshCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePassword;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthentication;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthentication;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

@OwnedBy(DX)
public class BitbucketToGitMapperTest extends CategoryTest {
  Set<String> delegateSelectors = new HashSet<>();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    delegateSelectors.add("abc");
    delegateSelectors.add("def");
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testMappingToHTTPGitConfigDTO() {
    final String url = "url";
    final String passwordRef = "passwordRef";
    final String username = "username";
    final String appId = "appId";
    final String insId = "insId";
    final String privateKeyRef = "privateKeyRef";
    final String validationRepo = "validationRepo";

    final BitbucketAuthentication bitbucketAuthentication =
        BitbucketAuthentication.builder()
            .authType(HTTP)
            .credentials(BitbucketHttpCredentials.builder()
                             .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                             .httpCredentialsSpec(BitbucketUsernamePassword.builder()
                                                      .passwordRef(SecretRefHelper.createSecretRef(passwordRef))
                                                      .username(username)
                                                      .build())
                             .build())
            .build();
    final BitbucketConnector bitbucketConnector = BitbucketConnector.builder()
                                                      .url(url)
                                                      .validationRepo(validationRepo)
                                                      .connectionType(GitConnectionType.ACCOUNT)
                                                      .authentication(bitbucketAuthentication)
                                                      .delegateSelectors(delegateSelectors)
                                                      .build();
    GitConfig gitConfig = BitbucketToGitMapper.mapToGitConfigDTO(bitbucketConnector);
    assertThat(gitConfig).isNotNull();
    assertThat(gitConfig.getGitAuthType()).isEqualTo(HTTP);
    assertThat(gitConfig.getDelegateSelectors()).isEqualTo(delegateSelectors);
    GitHTTPAuthentication gitAuthentication = (GitHTTPAuthentication) gitConfig.getGitAuth();
    assertThat(gitConfig.getGitConnectionType()).isEqualTo(ACCOUNT);
    assertThat(gitConfig.getUrl()).isEqualTo(url);
    assertThat(gitConfig.getValidationRepo()).isEqualTo(validationRepo);
    assertThat(gitAuthentication.getUsername()).isEqualTo(username);
    assertThat(gitAuthentication.getPasswordRef().toSecretRefStringValue()).isEqualTo(passwordRef);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testMappingToSSHGitConfigDTO() {
    final String url = "url";
    String sshKeyReference = "sshKeyReference";
    final BitbucketAuthentication bitbucketAuthentication =
        BitbucketAuthentication.builder()
            .authType(GitAuthType.SSH)
            .credentials(
                BitbucketSshCredentials.builder().sshKeyRef(SecretRefHelper.createSecretRef(sshKeyReference)).build())
            .build();

    final BitbucketConnector bitbucketConnector = BitbucketConnector.builder()
                                                      .url(url)
                                                      .connectionType(GitConnectionType.REPO)
                                                      .authentication(bitbucketAuthentication)
                                                      .delegateSelectors(delegateSelectors)
                                                      .build();
    GitConfig gitConfig = BitbucketToGitMapper.mapToGitConfigDTO(bitbucketConnector);
    assertThat(gitConfig).isNotNull();
    assertThat(gitConfig.getGitAuthType()).isEqualTo(SSH);
    GitSSHAuthentication gitAuthentication = (GitSSHAuthentication) gitConfig.getGitAuth();
    assertThat(gitAuthentication.getEncryptedSshKey()).isEqualTo(SecretRefHelper.createSecretRef(sshKeyReference));
    assertThat(gitConfig.getUrl()).isEqualTo(url);
    assertThat(gitConfig.getDelegateSelectors()).isEqualTo(delegateSelectors);
    assertThat(gitConfig.getGitConnectionType()).isEqualTo(REPO);
  }
}