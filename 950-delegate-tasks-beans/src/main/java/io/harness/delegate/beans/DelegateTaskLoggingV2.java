package io.harness.delegate.beans;

import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class DelegateTaskLoggingV2 {
  @JsonProperty("abstractions")
  private LinkedHashMap<String, String> logStreamingAbstractions;
  @JsonProperty("token")
  private String loggingToken;
}
