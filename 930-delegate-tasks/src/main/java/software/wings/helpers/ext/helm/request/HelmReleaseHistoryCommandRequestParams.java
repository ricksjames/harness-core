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
import software.wings.delegatetasks.helm.HelmCommandRequestPrams;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.service.impl.ContainerServiceParams;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class HelmReleaseHistoryCommandRequestParams extends HelmCommandRequestPrams {
  @Builder
  public HelmReleaseHistoryCommandRequestParams(HelmCommandRequestNG.HelmCommandType helmCommandType, String accountId,
      String appId, String kubeConfigLocation, String commandName, String activityId,
      ContainerServiceParams containerServiceParams, String releaseName, String chartUrl, String chartName,
      String chartVersion, String repoName, GitConfig gitConfig, List<EncryptedDataDetail> encryptedDataDetails,
      LogCallback executionLogCallback, String commandFlags, HelmCommandFlag helmCommandFlag,
      K8sDelegateManifestConfig repoConfig, HelmVersion helmVersion, String ocPath, String workingDir,
      List<String> variableOverridesYamlFiles, GitFileConfig gitFileConfig, boolean k8SteadyStateCheckEnabled,
      boolean mergeCapabilities, boolean isGitHostConnectivityCheck, boolean useNewKubectlVersion,
      boolean useLatestChartMuseumVersion) {
    super(helmCommandType, accountId, appId, kubeConfigLocation, commandName, activityId, containerServiceParams,
        releaseName, chartUrl, chartName, chartVersion, repoName, gitConfig, encryptedDataDetails, executionLogCallback,
        commandFlags, helmCommandFlag, repoConfig, helmVersion, ocPath, workingDir, variableOverridesYamlFiles,
        gitFileConfig, k8SteadyStateCheckEnabled, mergeCapabilities, isGitHostConnectivityCheck, useNewKubectlVersion,
        useLatestChartMuseumVersion);
  }

  public HelmReleaseHistoryCommandRequestParams(boolean mergeCapabilities) {
    super(HelmCommandRequestNG.HelmCommandType.RELEASE_HISTORY, mergeCapabilities);
  }
}
