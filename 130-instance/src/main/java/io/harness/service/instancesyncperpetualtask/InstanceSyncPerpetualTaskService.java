package io.harness.service.instancesyncperpetualtask;

import io.harness.dto.DeploymentSummary;
import io.harness.dto.infrastructureMapping.InfrastructureMapping;

public interface InstanceSyncPerpetualTaskService {
  void createPerpetualTasks(InfrastructureMapping infrastructureMapping);

  void createPerpetualTasksForNewDeployment(
      InfrastructureMapping infrastructureMapping, DeploymentSummary deploymentSummary);

  void deletePerpetualTasks(InfrastructureMapping infrastructureMapping);

  void deletePerpetualTasks(String accountId, String infrastructureMappingId);

  void resetPerpetualTask(String accountId, String perpetualTaskId);

  void deletePerpetualTask(String accountId, String infrastructureMappingId, String perpetualTaskId);

  boolean isInstanceSyncByPerpetualTaskEnabled(InfrastructureMapping infrastructureMapping);
}
