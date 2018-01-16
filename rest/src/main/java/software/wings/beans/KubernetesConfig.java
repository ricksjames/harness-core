package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.settings.SettingValue;

/**
 * Created by bzane on 2/28/17
 */
@JsonTypeName("KUBERNETES")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@ToString(exclude = {"password", "clientKeyPassphrase"})
public class KubernetesConfig extends SettingValue implements Encryptable {
  @NotEmpty private String masterUrl;
  private String username;
  @Encrypted private char[] password;
  @Encrypted private char[] caCert;
  @Encrypted private char[] clientCert;
  @Encrypted private char[] clientKey;
  @Encrypted private char[] clientKeyPassphrase;
  private String clientKeyAlgo;
  private String namespace;
  @NotEmpty @SchemaIgnore private String accountId;

  @SchemaIgnore private String encryptedPassword;
  @SchemaIgnore private String encryptedCaCert;
  @SchemaIgnore private String encryptedClientCert;
  @SchemaIgnore private String encryptedClientKey;
  @SchemaIgnore private String encryptedClientKeyPassphrase;

  /**
   * Instantiates a new setting value.
   */
  public KubernetesConfig() {
    super(SettingVariableTypes.KUBERNETES.name());
  }

  public KubernetesConfig(String masterUrl, String username, char[] password, char[] caCert, char[] clientCert,
      char[] clientKey, char[] clientKeyPassphrase, String clientKeyAlgo, String namespace, String accountId,
      String encryptedPassword, String encryptedCaCert, String encryptedClientCert, String encryptedClientKey,
      String encryptedClientKeyPassphrase) {
    this();
    this.masterUrl = masterUrl;
    this.username = username;
    this.password = password;
    this.caCert = caCert;
    this.clientCert = clientCert;
    this.clientKey = clientKey;
    this.clientKeyPassphrase = clientKeyPassphrase;
    this.clientKeyAlgo = clientKeyAlgo;
    this.namespace = namespace;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
    this.encryptedCaCert = encryptedCaCert;
    this.encryptedClientCert = encryptedClientCert;
    this.encryptedClientKey = encryptedClientKey;
    this.encryptedClientKeyPassphrase = encryptedClientKeyPassphrase;
  }
}
