/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.delegate.resources;

import static io.harness.delegate.utils.RbacConstants.DELEGATE_EDIT_PERMISSION;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_RESOURCE_TYPE;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateSetupDetails;
import io.harness.ng.core.delegate.client.DelegateNgManagerCgManagerClient;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.remote.client.RestClientUtils;
import io.harness.rest.RestResponse;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("delegate-setup-ng")
@Path("/delegate-setup-ng")
@Consumes({"application/json"})
@Slf4j
@OwnedBy(HarnessTeam.DEL)
@Tag(name = "Delegate Setup Resource", description = "Contains APIs related to Delegate Setup in ng")
@ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
public class DelegateSetupNgResource {
  private final DelegateNgManagerCgManagerClient delegateNgManagerCgManagerClient;
  private final AccessControlClient accessControlClient;

  @Inject
  public DelegateSetupNgResource(
      DelegateNgManagerCgManagerClient delegateNgManagerCgManagerClient, AccessControlClient accessControlClient) {
    this.delegateNgManagerCgManagerClient = delegateNgManagerCgManagerClient;
    this.accessControlClient = accessControlClient;
  }

  @POST
  @Timed
  @Path("generate-helm-values-yaml")
  @ExceptionMetered
  @ApiOperation(value = "Generate helm values yaml file", nickname = "generateNgHelmValuesYaml")
  @Operation(operationId = "generateNgHelmValuesYaml",
      summary = "Generates helm values yaml file from the data specified in request body (Delegate setup details).",
      responses = { @ApiResponse(responseCode = "default", description = "Generated yaml file.") })
  public RestResponse<File>
  generateNgHelmValuesYaml(
      @Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @RequestBody(
          required = true, description = "Delegate setup details, containing data to populate yaml file values.")
      DelegateSetupDetails delegateSetupDetails) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(DELEGATE_RESOURCE_TYPE, null), DELEGATE_EDIT_PERMISSION);
    return new RestResponse<>(RestClientUtils.getResponse(delegateNgManagerCgManagerClient.generateHelmValuesFile(
        accountIdentifier, orgIdentifier, projectIdentifier, delegateSetupDetails)));
  }
}
