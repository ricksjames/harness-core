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
import io.harness.delegate.beans.connector.awsconnector.AwsListInstancesTaskParamsRequest;
import io.harness.delegate.beans.connector.awsconnector.AwsListInstancesTaskResponse;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskType;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;

import software.wings.service.impl.AwsApiHelperService;
import software.wings.service.impl.AwsUtils;
import software.wings.service.impl.aws.model.AwsInstance;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
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
public class AwsListInstancesDelegateTaskHelperTest extends CategoryTest {
  @Mock private AwsApiHelperService awsApiHelperService;
  @Mock private AwsNgConfigMapper awsNgConfigMapper;
  @Mock private SecretDecryptionService secretDecryptionService;
  @Mock private AwsCallTracker tracker;
  @Mock private AwsUtils awsUtils;
  @Mock private AmazonEC2Client ec2Client;

  private AwsListInstancesDelegateTaskHelper service;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    service = spy(new AwsListInstancesDelegateTaskHelper());

    on(service).set("tracker", tracker);
    on(service).set("awsApiHelperService", awsApiHelperService);
    on(service).set("awsNgConfigMapper", awsNgConfigMapper);
    on(service).set("secretDecryptionService", secretDecryptionService);
    on(service).set("awsUtils", awsUtils);

    doReturn(null).when(secretDecryptionService).decrypt(any(), any());
    AwsInternalConfig awsInternalConfig = AwsInternalConfig.builder().build();
    doReturn(awsInternalConfig).when(awsNgConfigMapper).createAwsInternalConfig(any());
    doReturn(ec2Client).when(service).getAmazonEc2Client(any(), any());
    doNothing().when(tracker).trackEC2Call(anyString());
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void getListInstances() throws IOException {
    List<Filter> filters = Arrays.asList(new Filter("key", Arrays.asList("value")));
    doReturn(filters).when(awsUtils).getFilters(any(), any());

    Instance instance = (new Instance()).withInstanceId("id").withPublicDnsName("some dns");
    Reservation reservation = (new Reservation()).withInstances(instance);
    DescribeInstancesResult describeInstancesResult = (new DescribeInstancesResult()).withReservations(reservation);

    doReturn(describeInstancesResult).when(ec2Client).describeInstances(any());

    AwsListInstancesTaskParamsRequest request = generateRequest();
    AwsListInstancesTaskResponse delegateResponseData =
        (AwsListInstancesTaskResponse) service.getListInstances(request);

    assertThat(delegateResponseData.getInstances().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void getListInstancesFailure() {
    List<Filter> filters = Arrays.asList(new Filter("key", Arrays.asList("value")));
    doReturn(filters).when(awsUtils).getFilters(any(), any());

    doThrow(Exception.class).when(ec2Client).describeInstances(any());
    AwsListInstancesTaskParamsRequest request = generateRequest();

    assertThatThrownBy(() -> service.getListInstances(request)).isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void getListInstancesWithIds() throws IOException {
    Instance instance = (new Instance()).withInstanceId("id").withPublicDnsName("some dns");
    Reservation reservation = (new Reservation()).withInstances(instance);
    DescribeInstancesResult describeInstancesResult = (new DescribeInstancesResult()).withReservations(reservation);

    doReturn(describeInstancesResult).when(ec2Client).describeInstances(any());

    AwsInternalConfig awsInternalConfig = AwsInternalConfig.builder().build();
    List<String> ids = Arrays.asList("id1");

    List<AwsInstance> result = service.getListInstances(awsInternalConfig, "us-east-1", ids);

    assertThat(result.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void getListInstancesWithIdsFailure() throws IOException {
    AwsInternalConfig awsInternalConfig = AwsInternalConfig.builder().build();
    List<String> ids = Arrays.asList("id1");

    doThrow(Exception.class).when(ec2Client).describeInstances(any());
    assertThatThrownBy(() -> service.getListInstances(awsInternalConfig, "us-east-1", ids))
        .isInstanceOf(InvalidRequestException.class);
  }

  private AwsListInstancesTaskParamsRequest generateRequest() {
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

    return AwsListInstancesTaskParamsRequest.builder()
        .awsConnector(awsConnectorDTO)
        .encryptionDetails(Collections.emptyList())
        .awsTaskType(AwsTaskType.LIST_INSTANCES)
        .tags(Collections.singletonMap("key", "value"))
        .region("us-east-1")
        .build();
  }
}
