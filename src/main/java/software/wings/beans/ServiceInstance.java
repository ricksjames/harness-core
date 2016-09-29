package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Property;
import software.wings.sm.ExecutionStatus;

import java.util.Objects;

/**
 * The Class ServiceInstance.
 */
@Entity(value = "serviceInstance", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("envId"), @Field("host"), @Field("serviceTemplate") },
    options = @IndexOptions(unique = true)))
public class ServiceInstance extends Base {
  @Indexed private String envId;

  //@Reference(idOnly = true, ignoreMissing = true) private Host host;
  //@Reference(idOnly = true, ignoreMissing = true) private ServiceTemplate serviceTemplate;

  @Property("serviceTemplate") private String serviceTemplateId;

  @Indexed private String serviceName;

  @Property("host") private String hostId;

  @Indexed private String hostName;

  @Indexed private String tagName;

  private String releaseId;
  @Indexed private String releaseName;
  private String artifactId;
  @Indexed private String artifactName;
  @Indexed private long artifactDeployedOn;
  @Indexed private ExecutionStatus artifactDeploymentStatus;
  private String artifactDeploymentActivityId;

  private String lastActivityId;
  private ExecutionStatus lastActivityStatus;
  private long lastActivityCreatedAt;
  @Indexed private String commandName;
  @Indexed private String commandType;
  private long lastDeployedOn;

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Gets release id.
   *
   * @return the release id
   */
  public String getReleaseId() {
    return releaseId;
  }

  /**
   * Sets release id.
   *
   * @param releaseId the release id
   */
  public void setReleaseId(String releaseId) {
    this.releaseId = releaseId;
  }

  /**
   * Gets release name.
   *
   * @return the release name
   */
  public String getReleaseName() {
    return releaseName;
  }

  /**
   * Sets release name.
   *
   * @param releaseName the release name
   */
  public void setReleaseName(String releaseName) {
    this.releaseName = releaseName;
  }

  /**
   * Gets artifact id.
   *
   * @return the artifact id
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * Sets artifact id.
   *
   * @param artifactId the artifact id
   */
  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  /**
   * Gets artifact name.
   *
   * @return the artifact name
   */
  public String getArtifactName() {
    return artifactName;
  }

  /**
   * Sets artifact name.
   *
   * @param artifactName the artifact name
   */
  public void setArtifactName(String artifactName) {
    this.artifactName = artifactName;
  }

  /**
   * Gets artifact deployed on.
   *
   * @return the artifact deployed on
   */
  public long getArtifactDeployedOn() {
    return artifactDeployedOn;
  }

  /**
   * Sets artifact deployed on.
   *
   * @param artifactDeployedOn the artifact deployed on
   */
  public void setArtifactDeployedOn(long artifactDeployedOn) {
    this.artifactDeployedOn = artifactDeployedOn;
  }

  /**
   * Gets artifact deployment status.
   *
   * @return the artifact deployment status
   */
  public ExecutionStatus getArtifactDeploymentStatus() {
    return artifactDeploymentStatus;
  }

  /**
   * Sets artifact deployment status.
   *
   * @param artifactDeploymentStatus the artifact deployment status
   */
  public void setArtifactDeploymentStatus(ExecutionStatus artifactDeploymentStatus) {
    this.artifactDeploymentStatus = artifactDeploymentStatus;
  }

  /**
   * Gets artifact deployment activity id.
   *
   * @return the artifact deployment activity id
   */
  public String getArtifactDeploymentActivityId() {
    return artifactDeploymentActivityId;
  }

  /**
   * Sets artifact deployment activity id.
   *
   * @param artifactDeploymentActivityId the artifact deployment activity id
   */
  public void setArtifactDeploymentActivityId(String artifactDeploymentActivityId) {
    this.artifactDeploymentActivityId = artifactDeploymentActivityId;
  }

  /**
   * Gets last activity id.
   *
   * @return the last activity id
   */
  public String getLastActivityId() {
    return lastActivityId;
  }

  /**
   * Sets last activity id.
   *
   * @param lastActivityId the last activity id
   */
  public void setLastActivityId(String lastActivityId) {
    this.lastActivityId = lastActivityId;
  }

  /**
   * Gets last activity status.
   *
   * @return the last activity status
   */
  public ExecutionStatus getLastActivityStatus() {
    return lastActivityStatus;
  }

  /**
   * Sets last activity status.
   *
   * @param lastActivityStatus the last activity status
   */
  public void setLastActivityStatus(ExecutionStatus lastActivityStatus) {
    this.lastActivityStatus = lastActivityStatus;
  }

  /**
   * Gets command name.
   *
   * @return the command name
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * Sets command name.
   *
   * @param commandName the command name
   */
  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  /**
   * Gets command type.
   *
   * @return the command type
   */
  public String getCommandType() {
    return commandType;
  }

  /**
   * Sets command type.
   *
   * @param commandType the command type
   */
  public void setCommandType(String commandType) {
    this.commandType = commandType;
  }

  /**
   * Gets last deployed on.
   *
   * @return the last deployed on
   */
  public long getLastDeployedOn() {
    return lastDeployedOn;
  }

  /**
   * Sets last deployed on.
   *
   * @param lastDeployedOn the last deployed on
   */
  public void setLastDeployedOn(long lastDeployedOn) {
    this.lastDeployedOn = lastDeployedOn;
  }

  /**
   * Gets last activity created on.
   *
   * @return the last activity created on
   */
  public long getLastActivityCreatedAt() {
    return lastActivityCreatedAt;
  }

  /**
   * Sets last activity created on.
   *
   * @param lastActivityCreatedAt the last activity created on
   */
  public void setLastActivityCreatedAt(long lastActivityCreatedAt) {
    this.lastActivityCreatedAt = lastActivityCreatedAt;
  }

  public String getServiceTemplateId() {
    return serviceTemplateId;
  }

  public void setServiceTemplateId(String serviceTemplateId) {
    this.serviceTemplateId = serviceTemplateId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getHostId() {
    return hostId;
  }

  public void setHostId(String hostId) {
    this.hostId = hostId;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getTagName() {
    return tagName;
  }

  public void setTagName(String tagName) {
    this.tagName = tagName;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(envId, serviceTemplateId, serviceName, hostId, hostName, tagName, releaseId, releaseName,
              artifactId, artifactName, artifactDeployedOn, artifactDeploymentStatus, artifactDeploymentActivityId,
              lastActivityId, lastActivityStatus, lastActivityCreatedAt, commandName, commandType, lastDeployedOn);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final ServiceInstance other = (ServiceInstance) obj;
    return Objects.equals(this.envId, other.envId) && Objects.equals(this.serviceTemplateId, other.serviceTemplateId)
        && Objects.equals(this.serviceName, other.serviceName) && Objects.equals(this.hostId, other.hostId)
        && Objects.equals(this.hostName, other.hostName) && Objects.equals(this.tagName, other.tagName)
        && Objects.equals(this.releaseId, other.releaseId) && Objects.equals(this.releaseName, other.releaseName)
        && Objects.equals(this.artifactId, other.artifactId) && Objects.equals(this.artifactName, other.artifactName)
        && Objects.equals(this.artifactDeployedOn, other.artifactDeployedOn)
        && Objects.equals(this.artifactDeploymentStatus, other.artifactDeploymentStatus)
        && Objects.equals(this.artifactDeploymentActivityId, other.artifactDeploymentActivityId)
        && Objects.equals(this.lastActivityId, other.lastActivityId)
        && Objects.equals(this.lastActivityStatus, other.lastActivityStatus)
        && Objects.equals(this.lastActivityCreatedAt, other.lastActivityCreatedAt)
        && Objects.equals(this.commandName, other.commandName) && Objects.equals(this.commandType, other.commandType)
        && Objects.equals(this.lastDeployedOn, other.lastDeployedOn);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("envId", envId)
        .add("serviceTemplateId", serviceTemplateId)
        .add("serviceName", serviceName)
        .add("hostId", hostId)
        .add("hostName", hostName)
        .add("tagName", tagName)
        .add("releaseId", releaseId)
        .add("releaseName", releaseName)
        .add("artifactId", artifactId)
        .add("artifactName", artifactName)
        .add("artifactDeployedOn", artifactDeployedOn)
        .add("artifactDeploymentStatus", artifactDeploymentStatus)
        .add("artifactDeploymentActivityId", artifactDeploymentActivityId)
        .add("lastActivityId", lastActivityId)
        .add("lastActivityStatus", lastActivityStatus)
        .add("lastActivityCreatedAt", lastActivityCreatedAt)
        .add("commandName", commandName)
        .add("commandType", commandType)
        .add("lastDeployedOn", lastDeployedOn)
        .toString();
  }

  public static final class Builder {
    private String envId;
    private String serviceTemplateId;
    private String serviceName;
    private String hostId;
    private String hostName;
    private String tagName;
    private String releaseId;
    private String releaseName;
    private String artifactId;
    private String artifactName;
    private long artifactDeployedOn;
    private ExecutionStatus artifactDeploymentStatus;
    private String uuid;
    private String artifactDeploymentActivityId;
    private String appId;
    private String lastActivityId;
    private ExecutionStatus lastActivityStatus;
    private User createdBy;
    private long lastActivityCreatedAt;
    private long createdAt;
    private String commandName;
    private String commandType;
    private User lastUpdatedBy;
    private long lastDeployedOn;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder aServiceInstance() {
      return new Builder();
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public Builder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder withHost(Host host) {
      this.hostId = host.getUuid();
      this.hostName = host.getHostName();
      this.tagName = host.getConfigTag() != null ? host.getConfigTag().getName() : null;
      return this;
    }

    public Builder withServiceTemplate(ServiceTemplate serviceTemplate) {
      this.serviceName = serviceTemplate.getService() != null ? serviceTemplate.getService().getName() : "";
      this.serviceTemplateId = serviceTemplate.getUuid();
      return this;
    }

    public Builder withHostId(String hostId) {
      this.hostId = hostId;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withTagName(String tagName) {
      this.tagName = tagName;
      return this;
    }

    public Builder withReleaseId(String releaseId) {
      this.releaseId = releaseId;
      return this;
    }

    public Builder withReleaseName(String releaseName) {
      this.releaseName = releaseName;
      return this;
    }

    public Builder withArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    public Builder withArtifactName(String artifactName) {
      this.artifactName = artifactName;
      return this;
    }

    public Builder withArtifactDeployedOn(long artifactDeployedOn) {
      this.artifactDeployedOn = artifactDeployedOn;
      return this;
    }

    public Builder withArtifactDeploymentStatus(ExecutionStatus artifactDeploymentStatus) {
      this.artifactDeploymentStatus = artifactDeploymentStatus;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withArtifactDeploymentActivityId(String artifactDeploymentActivityId) {
      this.artifactDeploymentActivityId = artifactDeploymentActivityId;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withLastActivityId(String lastActivityId) {
      this.lastActivityId = lastActivityId;
      return this;
    }

    public Builder withLastActivityStatus(ExecutionStatus lastActivityStatus) {
      this.lastActivityStatus = lastActivityStatus;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withLastActivityCreatedAt(long lastActivityCreatedAt) {
      this.lastActivityCreatedAt = lastActivityCreatedAt;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public Builder withCommandType(String commandType) {
      this.commandType = commandType;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastDeployedOn(long lastDeployedOn) {
      this.lastDeployedOn = lastDeployedOn;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public Builder but() {
      return aServiceInstance()
          .withEnvId(envId)
          .withServiceTemplateId(serviceTemplateId)
          .withServiceName(serviceName)
          .withHostId(hostId)
          .withHostName(hostName)
          .withTagName(tagName)
          .withReleaseId(releaseId)
          .withReleaseName(releaseName)
          .withArtifactId(artifactId)
          .withArtifactName(artifactName)
          .withArtifactDeployedOn(artifactDeployedOn)
          .withArtifactDeploymentStatus(artifactDeploymentStatus)
          .withUuid(uuid)
          .withArtifactDeploymentActivityId(artifactDeploymentActivityId)
          .withAppId(appId)
          .withLastActivityId(lastActivityId)
          .withLastActivityStatus(lastActivityStatus)
          .withCreatedBy(createdBy)
          .withLastActivityCreatedAt(lastActivityCreatedAt)
          .withCreatedAt(createdAt)
          .withCommandName(commandName)
          .withCommandType(commandType)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastDeployedOn(lastDeployedOn)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public ServiceInstance build() {
      ServiceInstance serviceInstance = new ServiceInstance();
      serviceInstance.setEnvId(envId);
      serviceInstance.setServiceTemplateId(serviceTemplateId);
      serviceInstance.setServiceName(serviceName);
      serviceInstance.setHostId(hostId);
      serviceInstance.setHostName(hostName);
      serviceInstance.setTagName(tagName);
      serviceInstance.setReleaseId(releaseId);
      serviceInstance.setReleaseName(releaseName);
      serviceInstance.setArtifactId(artifactId);
      serviceInstance.setArtifactName(artifactName);
      serviceInstance.setArtifactDeployedOn(artifactDeployedOn);
      serviceInstance.setArtifactDeploymentStatus(artifactDeploymentStatus);
      serviceInstance.setUuid(uuid);
      serviceInstance.setArtifactDeploymentActivityId(artifactDeploymentActivityId);
      serviceInstance.setAppId(appId);
      serviceInstance.setLastActivityId(lastActivityId);
      serviceInstance.setLastActivityStatus(lastActivityStatus);
      serviceInstance.setCreatedBy(createdBy);
      serviceInstance.setLastActivityCreatedAt(lastActivityCreatedAt);
      serviceInstance.setCreatedAt(createdAt);
      serviceInstance.setCommandName(commandName);
      serviceInstance.setCommandType(commandType);
      serviceInstance.setLastUpdatedBy(lastUpdatedBy);
      serviceInstance.setLastDeployedOn(lastDeployedOn);
      serviceInstance.setLastUpdatedAt(lastUpdatedAt);
      serviceInstance.setActive(active);
      return serviceInstance;
    }
  }
}
