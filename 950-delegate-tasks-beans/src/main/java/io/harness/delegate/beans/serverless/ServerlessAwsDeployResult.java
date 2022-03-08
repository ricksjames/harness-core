/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans.serverless;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serverless.model.ServerlessAwsLambdaFunction;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class ServerlessAwsDeployResult implements ServerlessDeployResult {
  private String service;
  private String region;
  private String stage;
  private String previousVersionTimeStamp;
  private String errorMessage;
  private List<ServerlessAwsLambdaFunction> functions;
}
