/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cvng.beans.pagerduty;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequestType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@JsonTypeName("PAGERDUTY_DELETE_WEBHOOK")
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
public class PagerdutyDeleteWebhookRequest extends PagerDutyDataCollectionRequest {
  public static final String DSL = PagerdutyDeleteWebhookRequest.readDSL(
      "pagerduty-delete-webhook.datacollection", PagerdutyDeleteWebhookRequest.class);

  private String webhookId;

  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public DataCollectionRequestType getType() {
    return DataCollectionRequestType.PAGERDUTY_DELETE_WEBHOOK;
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> envVariables = new HashMap<>();
    envVariables.put("webhookId", webhookId);
    return envVariables;
  }
}
