package io.harness.cdng.service.steps;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@TypeAlias("serviceSectionStepParameters")
@RecasterAlias("io.harness.cdng.service.steps.ServiceSectionStepParameters")
public class ServiceSectionStepParameters implements StepParameters {
  ParameterField<String> serviceRef;
  String childNodeId;

  @Override
  public String toViewJson() {
    return RecastOrchestrationUtils.toJson(ServiceSectionStepParameters.builder().serviceRef(serviceRef).build());
  }
}
