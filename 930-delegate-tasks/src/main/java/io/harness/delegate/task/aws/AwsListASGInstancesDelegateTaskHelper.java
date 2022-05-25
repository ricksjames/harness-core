/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.aws;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AwsCallTracker;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsListASGInstancesTaskParamsRequest;
import io.harness.delegate.beans.connector.awsconnector.AwsListInstancesTaskResponse;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskParams;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.SecretDecryptionService;

import software.wings.service.impl.AwsApiHelperService;
import software.wings.service.impl.aws.client.CloseableAmazonWebServiceClient;
import software.wings.service.impl.aws.model.AwsInstance;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CDP)
public class AwsListASGInstancesDelegateTaskHelper {
  @Inject private AwsApiHelperService awsApiHelperService;
  @Inject private AwsNgConfigMapper awsNgConfigMapper;
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private AwsCallTracker tracker;
  @Inject private AwsListInstancesDelegateTaskHelper awsListInstancesDelegateTaskHelper;

  public DelegateResponseData getListInstances(AwsListASGInstancesTaskParamsRequest awsTaskParams) {
    decryptRequestDTOs(awsTaskParams);
    AwsInternalConfig awsInternalConfig = getAwsInternalConfig(awsTaskParams);

    List<AwsInstance> result = new ArrayList<>();

    List<String> instanceIds =
        getInstanceIds(awsInternalConfig, awsTaskParams.getRegion(), awsTaskParams.getAutoScalingGroupName());

    if (CollectionUtils.isNotEmpty(instanceIds)) {
      result = awsListInstancesDelegateTaskHelper.getListInstances(
          awsInternalConfig, awsTaskParams.getRegion(), instanceIds);
    }

    return AwsListInstancesTaskResponse.builder()
        .instances(result)
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .build();
  }

  public List<String> getInstanceIds(AwsInternalConfig awsInternalConfig, String region, String autoScalingGroupName) {
    List<String> result = new ArrayList<>();

    try (CloseableAmazonWebServiceClient<AmazonAutoScalingClient> closeableAmazonAutoScalingClient =
             new CloseableAmazonWebServiceClient(
                 getAmazonAutoScalingClient(Regions.fromName(region), awsInternalConfig))) {
      DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest =
          new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroupName);
      tracker.trackASGCall("Describe ASGs");
      DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult =
          closeableAmazonAutoScalingClient.getClient().describeAutoScalingGroups(describeAutoScalingGroupsRequest);

      if (!describeAutoScalingGroupsResult.getAutoScalingGroups().isEmpty()) {
        AutoScalingGroup autoScalingGroup = describeAutoScalingGroupsResult.getAutoScalingGroups().get(0);

        result = autoScalingGroup.getInstances().stream().map(Instance::getInstanceId).collect(toList());
      }

      return result;
    } catch (Exception e) {
      log.error("Exception getInstanceIds", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  private AwsInternalConfig getAwsInternalConfig(AwsTaskParams awsTaskParams) {
    AwsConnectorDTO awsConnectorDTO = awsTaskParams.getAwsConnector();
    AwsInternalConfig awsInternalConfig = awsNgConfigMapper.createAwsInternalConfig(awsConnectorDTO);
    awsInternalConfig.setDefaultRegion(awsTaskParams.getRegion());
    return awsInternalConfig;
  }

  private void decryptRequestDTOs(AwsTaskParams awsTaskParams) {
    AwsConnectorDTO awsConnectorDTO = awsTaskParams.getAwsConnector();
    if (awsConnectorDTO.getCredential() != null && awsConnectorDTO.getCredential().getConfig() != null) {
      secretDecryptionService.decrypt(
          (AwsManualConfigSpecDTO) awsConnectorDTO.getCredential().getConfig(), awsTaskParams.getEncryptionDetails());
      ExceptionMessageSanitizer.storeAllSecretsForSanitizing(
          (AwsManualConfigSpecDTO) awsConnectorDTO.getCredential().getConfig(), awsTaskParams.getEncryptionDetails());
    }
  }

  @VisibleForTesting
  AmazonAutoScalingClient getAmazonAutoScalingClient(Regions region, AwsInternalConfig awsConfig) {
    AmazonAutoScalingClientBuilder builder = AmazonAutoScalingClientBuilder.standard().withRegion(region);
    awsApiHelperService.attachCredentialsAndBackoffPolicy(builder, awsConfig);
    return (AmazonAutoScalingClient) builder.build();
  }
}
