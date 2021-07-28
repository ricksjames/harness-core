package io.harness.governance.pipeline.service.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.HarnessTagLink;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDC)
public class Tag {
  @Nonnull private String key;
  @Nullable private String value;

  public static Tag fromTagLink(HarnessTagLink tagLink) {
    return new Tag(tagLink.getKey(), tagLink.getValue());
  }
}
