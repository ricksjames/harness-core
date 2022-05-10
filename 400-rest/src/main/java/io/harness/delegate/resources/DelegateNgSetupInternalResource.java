/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.resources;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateSetupDetails;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.InternalApi;

import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.DelegateService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/delegate-setup/ng")
@Path("/delegate-setup/ng")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
@OwnedBy(HarnessTeam.DEL)
@Hidden
@InternalApi
public class DelegateNgSetupInternalResource {
  private final DelegateService delegateService;
  private final SubdomainUrlHelperIntfc subdomainUrlHelper;

  @Inject
  public DelegateNgSetupInternalResource(DelegateService delegateService, SubdomainUrlHelperIntfc subdomainUrlHelper) {
    this.delegateService = delegateService;
    this.subdomainUrlHelper = subdomainUrlHelper;
  }

  @POST
  @Path("ng/delegate-helm-values-yaml")
  @Timed
  @ExceptionMetered
  public RestResponse<File> generateNgHelmValuesYaml(@Context HttpServletRequest request,
      @Parameter(description = "Account UUID") @QueryParam("accountId") @NotEmpty String accountId,
      @Parameter(description = "Organization ID") @QueryParam("orgId") String orgId,
      @Parameter(description = "Project ID") @QueryParam("projectId") String projectId,
      @RequestBody(
          required = true, description = "Delegate setup details, containing data to populate yaml file values.")
      DelegateSetupDetails delegateSetupDetails) throws IOException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      File delegateFile = delegateService.generateHelmValuesYaml(accountId, delegateSetupDetails,
          subdomainUrlHelper.getManagerUrl(request, accountId), getVerificationUrl(request));
      return new RestResponse<>(delegateFile);
    }
  }

  private String getVerificationUrl(HttpServletRequest request) {
    return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
  }
}
