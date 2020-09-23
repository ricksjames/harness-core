package io.harness.ng.core.dto.secrets;

import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Builder
public class SecretRequestWrapper {
  @Valid @NotNull private SecretDTOV2 secret;
}
