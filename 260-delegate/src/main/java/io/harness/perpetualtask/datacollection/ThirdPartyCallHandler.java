/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.datacollection;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO.ApiCallLogDTOField;
import io.harness.cvng.beans.cvnglog.TraceableType;
import io.harness.datacollection.entity.CallDetails;

import software.wings.delegatetasks.DelegateLogService;

import com.google.common.base.Preconditions;
import java.time.Instant;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CV)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class ThirdPartyCallHandler implements Consumer<CallDetails> {
  private String accountId;
  private String requestUuid;
  private DelegateLogService delegateLogService;
  private Instant startTime;
  private Instant endTime;

  public ThirdPartyCallHandler(
      String accountId, String requestUuid, DelegateLogService delegateLogService, Instant startTime, Instant endTime) {
    this.accountId = accountId;
    this.requestUuid = requestUuid;
    this.delegateLogService = delegateLogService;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  @Override
  public void accept(CallDetails callDetails) {
    Preconditions.checkNotNull(callDetails.getRequest(), "Call Details request is null.");
    final ApiCallLogDTO cvngLogDTO = ApiCallLogDTO.builder()
                                         .traceableId(requestUuid)
                                         .traceableType(TraceableType.VERIFICATION_TASK)
                                         .accountId(accountId)
                                         .startTime(startTime.toEpochMilli())
                                         .endTime(endTime.toEpochMilli())
                                         .requestTime(callDetails.getRequestTime().toEpochMilli())
                                         .responseTime(callDetails.getResponseTime().toEpochMilli())
                                         .build();
    cvngLogDTO.addFieldToRequest(ApiCallLogDTOField.builder()
                                     .name("url")
                                     .type(ApiCallLogDTO.FieldType.URL)
                                     .value(callDetails.getRequest().request().url().toString())
                                     .build());

    if (callDetails.getResponse() != null && callDetails.getResponse().body() != null) {
      cvngLogDTO.addFieldToResponse(
          callDetails.getResponse().code(), callDetails.getResponse().body(), ApiCallLogDTO.FieldType.JSON);
    }

    delegateLogService.save(accountId, cvngLogDTO);
  }
}
