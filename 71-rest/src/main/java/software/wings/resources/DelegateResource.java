package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.common.Constants.DELEGATE_DIR;
import static software.wings.common.Constants.DOCKER_DELEGATE;
import static software.wings.common.Constants.ECS_DELEGATE;
import static software.wings.common.Constants.KUBERNETES_DELEGATE;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import freemarker.template.TemplateException;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.task.TaskParameters;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.app.MainConfiguration;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnectionHeartbeat;
import software.wings.beans.DelegatePackage;
import software.wings.beans.DelegateProfileParams;
import software.wings.beans.DelegateStatus;
import software.wings.beans.DelegateTaskEvent;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.WingsPersistence;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.GoogleDataStoreServiceImpl;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.DelegateScopeService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DownloadTokenService;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
@Api("delegates")
@Path("/delegates")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
public class DelegateResource {
  private DelegateService delegateService;
  private DelegateScopeService delegateScopeService;
  private DownloadTokenService downloadTokenService;
  private MainConfiguration mainConfiguration;
  private DataStoreService dataStoreService;
  private AccountService accountService;
  private WingsPersistence wingsPersistence;

  @Inject
  public DelegateResource(DelegateService delegateService, DelegateScopeService delegateScopeService,
      DownloadTokenService downloadTokenService, MainConfiguration mainConfiguration, DataStoreService dataStoreService,
      AccountService accountService, WingsPersistence wingsPersistence) {
    this.delegateService = delegateService;
    this.delegateScopeService = delegateScopeService;
    this.downloadTokenService = downloadTokenService;
    this.mainConfiguration = mainConfiguration;
    this.dataStoreService = dataStoreService;
    this.accountService = accountService;
    this.wingsPersistence = wingsPersistence;
  }

  @GET
  @ApiImplicitParams(
      { @ApiImplicitParam(name = "accountId", required = true, dataType = "string", paramType = "query") })
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Delegate>>
  list(@BeanParam PageRequest<Delegate> pageRequest) {
    return new RestResponse<>(delegateService.list(pageRequest));
  }

  @DelegateAuth
  @GET
  @Path("configuration")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateConfiguration> getDelegateConfiguration(
      @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(accountService.getDelegateConfiguration(accountId));
  }

  @GET
  @Path("status")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateStatus> listDelegateStatus(@QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateService.getDelegateStatus(accountId));
  }

  @GET
  @Path("available-versions-for-verification")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<String>> getAvailableVersions(@QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateService.getAvailableVersions(accountId));
  }

  @GET
  @Path("kubernetes-delegates")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> kubernetesDelegateNames(
      @Context HttpServletRequest request, @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateService.getKubernetesDelegateNames(accountId));
  }

  @GET
  @Path("delegate-tags")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<String>> delegateTags(
      @Context HttpServletRequest request, @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateService.getAllDelegateTags(accountId));
  }

  @GET
  @Path("{delegateId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Delegate> get(
      @PathParam("delegateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateService.get(accountId, delegateId, true));
  }

  @GET
  @Path("latest")
  @Timed
  @ExceptionMetered
  public RestResponse<String> get(@QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateService.getLatestDelegateVersion(accountId));
  }

  @DelegateAuth
  @PUT
  @Path("{delegateId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Delegate> update(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    delegate.setAccountId(accountId);
    delegate.setUuid(delegateId);

    Delegate existingDelegate = delegateService.get(accountId, delegateId, true);
    if (existingDelegate != null) {
      delegate.setDelegateType(existingDelegate.getDelegateType());
      delegate.setDelegateGroupName(existingDelegate.getDelegateGroupName());
    }

    return new RestResponse<>(delegateService.update(delegate));
  }

  @PUT
  @Path("{delegateId}/description")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Delegate> updateDescription(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, @Trimmed String newDescription) {
    return new RestResponse<>(delegateService.updateDescription(accountId, delegateId, newDescription));
  }

  @DELETE
  @Path("{delegateId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Void> delete(
      @PathParam("delegateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    delegateService.delete(accountId, delegateId);
    return new RestResponse<>();
  }

  @DELETE
  @Path("delete-all-except")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Void> deleteAllExcept(
      @QueryParam("accountId") @NotEmpty String accountId, List<String> delegatesToRetain) {
    delegateService.retainOnlySelectedDelegatesAndDeleteRest(accountId, delegatesToRetain);
    return new RestResponse<>();
  }

  @PUT
  @Path("{delegateId}/scopes")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Delegate> updateScopes(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateScopes delegateScopes) {
    Delegate delegate = delegateService.get(accountId, delegateId, true);
    if (delegateScopes == null) {
      delegate.setIncludeScopes(null);
      delegate.setExcludeScopes(null);
    } else {
      if (isNotEmpty(delegateScopes.getIncludeScopeIds())) {
        delegate.setIncludeScopes(delegateScopes.getIncludeScopeIds()
                                      .stream()
                                      .map(s -> delegateScopeService.get(accountId, s))
                                      .collect(toList()));
      } else {
        delegate.setIncludeScopes(null);
      }
      if (isNotEmpty(delegateScopes.getExcludeScopeIds())) {
        delegate.setExcludeScopes(delegateScopes.getExcludeScopeIds()
                                      .stream()
                                      .map(s -> delegateScopeService.get(accountId, s))
                                      .collect(toList()));
      } else {
        delegate.setExcludeScopes(null);
      }
    }
    return new RestResponse<>(delegateService.updateScopes(delegate));
  }

  private static class DelegateScopes {
    private List<String> includeScopeIds;
    private List<String> excludeScopeIds;

    public List<String> getIncludeScopeIds() {
      return includeScopeIds;
    }

    public void setIncludeScopeIds(List<String> includeScopeIds) {
      this.includeScopeIds = includeScopeIds;
    }

    public List<String> getExcludeScopeIds() {
      return excludeScopeIds;
    }

    public void setExcludeScopeIds(List<String> excludeScopeIds) {
      this.excludeScopeIds = excludeScopeIds;
    }
  }

  @PUT
  @Path("{delegateId}/tags")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Delegate> updateTags(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateTags delegateTags) {
    Delegate delegate = delegateService.get(accountId, delegateId, true);
    delegate.setTags(delegateTags.getTags());
    return new RestResponse<>(delegateService.updateTags(delegate));
  }
  private static class DelegateTags {
    private List<String> tags;
    public List<String> getTags() {
      return tags;
    }
    public void setTags(List<String> tags) {
      this.tags = tags;
    }
  }

  @DelegateAuth
  @POST
  @Path("register")
  @Timed
  @ExceptionMetered
  public RestResponse<Delegate> register(@QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    delegate.setAccountId(accountId);
    long startTime = System.currentTimeMillis();
    Delegate register = delegateService.register(delegate);
    logger.info("Delegate registration took {} in ms", System.currentTimeMillis() - startTime);
    return new RestResponse<>(register);
  }

  @DelegateAuth
  @GET
  @Path("{delegateId}/profile")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateProfileParams> checkForProfile(@QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("delegateId") String delegateId, @QueryParam("profileId") String profileId,
      @QueryParam("lastUpdatedAt") Long lastUpdatedAt) {
    DelegateProfileParams profileParams =
        delegateService.checkForProfile(accountId, delegateId, profileId, lastUpdatedAt);
    return new RestResponse<>(profileParams);
  }

  @GET
  @Path("{delegateId}/profile-result")
  @Timed
  @ExceptionMetered
  public RestResponse<String> getProfileResult(
      @PathParam("delegateId") String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateService.getProfileResult(accountId, delegateId));
  }

  @DelegateAuth
  @POST
  @Path("connectionHeartbeat/{delegateId}")
  @Timed
  @ExceptionMetered
  public void connectionHeartbeat(@QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("delegateId") String delegateId, DelegateConnectionHeartbeat connectionHeartbeat) {
    delegateService.doConnectionHeartbeat(accountId, delegateId, connectionHeartbeat);
  }

  @POST
  public RestResponse<Delegate> add(@QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    delegate.setAccountId(accountId);
    return new RestResponse<>(delegateService.add(delegate));
  }

  @GET
  @Path("downloadUrl")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> downloadUrl(
      @Context HttpServletRequest request, @QueryParam("accountId") @NotEmpty String accountId) {
    String url = getManagerUrl(request);

    return new RestResponse<>(ImmutableMap.of("downloadUrl",
        url + request.getRequestURI().replace("downloadUrl", "download") + "?accountId=" + accountId
            + "&token=" + downloadTokenService.createDownloadToken("delegate." + accountId),
        "dockerUrl",
        url + request.getRequestURI().replace("downloadUrl", "docker") + "?accountId=" + accountId
            + "&token=" + downloadTokenService.createDownloadToken("delegate." + accountId),
        "kubernetesUrl",
        url + request.getRequestURI().replace("downloadUrl", "kubernetes") + "?accountId=" + accountId
            + "&token=" + downloadTokenService.createDownloadToken("delegate." + accountId),
        "ecsUrl",
        url + request.getRequestURI().replace("downloadUrl", "ecs") + "?accountId=" + accountId
            + "&token=" + downloadTokenService.createDownloadToken("delegate." + accountId)));
  }

  @PublicApi
  @GET
  @Path("download")
  @Timed
  @ExceptionMetered
  public Response downloadScripts(@Context HttpServletRequest request,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("token") @NotEmpty String token)
      throws IOException, TemplateException {
    downloadTokenService.validateDownloadToken("delegate." + accountId, token);
    File delegateFile = delegateService.downloadScripts(getManagerUrl(request), getVerificationUrl(request), accountId);
    return Response.ok(delegateFile)
        .header("Content-Transfer-Encoding", "binary")
        .type("application/zip; charset=binary")
        .header("Content-Disposition", "attachment; filename=" + DELEGATE_DIR + ".tar.gz")
        .build();
  }

  @PublicApi
  @GET
  @Path("docker")
  @Timed
  @ExceptionMetered
  public Response downloadDocker(@Context HttpServletRequest request,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("token") @NotEmpty String token)
      throws IOException, TemplateException {
    downloadTokenService.validateDownloadToken("delegate." + accountId, token);
    File delegateFile = delegateService.downloadDocker(getManagerUrl(request), getVerificationUrl(request), accountId);
    return Response.ok(delegateFile)
        .header("Content-Transfer-Encoding", "binary")
        .type("application/zip; charset=binary")
        .header("Content-Disposition", "attachment; filename=" + DOCKER_DELEGATE + ".tar.gz")
        .build();
  }

  @PublicApi
  @GET
  @Path("kubernetes")
  @Timed
  @ExceptionMetered
  public Response downloadKubernetes(@Context HttpServletRequest request,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("delegateName") @NotEmpty String delegateName,
      @QueryParam("delegateProfileId") String delegateProfileId, @QueryParam("token") @NotEmpty String token)
      throws IOException, TemplateException {
    downloadTokenService.validateDownloadToken("delegate." + accountId, token);
    File delegateFile = delegateService.downloadKubernetes(
        getManagerUrl(request), getVerificationUrl(request), accountId, delegateName, delegateProfileId);
    return Response.ok(delegateFile)
        .header("Content-Transfer-Encoding", "binary")
        .type("application/zip; charset=binary")
        .header("Content-Disposition", "attachment; filename=" + KUBERNETES_DELEGATE + ".tar.gz")
        .build();
  }

  @PublicApi
  @GET
  @Path("ecs")
  @Timed
  @ExceptionMetered
  public Response downloadEcs(@Context HttpServletRequest request, @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("delegateGroupName") @NotEmpty String delegateGroupName, @QueryParam("awsVpcMode") Boolean awsVpcMode,
      @QueryParam("hostname") String hostname, @QueryParam("delegateProfileId") String delegateProfileId,
      @QueryParam("token") @NotEmpty String token) throws IOException, TemplateException {
    downloadTokenService.validateDownloadToken("delegate." + accountId, token);
    File delegateFile = delegateService.downloadECSDelegate(getManagerUrl(request), getVerificationUrl(request),
        accountId, awsVpcMode, hostname, delegateGroupName, delegateProfileId);
    return Response.ok(delegateFile)
        .header("Content-Transfer-Encoding", "binary")
        .type("application/zip; charset=binary")
        .header("Content-Disposition", "attachment; filename=" + ECS_DELEGATE + ".tar.gz")
        .build();
  }

  private String getManagerUrl(HttpServletRequest request) {
    String apiUrl = mainConfiguration.getApiUrl();
    return !StringUtils.isEmpty(apiUrl)
        ? apiUrl
        : request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
  }

  private String getVerificationUrl(HttpServletRequest request) {
    return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
  }

  @DelegateAuth
  @POST
  @Path("{delegateId}/tasks/{taskId}")
  @Consumes("application/x-kryo")
  @Timed
  @ExceptionMetered
  public void updateTaskResponse(@PathParam("delegateId") String delegateId, @PathParam("taskId") String taskId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateTaskResponse delegateTaskResponse) {
    delegateService.processDelegateResponse(accountId, delegateId, taskId, delegateTaskResponse);
  }

  @DelegateAuth
  @PUT
  @Produces("application/x-kryo")
  @Path("{delegateId}/tasks/{taskId}/acquire")
  @Timed
  @ExceptionMetered
  public DelegatePackage acquireDelegateTask(@PathParam("delegateId") String delegateId,
      @PathParam("taskId") String taskId, @QueryParam("accountId") @NotEmpty String accountId) {
    final DelegatePackage delegatePackage = delegateService.acquireDelegateTask(accountId, delegateId, taskId);

    if (delegatePackage != null && delegatePackage.getDelegateTask() != null
        && delegatePackage.getDelegateTask().getData() != null) {
      final Object[] parameters = delegatePackage.getDelegateTask().getData().getParameters();

      if (parameters != null && (parameters.length != 1 || !(parameters[0] instanceof TaskParameters))) {
        logger.warn(
            "Task {} need task parameters refactoring", delegatePackage.getDelegateTask().getData().getTaskType());
      }
    }

    return delegatePackage;
  }

  @DelegateAuth
  @POST
  @Produces("application/x-kryo")
  @Path("{delegateId}/tasks/{taskId}/report")
  @Timed
  @ExceptionMetered
  public DelegatePackage reportConnectionResults(@PathParam("delegateId") String delegateId,
      @PathParam("taskId") String taskId, @QueryParam("accountId") @NotEmpty String accountId,
      List<DelegateConnectionResult> results) {
    return delegateService.reportConnectionResults(accountId, delegateId, taskId, results);
  }

  @DelegateAuth
  @GET
  @Produces("application/x-kryo")
  @Path("{delegateId}/tasks/{taskId}/fail")
  @Timed
  @ExceptionMetered
  public void failIfAllDelegatesFailed(@PathParam("delegateId") String delegateId, @PathParam("taskId") String taskId,
      @QueryParam("accountId") @NotEmpty String accountId) {
    delegateService.failIfAllDelegatesFailed(accountId, delegateId, taskId);
  }

  @DelegateAuth
  @PUT
  @Path("{delegateId}/clear-cache")
  @Timed
  @ExceptionMetered
  public void clearCache(
      @PathParam("delegateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    delegateService.clearCache(accountId, delegateId);
  }

  @DelegateAuth
  @GET
  @Path("{delegateId}/upgrade")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateScripts> checkForUpgrade(@Context HttpServletRequest request,
      @HeaderParam("Version") String version, @PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId) throws IOException, TemplateException {
    return new RestResponse<>(
        delegateService.getDelegateScripts(accountId, version, getManagerUrl(request), getVerificationUrl(request)));
  }

  @DelegateAuth
  @GET
  @Path("delegateScripts")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateScripts> getDelegateScripts(@Context HttpServletRequest request,
      @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("delegateVersion") @NotEmpty String delegateVersion) throws IOException, TemplateException {
    return new RestResponse<>(delegateService.getDelegateScripts(
        accountId, delegateVersion, getManagerUrl(request), getVerificationUrl(request)));
  }

  @DelegateAuth
  @GET
  @Path("{delegateId}/task-events")
  @Timed
  @ExceptionMetered
  public List<DelegateTaskEvent> getDelegateTaskEvents(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("syncOnly") boolean syncOnly) {
    return delegateService.getDelegateTaskEvents(accountId, delegateId, syncOnly);
  }

  @DelegateAuth
  @POST
  @Path("heartbeat-with-polling")
  @Timed
  @ExceptionMetered
  public RestResponse<Delegate> updateDelegateHB(
      @QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    // delegate.isPolllingModeEnabled() will be true here.
    if ("ECS".equals(delegate.getDelegateType())) {
      Delegate registeredDelegate = delegateService.handleEcsDelegateRequest(delegate);
      return new RestResponse<>(registeredDelegate);
    } else {
      return new RestResponse<>(delegateService.updateHeartbeatForDelegateWithPollingEnabled(delegate));
    }
  }

  @DelegateAuth
  @POST
  @Path("{delegateId}/state-executions")
  @Timed
  @ExceptionMetered
  public void saveApiCallLogs(@PathParam("delegateId") String delegateId, @QueryParam("accountId") String accountId,
      List<ThirdPartyApiCallLog> logs) {
    dataStoreService.save(ThirdPartyApiCallLog.class, logs, false);
    if (dataStoreService instanceof GoogleDataStoreServiceImpl) {
      wingsPersistence.save(logs);
    }
  }
}
