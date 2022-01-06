/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.graphql.schema.mutation.cloudProvider.aws;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.utils.RequestField;

import software.wings.graphql.schema.type.cloudProvider.aws.QLAwsCredentialsType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLUpdateAwsCloudProviderInput {
  private RequestField<String> name;

  private RequestField<QLAwsCredentialsType> credentialsType;
  private RequestField<QLUpdateEc2IamCredentials> ec2IamCredentials;
  private RequestField<QLUpdateAwsManualCredentials> manualCredentials;
  private RequestField<QLUpdateIrsaCredentials> irsaCredentials;

  private RequestField<QLUpdateAwsCrossAccountAttributes> crossAccountAttributes;
  private RequestField<String> defaultRegion;
}
