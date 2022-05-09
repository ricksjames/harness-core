package software.wings.helpers.ext.helm.request;

import static io.harness.expression.Expression.ALLOW_SECRETS;
import static io.harness.k8s.model.HelmVersion.V2;

import io.harness.delegate.task.helm.HelmCommandFlag;
import io.harness.delegate.task.helm.HelmCommandRequestNG;
import io.harness.expression.Expression;
import io.harness.k8s.model.HelmVersion;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.service.impl.ContainerServiceParams;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.Builder;

public class HelmReleaseHistoryCommandRequestParams {
  private HelmCommandRequestNG.HelmCommandType helmCommandType;
  private String accountId;
  private String appId;
  private String kubeConfigLocation;
  private String commandName;
  private String activityId;
  private ContainerServiceParams containerServiceParams;
  private String releaseName;
  private String chartUrl;
  private String chartName;
  private String chartVersion;
  private String repoName;
  private GitConfig gitConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;
  @JsonIgnore private transient LogCallback executionLogCallback;
  @Expression(ALLOW_SECRETS) private String commandFlags;
  @Expression(ALLOW_SECRETS) private HelmCommandFlag helmCommandFlag;
  private K8sDelegateManifestConfig repoConfig;
  @Builder.Default private HelmVersion helmVersion = V2;
  private String ocPath;
  private String workingDir;
  @Expression(ALLOW_SECRETS) private List<String> variableOverridesYamlFiles;
  private GitFileConfig gitFileConfig;
  private boolean k8SteadyStateCheckEnabled;
  private boolean mergeCapabilities; // HELM_MERGE_CAPABILITIES
  private boolean isGitHostConnectivityCheck;
  private boolean useNewKubectlVersion;
  private boolean useLatestChartMuseumVersion;

  public HelmReleaseHistoryCommandRequestParams(boolean mergeCapabilities) {
    helmCommandType = HelmCommandRequestNG.HelmCommandType.RELEASE_HISTORY;
    this.mergeCapabilities = mergeCapabilities;
  }
}
