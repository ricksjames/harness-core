package io.harness.engine.governance;

import static io.harness.eraro.ErrorCode.POLICY_EVALUATION_FAILURE;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;
import io.harness.pms.contracts.governance.GovernanceMetadata;

public class PolicyEvaluationFailureException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  GovernanceMetadata governanceMetadata;
  String yaml;

  public PolicyEvaluationFailureException(String message, GovernanceMetadata governanceMetadata, String yaml) {
    super(message, null, POLICY_EVALUATION_FAILURE, Level.ERROR, null, null);
    param(MESSAGE_KEY, message);
    this.governanceMetadata = governanceMetadata;
    this.yaml = yaml;
  }

  public GovernanceMetadata getGovernanceMetadata() {
    return governanceMetadata;
  }

  public String getYaml() {
    return yaml;
  }
}
