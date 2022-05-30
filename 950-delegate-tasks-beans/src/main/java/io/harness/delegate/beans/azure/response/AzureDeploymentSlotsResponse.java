package io.harness.delegate.beans.azure.response;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(HarnessTeam.CDP)
public class AzureDeploymentSlotsResponse extends AzureDelegateTaskResponse {
  private List<AzureDeploymentSlotResponse> deploymentSlots;
}
