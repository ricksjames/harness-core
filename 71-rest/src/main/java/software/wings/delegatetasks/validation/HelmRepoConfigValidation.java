package software.wings.delegatetasks.validation;

import static io.harness.network.Http.connectableHttpUrl;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfigValidationTaskParams;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

import java.util.List;
import java.util.function.Consumer;

public class HelmRepoConfigValidation extends AbstractDelegateValidateTask {
  private static final Logger logger = LoggerFactory.getLogger(HelmRepoConfigValidation.class);

  private static final String UNHANDLED_CONFIG_MSG = "Unhandled type of helm repo config. Type : ";

  @Inject private K8sGlobalConfigService k8sGlobalConfigService;

  public HelmRepoConfigValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    logger.info(format("Running validation for task %s", delegateTaskId));

    HelmRepoConfigValidationTaskParams taskParams = (HelmRepoConfigValidationTaskParams) getParameters()[0];
    HelmRepoConfig helmRepoConfig = taskParams.getHelmRepoConfig();

    boolean validated = validateHelmRepoConfig(helmRepoConfig);

    if (!validated) {
      return taskValidationResult(false);
    }

    return taskValidationResult(true);
  }

  @Override
  public List<String> getCriteria() {
    HelmRepoConfigValidationTaskParams taskParams = (HelmRepoConfigValidationTaskParams) getParameters()[0];
    HelmRepoConfig helmRepoConfig = taskParams.getHelmRepoConfig();

    if (helmRepoConfig instanceof HttpHelmRepoConfig) {
      return singletonList("HTTP_HELM_REPO_CONFIG: " + ((HttpHelmRepoConfig) helmRepoConfig).getChartRepoUrl());
    } else if (helmRepoConfig instanceof AmazonS3HelmRepoConfig) {
      AmazonS3HelmRepoConfig amazonS3HelmRepoConfig = (AmazonS3HelmRepoConfig) helmRepoConfig;
      return singletonList("AMAZON_S3_HELM_REPO_CONFIG: " + amazonS3HelmRepoConfig.getBucketName() + ":"
          + amazonS3HelmRepoConfig.getFolderPath() + ":" + amazonS3HelmRepoConfig.getRegion());
    }

    throw new WingsException(UNHANDLED_CONFIG_MSG + helmRepoConfig.getSettingType());
  }

  private boolean validateHelmRepoConfig(HelmRepoConfig helmRepoConfig) {
    if (helmRepoConfig instanceof HttpHelmRepoConfig) {
      return validateHttpHelmRepoConfig(helmRepoConfig);
    } else if (helmRepoConfig instanceof AmazonS3HelmRepoConfig) {
      return validateAmazonS3HelmRepoConfig();
    }

    throw new WingsException(UNHANDLED_CONFIG_MSG + helmRepoConfig.getSettingType());
  }

  private List<DelegateConnectionResult> taskValidationResult(boolean validated) {
    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
  }

  private boolean validateHttpHelmRepoConfig(HelmRepoConfig helmRepoConfig) {
    HttpHelmRepoConfig httpHelmRepoConfig = (HttpHelmRepoConfig) helmRepoConfig;

    String chartRepoUrl = httpHelmRepoConfig.getChartRepoUrl();
    if (!connectableHttpUrl(chartRepoUrl)) {
      logger.info(format("Unreachable URL %s for task %s from delegate", chartRepoUrl, delegateTaskId));
      return false;
    }

    String helmPath = k8sGlobalConfigService.getHelmPath();
    if (isBlank(helmPath)) {
      logger.info(format("Helm not installed in delegate for task %s", delegateTaskId));
      return false;
    }

    return true;
  }

  private boolean validateAmazonS3HelmRepoConfig() {
    String helmPath = k8sGlobalConfigService.getHelmPath();
    if (isBlank(helmPath)) {
      logger.info(format("Helm not installed in delegate for task %s", delegateTaskId));
      return false;
    }

    String chartMuseumPath = k8sGlobalConfigService.getChartMuseumPath();
    if (isBlank(chartMuseumPath)) {
      logger.info(format("chartmuseum not installed in delegate for task %s", delegateTaskId));
      return false;
    }

    return true;
  }
}