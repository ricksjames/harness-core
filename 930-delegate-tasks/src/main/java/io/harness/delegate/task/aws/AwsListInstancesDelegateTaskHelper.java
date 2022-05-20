/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.aws;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AwsCallTracker;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsListInstancesTaskParamsRequest;
import io.harness.delegate.beans.connector.awsconnector.AwsListInstancesTaskResponse;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskParams;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.SecretDecryptionService;

import software.wings.api.DeploymentType;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.AwsInstanceFilter.AwsInstanceFilterBuilder;
import software.wings.beans.AwsInstanceFilter.Tag;
import software.wings.delegatetasks.ExceptionMessageSanitizer;
import software.wings.service.impl.AwsApiHelperService;
import software.wings.service.impl.AwsUtils;
import software.wings.service.impl.aws.client.CloseableAmazonWebServiceClient;
import software.wings.service.impl.aws.model.AwsInstance;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Reservation;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.jsonwebtoken.lang.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CDP)
public class AwsListInstancesDelegateTaskHelper {
  @Inject private AwsApiHelperService awsApiHelperService;
  @Inject private AwsNgConfigMapper awsNgConfigMapper;
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private AwsCallTracker tracker;
  @Inject private AwsUtils awsUtils;

  public DelegateResponseData getListInstances(AwsListInstancesTaskParamsRequest awsTaskParams) {
    decryptRequestDTOs(awsTaskParams);
    AwsInternalConfig awsInternalConfig = getAwsInternalConfig(awsTaskParams);

    AwsInstanceFilterBuilder awsInstanceFilterBuilder = AwsInstanceFilter.builder().vpcIds(awsTaskParams.getVpcIds());

    if (!Collections.isEmpty(awsTaskParams.getTags())) {
      List<Tag> tags = awsTaskParams.getTags()
                           .entrySet()
                           .stream()
                           .map(entry -> Tag.builder().key(entry.getKey()).value(entry.getValue()).build())
                           .collect(toList());
      awsInstanceFilterBuilder.tags(tags);
    }

    DeploymentType deploymentType = awsTaskParams.isWinRm() ? DeploymentType.WINRM : DeploymentType.SSH;
    List<Filter> filters = awsUtils.getFilters(deploymentType, awsInstanceFilterBuilder.build());

    try (
        CloseableAmazonWebServiceClient<AmazonEC2Client> closeableAmazonEC2Client = new CloseableAmazonWebServiceClient(
            getAmazonEc2Client(Regions.fromName(awsTaskParams.getRegion()), awsInternalConfig))) {
      List<AwsInstance> result = new ArrayList<>();
      String nextToken = null;
      if (isNotEmpty(filters)) {
        tracker.trackEC2Call("Filters: "
            + filters.stream()
                  .map(f
                      -> String.format("[Name:%s]-[Values:[%s]]", f.getName(),
                          isNotEmpty(f.getValues()) ? String.join(",", f.getValues()) : ""))
                  .collect(Collectors.joining(",")));
      }
      do {
        DescribeInstancesRequest describeInstancesRequest =
            new DescribeInstancesRequest().withNextToken(nextToken).withFilters(filters);
        tracker.trackEC2Call("List Ec2 instances");
        DescribeInstancesResult describeInstancesResult =
            closeableAmazonEC2Client.getClient().describeInstances(describeInstancesRequest);
        result.addAll(getInstanceList(describeInstancesResult));
        nextToken = describeInstancesResult.getNextToken();
      } while (nextToken != null);
      if (isNotEmpty(result)) {
        tracker.trackEC2Call(
            "Found instances: " + result.stream().map(AwsInstance::getInstanceId).collect(Collectors.joining(",")));
      } else {
        tracker.trackEC2Call("Found 0 instances");
      }
      return AwsListInstancesTaskResponse.builder()
          .instances(result)
          .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
          .build();
    } catch (Exception e) {
      log.error("Exception getListInstances", e);
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

  AmazonEC2Client getAmazonEc2Client(Regions region, AwsInternalConfig awsConfig) {
    AmazonEC2ClientBuilder builder = AmazonEC2ClientBuilder.standard().withRegion(region);
    awsApiHelperService.attachCredentialsAndBackoffPolicy(builder, awsConfig);
    return (AmazonEC2Client) builder.build();
  }

  private List<AwsInstance> getInstanceList(DescribeInstancesResult result) {
    return result.getReservations()
        .stream()
        .map(Reservation::getInstances)
        .flatMap(List::stream)
        .map(o -> AwsInstance.builder().instanceId(o.getInstanceId()).publicDnsName(o.getPublicDnsName()).build())
        .collect(Collectors.toList());
  }
}
