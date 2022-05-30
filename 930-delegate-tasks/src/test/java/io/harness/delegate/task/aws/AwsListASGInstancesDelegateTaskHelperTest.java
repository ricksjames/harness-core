/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.VITALIE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AwsCallTracker;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsListASGInstancesTaskParamsRequest;
import io.harness.delegate.beans.connector.awsconnector.AwsListInstancesTaskResponse;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskType;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;

import software.wings.service.impl.AwsApiHelperService;
import software.wings.service.impl.aws.model.AwsInstance;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class AwsListASGInstancesDelegateTaskHelperTest extends CategoryTest {
  @Mock private AwsApiHelperService awsApiHelperService;
  @Mock private AwsNgConfigMapper awsNgConfigMapper;
  @Mock private SecretDecryptionService secretDecryptionService;
  @Mock private AwsCallTracker tracker;
  @Mock private AmazonAutoScalingClient amazonAutoScalingClient;
  @Mock private AwsListInstancesDelegateTaskHelper awsListInstancesDelegateTaskHelper;

  private AwsListASGInstancesDelegateTaskHelper service;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    service = spy(new AwsListASGInstancesDelegateTaskHelper());

    on(service).set("tracker", tracker);
    on(service).set("awsApiHelperService", awsApiHelperService);
    on(service).set("awsNgConfigMapper", awsNgConfigMapper);
    on(service).set("secretDecryptionService", secretDecryptionService);
    on(service).set("awsListInstancesDelegateTaskHelper", awsListInstancesDelegateTaskHelper);

    doReturn(null).when(secretDecryptionService).decrypt(any(), any());
    AwsInternalConfig awsInternalConfig = AwsInternalConfig.builder().build();
    doReturn(awsInternalConfig).when(awsNgConfigMapper).createAwsInternalConfig(any());
    doReturn(amazonAutoScalingClient).when(service).getAmazonAutoScalingClient(any(), any());
    doNothing().when(tracker).trackEC2Call(anyString());
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void getInstanceIds() throws IOException {
    Instance instance = (new Instance()).withInstanceId("id");
    AutoScalingGroup autoScalingGroup = (new AutoScalingGroup()).withInstances(instance);
    DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult =
        (new DescribeAutoScalingGroupsResult()).withAutoScalingGroups(autoScalingGroup);

    doReturn(describeAutoScalingGroupsResult).when(amazonAutoScalingClient).describeAutoScalingGroups(any());

    AwsInternalConfig awsInternalConfig = AwsInternalConfig.builder().build();
    List<String> ids = service.getInstanceIds(awsInternalConfig, "us-east-1", "GroupName");
    assertThat(ids.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void getInstanceIdsFailure() {
    AwsInternalConfig awsInternalConfig = AwsInternalConfig.builder().build();
    doThrow(Exception.class).when(amazonAutoScalingClient).describeAutoScalingGroups(any());
    assertThatThrownBy(() -> service.getInstanceIds(awsInternalConfig, "us-east-1", "GroupName"))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void getListInstances() throws IOException {
    List<String> ids = Arrays.asList("id");
    List<AwsInstance> instances = Arrays.asList(AwsInstance.builder().instanceId("id").build());

    doReturn(ids).when(service).getInstanceIds(any(), any(), any());
    doReturn(instances).when(awsListInstancesDelegateTaskHelper).getListInstances(any(), any(), any());

    AwsListASGInstancesTaskParamsRequest request = generateRequest();
    AwsListInstancesTaskResponse response = (AwsListInstancesTaskResponse) service.getListInstances(request);

    assertThat(response.getInstances().size()).isEqualTo(1);
  }

  private AwsListASGInstancesTaskParamsRequest generateRequest() {
    AwsConnectorDTO awsConnectorDTO =
        AwsConnectorDTO.builder()
            .credential(
                AwsCredentialDTO.builder()
                    .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
                    .config(AwsManualConfigSpecDTO.builder()
                                .accessKey("test-access-key")
                                .secretKeyRef(SecretRefData.builder().decryptedValue("secret".toCharArray()).build())
                                .build())
                    .build())
            .build();

    return AwsListASGInstancesTaskParamsRequest.builder()
        .awsConnector(awsConnectorDTO)
        .encryptionDetails(Collections.emptyList())
        .awsTaskType(AwsTaskType.LIST_ASG_INSTANCES)
        .autoScalingGroupName("group")
        .region("us-east-1")
        .build();
  }
}
