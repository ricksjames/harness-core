/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.shell.ssh;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.shell.CommandTaskParameters;
import io.harness.delegate.task.shell.FileBasedScriptExecutorNG;
import io.harness.delegate.task.shell.SshCommandTaskParameters;
import io.harness.delegate.task.ssh.NgCommandUnit;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class SshCopyCommandHandler implements CommandHandler {
  @Inject private SshScriptExecutorFactory sshScriptExecutorFactory;

  @Override
  public CommandExecutionStatus handle(CommandTaskParameters parameters, NgCommandUnit commandUnit,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    if (!(parameters instanceof SshCommandTaskParameters)) {
      throw new InvalidRequestException("Invalid task parameters submitted for command task.");
    }
    SshCommandTaskParameters sshCommandTaskParameters = (SshCommandTaskParameters) parameters;

    SshExecutorFactoryContext context =
        SshExecutorFactoryContext.builder()
            .accountId(sshCommandTaskParameters.getAccountId())
            .executionId(sshCommandTaskParameters.getExecutionId())
            .workingDirectory(commandUnit.getWorkingDirectory())
            .commandUnitName(commandUnit.getName())
            .commandUnitsProgress(commandUnitsProgress)
            .environment(sshCommandTaskParameters.getEnvironmentVariables())
            .encryptedDataDetailList(sshCommandTaskParameters.getSshInfraDelegateConfig().getEncryptionDataDetails())
            .sshKeySpecDTO(sshCommandTaskParameters.getSshInfraDelegateConfig().getSshKeySpecDto())
            .iLogStreamingTaskClient(logStreamingTaskClient)
            .executeOnDelegate(sshCommandTaskParameters.isExecuteOnDelegate())
            .host(sshCommandTaskParameters.getHost())
            .build();

    FileBasedScriptExecutorNG executor = sshScriptExecutorFactory.getFileBasedExecutor(context);

    return CommandExecutionStatus.SUCCESS;
  }
}
