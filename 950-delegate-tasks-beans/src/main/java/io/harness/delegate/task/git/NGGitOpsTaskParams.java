package io.harness.delegate.task.git;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDP)
@Data
@Builder
public class NGGitOpsTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  private GitFetchFilesConfig gitFetchFilesConfig; // will have ScmConnector
  // List<EncryptedDataDetail> encryptedDataDetails; // will be there in gitFetchFilesConfig
  Map<String, String> stringMap;
  String targetBranch;
  private String accountId;
  private String prTitle;
  private String activityId;
  // TODO: set orgId, projId etc.. -- (is it required? is this related to RBAC?)

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return null;
  }
}
