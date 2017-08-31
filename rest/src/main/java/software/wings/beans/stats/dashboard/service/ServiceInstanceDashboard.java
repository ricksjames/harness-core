package software.wings.beans.stats.dashboard.service;

import software.wings.beans.stats.dashboard.EntitySummary;

import java.util.List;

/**
 * @author rktummala on 08/14/17
 */
public class ServiceInstanceDashboard {
  private EntitySummary serviceSummary;
  private List<CurrentActiveInstances> currentActiveInstancesList;
  private List<PipelineExecutionHistory> pipelineExecutionHistoryList;
  private List<DeploymentHistory> deploymentHistoryList;

  public List<CurrentActiveInstances> getCurrentActiveInstancesList() {
    return currentActiveInstancesList;
  }

  private void setCurrentActiveInstancesList(List<CurrentActiveInstances> currentActiveInstancesList) {
    this.currentActiveInstancesList = currentActiveInstancesList;
  }

  public List<PipelineExecutionHistory> getPipelineExecutionHistoryList() {
    return pipelineExecutionHistoryList;
  }

  private void setPipelineExecutionHistoryList(List<PipelineExecutionHistory> pipelineExecutionHistoryList) {
    this.pipelineExecutionHistoryList = pipelineExecutionHistoryList;
  }

  public List<DeploymentHistory> getDeploymentHistoryList() {
    return deploymentHistoryList;
  }

  private void setDeploymentHistoryList(List<DeploymentHistory> deploymentHistoryList) {
    this.deploymentHistoryList = deploymentHistoryList;
  }

  public EntitySummary getServiceSummary() {
    return serviceSummary;
  }

  public void setServiceSummary(EntitySummary serviceSummary) {
    this.serviceSummary = serviceSummary;
  }

  public static final class Builder {
    private EntitySummary serviceSummary;
    private List<CurrentActiveInstances> currentActiveInstancesList;
    private List<PipelineExecutionHistory> pipelineExecutionHistoryList;
    private List<DeploymentHistory> deploymentHistoryList;

    private Builder() {}

    public static final Builder aServiceInstanceDashboard() {
      return new Builder();
    }

    public Builder withServiceSummary(EntitySummary serviceSummary) {
      this.serviceSummary = serviceSummary;
      return this;
    }

    public Builder withCurrentActiveInstancesList(List<CurrentActiveInstances> currentActiveInstancesList) {
      this.currentActiveInstancesList = currentActiveInstancesList;
      return this;
    }

    public Builder withPipelineExecutionHistoryList(List<PipelineExecutionHistory> pipelineExecutionHistoryList) {
      this.pipelineExecutionHistoryList = pipelineExecutionHistoryList;
      return this;
    }

    public Builder withDeploymentHistoryList(List<DeploymentHistory> deploymentHistoryList) {
      this.deploymentHistoryList = deploymentHistoryList;
      return this;
    }

    public ServiceInstanceDashboard build() {
      ServiceInstanceDashboard serviceInstanceDashboard = new ServiceInstanceDashboard();
      serviceInstanceDashboard.setServiceSummary(serviceSummary);
      serviceInstanceDashboard.setCurrentActiveInstancesList(currentActiveInstancesList);
      serviceInstanceDashboard.setDeploymentHistoryList(deploymentHistoryList);
      serviceInstanceDashboard.setPipelineExecutionHistoryList(pipelineExecutionHistoryList);
      return serviceInstanceDashboard;
    }
  }
}
