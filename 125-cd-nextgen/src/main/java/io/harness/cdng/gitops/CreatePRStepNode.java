package io.harness.cdng.gitops;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CdAbstractStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.GITOPS_CREATE_PR)
@TypeAlias("CreatePRStepNode")
@OwnedBy(CDP)
@RecasterAlias("io.harness.cdng.gitops.CreatePRStepNode")
public class CreatePRStepNode extends CdAbstractStepNode {
  @JsonProperty("type") @NotNull StepType type = StepType.CreatePR;
  @NotNull
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  CreatePRStepInfo createPRStepInfo;

  @Override
  public String getType() {
    return StepSpecTypeConstants.GITOPS_CREATE_PR;
  }

  @Override
  public StepSpecType getStepSpecType() {
    return createPRStepInfo;
  }

  enum StepType {
    CreatePR(StepSpecTypeConstants.GITOPS_CREATE_PR);
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
