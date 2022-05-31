package io.harness.delegate.task.git;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.scm.GitCapabilityHelper;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDP)
@Data
@Builder
public class NGGitOpsTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  private GitFetchFilesConfig gitFetchFilesConfig; // will have ScmConnector
  private Map<String, String> stringMap;
  private String commitMessage;
  private boolean isNewBranch;
  private String sourceBranch;
  private String targetBranch;
  private String accountId;
  private String prTitle;
  private String activityId;
  ConnectorInfoDTO connectorInfoDTO;
  GitOpsTaskType gitOpsTaskType;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilities = new ArrayList<>();

    GitStoreDelegateConfig gitStoreDelegateConfig = gitFetchFilesConfig.getGitStoreDelegateConfig();
    capabilities.addAll(GitCapabilityHelper.fetchRequiredExecutionCapabilities(
        ScmConnectorMapper.toGitConfigDTO(gitFetchFilesConfig.getGitStoreDelegateConfig().getGitConfigDTO()),
        gitStoreDelegateConfig.getEncryptedDataDetails(), gitStoreDelegateConfig.getSshKeySpecDTO()));
    capabilities.addAll(EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
        gitStoreDelegateConfig.getEncryptedDataDetails(), maskingEvaluator));

    return capabilities;
  }
}
