/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.jenkins;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.context.MdcGlobalContextData;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionMetadataKeys;
import io.harness.exception.runtime.JenkinsServerRuntimeException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.manage.GlobalContextManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class JenkinsArtifactTaskHelper {
  private final JenkinsArtifactTaskHandler jenkinsArtifactTaskHandler;

  public ArtifactTaskResponse getArtifactCollectResponse(
      ArtifactTaskParameters artifactTaskParameters, LogCallback executionLogCallback) {
    JenkinsArtifactDelegateRequest attributes = (JenkinsArtifactDelegateRequest) artifactTaskParameters.getAttributes();
    String registryUrl = attributes.getJenkinsConnectorDTO().getJenkinsUrl();
    jenkinsArtifactTaskHandler.decryptRequestDTOs(attributes);
    ArtifactTaskResponse artifactTaskResponse;
    try {
      switch (artifactTaskParameters.getArtifactTaskType()) {
        case VALIDATE_ARTIFACT_SERVER:
          saveLogs(executionLogCallback, "Validating  Artifact Server");
          artifactTaskResponse = getSuccessTaskResponse(jenkinsArtifactTaskHandler.validateArtifactServer(attributes));
          saveLogs(executionLogCallback, "validated artifact server: " + registryUrl);
          break;
        case GET_JOBS:
          saveLogs(executionLogCallback, "Get the Jenkins Job");
          artifactTaskResponse = getSuccessTaskResponse(jenkinsArtifactTaskHandler.getJob(attributes));
          saveLogs(executionLogCallback, "Get the Jenkins Job " + registryUrl);
          break;
        case GET_ARTIFACT_PATH:
          saveLogs(executionLogCallback, "Get the Jenkins Artifact Patb");
          artifactTaskResponse = getSuccessTaskResponse(jenkinsArtifactTaskHandler.getArtifactPaths(attributes));
          saveLogs(executionLogCallback, "Get the Jenkins Job " + registryUrl);
          break;
        case GET_BUILDS:
          saveLogs(executionLogCallback, "Get the Jenkins Builds for Job");
          artifactTaskResponse = getSuccessTaskResponse(jenkinsArtifactTaskHandler.getBuilds(attributes));
          saveLogs(executionLogCallback, "Get the Jenkins Builds for Job " + registryUrl);
          break;
        default:
          saveLogs(executionLogCallback,
              "No corresponding Jenkins artifact task type [{}]: " + artifactTaskParameters.toString());
          log.error("No corresponding Jenkins artifact task type [{}]", artifactTaskParameters.toString());
          return ArtifactTaskResponse.builder()
              .commandExecutionStatus(CommandExecutionStatus.FAILURE)
              .errorMessage("There is no Jenkins artifact task type impl defined for - "
                  + artifactTaskParameters.getArtifactTaskType().name())
              .errorCode(ErrorCode.INVALID_ARGUMENT)
              .build();
      }
    } catch (JenkinsServerRuntimeException ex) {
      if (GlobalContextManager.get(MdcGlobalContextData.MDC_ID) == null) {
        MdcGlobalContextData mdcGlobalContextData = MdcGlobalContextData.builder().map(new HashMap<>()).build();
        GlobalContextManager.upsertGlobalContextRecord(mdcGlobalContextData);
      }
      ((MdcGlobalContextData) GlobalContextManager.get(MdcGlobalContextData.MDC_ID))
          .getMap()
          .put(ExceptionMetadataKeys.CONNECTOR.name(), attributes.getConnectorRef());
      throw ex;
    }
    return artifactTaskResponse;
  }

  private ArtifactTaskResponse getSuccessTaskResponse(ArtifactTaskExecutionResponse taskExecutionResponse) {
    return ArtifactTaskResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .artifactTaskExecutionResponse(taskExecutionResponse)
        .build();
  }

  private void saveLogs(LogCallback executionLogCallback, String message) {
    if (executionLogCallback != null) {
      executionLogCallback.saveExecutionLog(message);
    }
  }
  public ArtifactTaskResponse getArtifactCollectResponse(ArtifactTaskParameters artifactTaskParameters) {
    return getArtifactCollectResponse(artifactTaskParameters, null);
  }
}
