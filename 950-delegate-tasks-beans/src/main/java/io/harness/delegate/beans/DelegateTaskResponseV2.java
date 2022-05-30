/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.TaskTypeV2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = DelegateTaskResponseV2Deserializer.class)
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public class DelegateTaskResponseV2 {
  private String id;
  @JsonProperty("data")
  private DelegateResponseData delegateResponseData;
  @JsonProperty("type")
  private TaskTypeV2 taskType;
  @JsonProperty("code")
  private DelegateTaskResponse.ResponseCode responseCode;
}
