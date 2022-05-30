package io.harness.cdng.k8s.resources.azure.dtos;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(CDP)
@Schema(
    name = "AzureDeploymentSlotsResponse", description = "Azure response for a list of deployment slots of an webApp")
public class AzureDeploymentSlotsDTO {
  List<AzureDeploymentSlotDTO> deploymentSlots;
}
