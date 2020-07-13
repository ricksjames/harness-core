package io.harness.cvng.core.entities;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.harness.cvng.util.ErrorMessageUtils.generateErrorMessageFromParam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.cvng.models.VerificationType;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import javax.validation.constraints.NotNull;

@Data
@FieldNameConstants(innerTypeName = "CVConfigKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "cvConfigs")
@HarnessEntity(exportable = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class CVConfig
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess, PersistentRegularIterable {
  @Id private String uuid;
  @FdIndex private Long dataCollectionTaskIteration;
  @NotNull private String name;
  private long createdAt;
  private long lastUpdatedAt;
  @NotNull private VerificationType verificationType;

  @NotNull @FdIndex private String accountId;
  @NotNull @FdIndex private String connectorId;

  @NotNull private String serviceIdentifier;
  @NotNull private String envIdentifier;
  @NotNull private String projectIdentifier;
  @NotNull private CVMonitoringCategory category;
  private String dataCollectionTaskId;
  private String productName;
  private String groupId;

  @FdIndex private Long analysisOrchestrationIteration;

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    if (CVConfigKeys.dataCollectionTaskIteration.equals(fieldName)) {
      this.dataCollectionTaskIteration = nextIteration;
      return;
    }
    if (fieldName.equals(CVConfigKeys.analysisOrchestrationIteration)) {
      this.analysisOrchestrationIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (CVConfigKeys.dataCollectionTaskIteration.equals(fieldName)) {
      return this.dataCollectionTaskIteration;
    }
    if (fieldName.equals(CVConfigKeys.analysisOrchestrationIteration)) {
      return analysisOrchestrationIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public void validate() {
    checkNotNull(getVerificationType(), generateErrorMessageFromParam(CVConfigKeys.verificationType));
    checkNotNull(accountId, generateErrorMessageFromParam(CVConfigKeys.accountId));
    checkNotNull(connectorId, generateErrorMessageFromParam(CVConfigKeys.connectorId));
    checkNotNull(serviceIdentifier, generateErrorMessageFromParam(CVConfigKeys.serviceIdentifier));
    checkNotNull(envIdentifier, generateErrorMessageFromParam(CVConfigKeys.envIdentifier));
    checkNotNull(projectIdentifier, generateErrorMessageFromParam(CVConfigKeys.projectIdentifier));
    checkNotNull(groupId, generateErrorMessageFromParam(CVConfigKeys.groupId));

    validateParams();
  }

  protected abstract void validateParams();

  public abstract DataSourceType getType();

  @JsonIgnore public abstract String getDataCollectionDsl();
}
