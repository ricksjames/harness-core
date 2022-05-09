package software.wings.helpers.ext.helm.request;


import io.harness.delegate.task.helm.HelmCommandFlag;
import io.harness.delegate.task.helm.HelmCommandRequestNG;
import io.harness.k8s.model.HelmVersion;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.delegatetasks.helm.HelmCommandRequestPrams;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class HelmRollbackCommandRequestParams extends HelmCommandRequestPrams {
  private Integer newReleaseVersion;
  private Integer prevReleaseVersion;

  public HelmRollbackCommandRequestParams(HelmCommandRequestNG.HelmCommandType helmCommandType, String accountId,
      String appId, String kubeConfigLocation, String commandName, String activityId,
      ContainerServiceParams containerServiceParams, String releaseName, String chartUrl, String chartName,
      String chartVersion, String repoName, GitConfig gitConfig, List<EncryptedDataDetail> encryptedDataDetails,
      LogCallback executionLogCallback, String commandFlags, HelmCommandFlag helmCommandFlag,
      K8sDelegateManifestConfig repoConfig, HelmVersion helmVersion, String ocPath, String workingDir,
      List<String> variableOverridesYamlFiles, GitFileConfig gitFileConfig, boolean k8SteadyStateCheckEnabled,
      boolean mergeCapabilities, boolean isGitHostConnectivityCheck, boolean useNewKubectlVersion,
      boolean useLatestChartMuseumVersion, Integer newReleaseVersion, Integer prevReleaseVersion,
      Integer rollbackVersion, long timeoutInMillis) {
    super(helmCommandType, accountId, appId, kubeConfigLocation, commandName, activityId, containerServiceParams,
        releaseName, chartUrl, chartName, chartVersion, repoName, gitConfig, encryptedDataDetails, executionLogCallback,
        commandFlags, helmCommandFlag, repoConfig, helmVersion, ocPath, workingDir, variableOverridesYamlFiles,
        gitFileConfig, k8SteadyStateCheckEnabled, mergeCapabilities, isGitHostConnectivityCheck, useNewKubectlVersion,
        useLatestChartMuseumVersion);
    this.newReleaseVersion = newReleaseVersion;
    this.prevReleaseVersion = prevReleaseVersion;
    this.rollbackVersion = rollbackVersion;
    this.timeoutInMillis = timeoutInMillis;
  }

  private Integer rollbackVersion;
  private long timeoutInMillis;

  public HelmRollbackCommandRequestParams(boolean mergeCapabilities) {
    super(HelmCommandRequestNG.HelmCommandType.ROLLBACK, mergeCapabilities);
  }
}
