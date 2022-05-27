/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.ssh;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.shellscript.ShellScriptSourceWrapper;
import io.harness.steps.shellscript.ShellType;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("ExecuteCommandStepParameters")
@RecasterAlias("io.harness.cdng.ssh.ExecuteCommandStepParameters")
public class ExecuteCommandStepParameters extends ExecuteCommandBaseStepInfo implements SshSpecParameters {
  Map<String, Object> environmentVariables;

  @Builder(builderMethodName = "infoBuilder")
  public ExecuteCommandStepParameters(ShellType shell, ShellScriptSourceWrapper source, List<TailFilePattern> tailFiles,
                                      ParameterField<Boolean> onDelegate, ParameterField<List<TaskSelectorYaml>> delegateSelectors,
                                      ParameterField<String> workingDirectory, Map<String, Object> environmentVariables) {
    super(shell, source, tailFiles, onDelegate, delegateSelectors, workingDirectory);
    this.environmentVariables = environmentVariables;
  }
}
