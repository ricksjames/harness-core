/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.azure.impl;

import static io.harness.azure.model.AzureConstants.OBJECT_ID_NAME_BLANK_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.ROLE_ASSIGNMENT_NAME_BLANK_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.SUBSCRIPTION_ID_NULL_VALIDATION_MSG;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.AzureClient;
import io.harness.azure.client.AzureAuthorizationClient;
import io.harness.azure.client.AzureAuthorizationRestClient;
import io.harness.azure.model.AzureAuthenticationType;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureConstants;
import io.harness.azure.utility.AzureUtils;
import io.harness.concurrent.HTimeLimiter;
import io.harness.exception.AzureAuthenticationException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;

import software.wings.helpers.ext.azure.AzureIdentityAccessTokenResponse;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.graphrbac.BuiltInRole;
import com.microsoft.azure.management.graphrbac.RoleAssignment;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

@Singleton
@Slf4j
public class AzureAuthorizationClientImpl extends AzureClient implements AzureAuthorizationClient {
  @Inject TimeLimiter timeLimiter;

  public RoleAssignment roleAssignmentAtSubscriptionScope(final AzureConfig azureConfig, final String subscriptionId,
      final String objectId, final String roleAssignmentName, final BuiltInRole builtInRole) {
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }
    if (isBlank(objectId)) {
      throw new IllegalArgumentException(OBJECT_ID_NAME_BLANK_VALIDATION_MSG);
    }
    if (isBlank(roleAssignmentName)) {
      throw new IllegalArgumentException(ROLE_ASSIGNMENT_NAME_BLANK_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig, subscriptionId);

    log.debug(
        "Start role assignment at subscription scope, subscriptionId: {}, objectId: {}, roleAssignmentName: {}, builtInRole: {}",
        subscriptionId, objectId, roleAssignmentName, builtInRole.toString());
    return azure.accessManagement()
        .roleAssignments()
        .define(roleAssignmentName)
        .forObjectId(objectId)
        .withBuiltInRole(builtInRole)
        .withSubscriptionScope(subscriptionId)
        .create();
  }

  public List<RoleAssignment> getRoleDefinition(
      final AzureConfig azureConfig, final String scope, final String roleName) {
    if (isBlank(scope)) {
      throw new IllegalArgumentException("Parameter scope cannot be null or empty");
    }
    if (isBlank(roleName)) {
      throw new IllegalArgumentException("Parameter roleName cannot be null or empty");
    }

    Azure azure = getAzureClient(azureConfig, scope);

    log.debug("Start getting role definition at scope: {}, subscriptionId: {}, roleName: {}", scope, roleName);
    PagedList<RoleAssignment> roleAssignments = azure.accessManagement().roleAssignments().listByScope(scope);
    return new ArrayList<>(roleAssignments.size());
  }

  @Override
  public boolean validateAzureConnection(AzureConfig azureConfig) {
    try {
      return HTimeLimiter.callInterruptible21(timeLimiter, Duration.ofSeconds(90L), () -> {
        Azure azure = getAzureClientWithDefaultSubscription(azureConfig);
        AzureAuthenticationType azureCredentialType = azureConfig.getAzureAuthenticationType();
        StringBuffer message = new StringBuffer("Azure connection validation for ");
        if (azureCredentialType == AzureAuthenticationType.SERVICE_PRINCIPAL_CERT
            || azureCredentialType == AzureAuthenticationType.SERVICE_PRINCIPAL_SECRET) {
          message.append(format("clientId %s", message, azureConfig.getClientId()));
        } else if (azureCredentialType == AzureAuthenticationType.MANAGED_IDENTITY_USER_ASSIGNED) {
          message.append(format("User Assigned Managed Identity %s", message, azureConfig.getClientId()));
        } else if (azureCredentialType == AzureAuthenticationType.MANAGED_IDENTITY_SYSTEM_ASSIGNED) {
          message.append(format("System Assigned Managed Identity", message));
        }

        boolean result = azure != null;
        log.info(format("%s was %s", message.toString(), result ? "successful" : "unsuccessful"));

        return result;
      });
    } catch (UncheckedTimeoutException e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Timeout occurred. Failed to validate connection for Azure connector.",
          "Please check your Azure connector configuration.",
          new AzureAuthenticationException(
              "Failed to validate connection for Azure connector", WingsException.USER, e));
    } catch (WingsException we) {
      throw we;
    } catch (Exception e) {
      throw NestedExceptionUtils.hintWithExplanationException("Failed to validate connection for Azure connector.",
          "Please check your Azure connector configuration.",
          new AzureAuthenticationException(e.getMessage(), WingsException.USER, e));
    }
  }

  @Override
  public AzureIdentityAccessTokenResponse getUserAccessToken(AzureConfig azureConfig) {
    try {
      AzureAuthenticationType azureCredentialType = azureConfig.getAzureAuthenticationType();
      AzureAuthorizationRestClient azureAuthorizationRestClient =
          getAzureRestClient(AzureUtils.AUTH_URL, AzureAuthorizationRestClient.class);
      Response<AzureIdentityAccessTokenResponse> response = null;
      if (azureCredentialType == AzureAuthenticationType.SERVICE_PRINCIPAL_CERT) {
        String clientAssertion = createClientAssertion(azureConfig);
        response = azureAuthorizationRestClient
                       .servicePrincipalAccessToken(azureConfig.getTenantId(),
                           AzureConstants.CLIENT_CREDENTIALS_GRANT_TYPE, azureConfig.getClientId(),
                           AzureUtils.AUTH_SCOPE, AzureConstants.JWT_BEARER_CLIENT_ASSERTION_TYPE, clientAssertion)
                       .execute();

      } else if (azureCredentialType == AzureAuthenticationType.SERVICE_PRINCIPAL_SECRET) {
        response =
            azureAuthorizationRestClient
                .servicePrincipalAccessToken(azureConfig.getTenantId(), AzureConstants.CLIENT_CREDENTIALS_GRANT_TYPE,
                    azureConfig.getClientId(), AzureUtils.AUTH_SCOPE, String.valueOf(azureConfig.getKey()))
                .execute();
      }

      if (response != null) {
        if (response.isSuccessful()) {
          return response.body();
        }
        String errorBody = response.errorBody().string();
        throw NestedExceptionUtils.hintWithExplanationException("Fetching access token for azure user has failed.",
            "Please check your connector configuration",
            new AzureAuthenticationException("Response Code: " + response.code()
                + ", Response Message: " + response.message() + ", Error Body: " + errorBody));
      }
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }

    throw NestedExceptionUtils.hintWithExplanationException("Fetching access token for azure user has failed.",
        "Please use a different type (non managed identity) of Azure connector for this action",
        new AzureAuthenticationException(
            "Retrieving ManagedIdentity access token is currently not supported via REST API"));
  }
}
