/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.DelegateSize.LAPTOP;
import static io.harness.delegate.beans.DelegateSize.LARGE;
import static io.harness.delegate.beans.DelegateSize.MEDIUM;
import static io.harness.delegate.beans.DelegateSize.SMALL;
import static io.harness.delegate.beans.DelegateType.DOCKER;
import static io.harness.delegate.beans.DelegateType.KUBERNETES;
import static io.harness.delegate.beans.K8sPermissionType.CLUSTER_ADMIN;
import static io.harness.delegate.beans.K8sPermissionType.CLUSTER_VIEWER;
import static io.harness.delegate.beans.K8sPermissionType.NAMESPACE_ADMIN;

import static java.lang.String.format;

import io.harness.delegate.DelegateDownloadResponse;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateSetupDetails;
import io.harness.delegate.beans.DelegateTokenDetails;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.delegate.beans.K8sConfigDetails;
import io.harness.delegate.service.intfc.DelegateDownloadService;
import io.harness.delegate.service.intfc.DelegateNgTokenService;
import io.harness.delegate.utils.DelegateEntityOwnerHelper;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;

import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import java.io.File;
import java.util.Arrays;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DelegateDownloadServiceImpl implements DelegateDownloadService {
  private final DelegateService delegateService;
  private final DelegateNgTokenService delegateNgTokenService;

  @Inject
  public DelegateDownloadServiceImpl(DelegateService delegateService, DelegateNgTokenService delegateNgTokenService) {
    this.delegateService = delegateService;
    this.delegateNgTokenService = delegateNgTokenService;
  }

  @Override
  public DelegateDownloadResponse downloadNgDelegate(
      String accountId, DelegateSetupDetails delegateSetupDetails, String managerHost, String verificationServiceUrl) {
    try {
      if (DOCKER.equals(delegateSetupDetails.getDelegateType())) {
        return downloadNgDockerDelegate(accountId, delegateSetupDetails, managerHost, verificationServiceUrl);
      }
      if (KUBERNETES.equals(delegateSetupDetails.getDelegateType())) {
        return downloadNgKubernetesDelegate(accountId, delegateSetupDetails, managerHost, verificationServiceUrl);
      }
      return new DelegateDownloadResponse(
          "Invalid delegate type given. Delegate type must be either of KUBERNETES or DOCKER.", null);
    } catch (Exception e) {
      log.error("Error occurred during downloading ng delegate.", e);
      return new DelegateDownloadResponse(ExceptionUtils.getMessage(e), null);
    }
  }

  private DelegateDownloadResponse downloadNgKubernetesDelegate(
      String accountId, DelegateSetupDetails delegateSetupDetails, String managerHost, String verificationServiceUrl) {
    try {
      checkAndBuildProperDelegateSetupDetails(accountId, delegateSetupDetails, KUBERNETES);
      File delegateFile = delegateService.generateKubernetesYaml(
          accountId, delegateSetupDetails, managerHost, verificationServiceUrl, MediaType.TEXT_PLAIN_TYPE);
      return new DelegateDownloadResponse(null, delegateFile);
    } catch (Exception e) {
      log.error("Error occurred during downloading ng kubernetes delegate.", e);
      return new DelegateDownloadResponse(ExceptionUtils.getMessage(e), null);
    }
  }

  private DelegateDownloadResponse downloadNgDockerDelegate(
      String accountId, DelegateSetupDetails delegateSetupDetails, String managerHost, String verificationServiceUrl) {
    try {
      checkAndBuildProperDelegateSetupDetails(accountId, delegateSetupDetails, DOCKER);
      File delegateFile =
          delegateService.downloadNgDocker(managerHost, verificationServiceUrl, accountId, delegateSetupDetails);
      return new DelegateDownloadResponse(null, delegateFile);
    } catch (Exception e) {
      log.error("Error occurred during downloading ng docker delegate.", e);
      return new DelegateDownloadResponse(ExceptionUtils.getMessage(e), null);
    }
  }

  private void checkAndBuildProperDelegateSetupDetails(
      String accountId, DelegateSetupDetails delegateSetupDetails, String delegateType) {
    delegateService.checkUniquenessOfDelegateName(accountId, delegateSetupDetails.getName(), true);

    DelegateEntityOwner owner = DelegateEntityOwnerHelper.buildOwner(
        delegateSetupDetails.getOrgIdentifier(), delegateSetupDetails.getProjectIdentifier());

    String delegateToken = delegateSetupDetails.getTokenName();
    if (isEmpty(delegateToken)) {
      delegateToken = delegateNgTokenService.getDefaultTokenName(owner);
    }
    DelegateTokenDetails delegateTokenDetails = delegateNgTokenService.getDelegateToken(accountId, delegateToken);
    if (delegateTokenDetails == null || DelegateTokenStatus.REVOKED.equals(delegateTokenDetails.getStatus())) {
      throw new InvalidRequestException(format(
          "Can not use %s delegate token. This token does not exists or has been revoked. Please specify a valid delegate token.",
          delegateToken));
    }
    delegateSetupDetails.setTokenName(delegateToken);

    // properties specific for k8s delegate
    if (delegateType.equals(KUBERNETES)) {
      if (!Arrays.asList(LAPTOP, SMALL, MEDIUM, LARGE).contains(delegateSetupDetails.getSize())) {
        delegateSetupDetails.setSize(LAPTOP);
      }

      K8sConfigDetails k8sConfigDetails = delegateSetupDetails.getK8sConfigDetails();
      if (k8sConfigDetails == null
          || !Arrays.asList(CLUSTER_ADMIN, CLUSTER_VIEWER, NAMESPACE_ADMIN)
                  .contains(delegateSetupDetails.getK8sConfigDetails().getK8sPermissionType())) {
        delegateSetupDetails.setK8sConfigDetails(K8sConfigDetails.builder().k8sPermissionType(CLUSTER_ADMIN).build());
      } else if (NAMESPACE_ADMIN.equals(k8sConfigDetails.getK8sPermissionType())
          && isEmpty(k8sConfigDetails.getNamespace())) {
        throw new InvalidRequestException("K8s namespace must be provided for this type of permission.");
      }
    }
  }
}
