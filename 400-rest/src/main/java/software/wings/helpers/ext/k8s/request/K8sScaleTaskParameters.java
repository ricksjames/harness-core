package software.wings.helpers.ext.k8s.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.k8s.model.HelmVersion;

import software.wings.beans.InstanceUnitType;

import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class K8sScaleTaskParameters extends K8sTaskParameters {
  private String workload;
  private Integer instances;
  private InstanceUnitType instanceUnitType;
  private Optional<Integer> maxInstances;
  private boolean skipSteadyStateCheck;
  @Builder
  public K8sScaleTaskParameters(String accountId, String appId, String commandName, String activityId,
      K8sTaskType k8sTaskType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, String workload, Integer instances, InstanceUnitType instanceUnitType,
      Integer maxInstances, boolean skipSteadyStateCheck, HelmVersion helmVersion, Set<String> delegateSelectors,
      boolean useLatestChartMuseumVersion, boolean useVarSupportForKustomize, boolean useNewKubectlVersion) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sTaskType, helmVersion, delegateSelectors, useLatestChartMuseumVersion,
            useVarSupportForKustomize, useNewKubectlVersion);
    this.workload = workload;
    this.instances = instances;
    this.instanceUnitType = instanceUnitType;
    this.maxInstances = Optional.ofNullable(maxInstances);
    this.skipSteadyStateCheck = skipSteadyStateCheck;
  }
}
