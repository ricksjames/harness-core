package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.contracts.execution.Status.NO_OP;
import static io.harness.pms.contracts.execution.Status.SKIPPED;
import static io.harness.pms.contracts.execution.Status.TASK_WAITING;
import static io.harness.pms.sdk.core.execution.invokers.StrategyHelper.buildResponseDataSupplier;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.SkipTaskExecutableResponse;
import io.harness.pms.contracts.execution.TaskExecutableResponse;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventMetadata;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest.RequestCase;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.StringOutcome;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.ProgressableStrategy;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;

import com.google.inject.Inject;
import java.util.Collections;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(PIPELINE)
public class TaskStrategy extends ProgressableStrategy {
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private StrategyHelper strategyHelper;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    TaskExecutable taskExecutable = extractStep(nodeExecution);
    Ambiance ambiance = nodeExecution.getAmbiance();
    TaskRequest task = taskExecutable.obtainTask(ambiance,
        sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(ambiance, nodeExecution, task);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecutionProto nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    String accountId = AmbianceUtils.getAccountId(ambiance);
    TaskExecutable taskExecutable = extractStep(nodeExecution);
    StepResponse stepResponse = null;
    try {
      stepResponse = taskExecutable.handleTaskResult(ambiance,
          sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution),
          buildResponseDataSupplier(resumePackage.getResponseDataMap()));
    } catch (Exception e) {
      stepResponse = strategyHelper.handleException(e);
    }
    sdkNodeExecutionService.handleStepResponse(nodeExecution.getUuid(),
        StepResponseMapper.toStepResponseProto(stepResponse),
        SdkResponseEventMetadata.newBuilder().setAccountId(accountId).build());
  }

  private void handleResponse(@NonNull Ambiance ambiance, NodeExecutionProto nodeExecution, TaskRequest taskRequest) {
    String accountId = AmbianceUtils.getAccountId(ambiance);
    if (RequestCase.SKIPTASKREQUEST == taskRequest.getRequestCase()) {
      sdkNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), NO_OP,
          ExecutableResponse.newBuilder()
              .setSkipTask(SkipTaskExecutableResponse.newBuilder()
                               .setMessage(taskRequest.getSkipTaskRequest().getMessage())
                               .build())
              .build(),
          Collections.emptyList(), SdkResponseEventMetadata.newBuilder().setAccountId(accountId).build());
      sdkNodeExecutionService.handleStepResponse(nodeExecution.getUuid(),
          StepResponseMapper.toStepResponseProto(
              StepResponse.builder()
                  .status(SKIPPED)
                  .stepOutcome(
                      StepOutcome.builder()
                          .name("skipOutcome")
                          .outcome(
                              StringOutcome.builder().message(taskRequest.getSkipTaskRequest().getMessage()).build())
                          .build())
                  .build()),
          SdkResponseEventMetadata.newBuilder().setAccountId(accountId).build());
      return;
    }

    ExecutableResponse executableResponse =
        ExecutableResponse.newBuilder()
            .setTask(
                TaskExecutableResponse.newBuilder()
                    .setTaskCategory(taskRequest.getTaskCategory())
                    .addAllLogKeys(CollectionUtils.emptyIfNull(taskRequest.getDelegateTaskRequest().getLogKeysList()))
                    .addAllUnits(CollectionUtils.emptyIfNull(taskRequest.getDelegateTaskRequest().getUnitsList()))
                    .setTaskName(taskRequest.getDelegateTaskRequest().getTaskName())
                    .build())
            .build();

    QueueTaskRequest queueTaskRequest = QueueTaskRequest.newBuilder()
                                            .setNodeExecutionId(nodeExecution.getUuid())
                                            .putAllSetupAbstractions(ambiance.getSetupAbstractionsMap())
                                            .setTaskRequest(taskRequest)
                                            .setExecutableResponse(executableResponse)
                                            .setStatus(TASK_WAITING)
                                            .build();
    sdkNodeExecutionService.queueTaskRequest(
        queueTaskRequest, SdkResponseEventMetadata.newBuilder().setAccountId(accountId).build());
  }

  @Override
  public TaskExecutable extractStep(NodeExecutionProto nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (TaskExecutable) stepRegistry.obtain(node.getStepType());
  }
}
