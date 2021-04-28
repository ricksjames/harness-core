package io.harness.ng.core.user.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.Boolean.TRUE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.accesscontrol.user.ACLAggregateFilter;
import io.harness.ng.accesscontrol.user.AggregateUserService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.invites.dto.UserSearchDTO;
import io.harness.ng.core.user.TwoFactorAuthMechanismInfo;
import io.harness.ng.core.user.TwoFactorAuthSettingsInfo;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserMembershipUpdateMechanism;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.Scope;
import io.harness.ng.core.user.remote.dto.UserAggregateDTO;
import io.harness.ng.core.user.remote.mapper.UserSearchMapper;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.ng.userprofile.services.api.UserInfoService;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import retrofit2.http.Body;

@Api("user")
@Path("user")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
@Slf4j
@OwnedBy(PL)
public class UserResource {
  AggregateUserService aggregateUserService;
  NgUserService ngUserService;
  UserInfoService userInfoService;

  @GET
  @Path("currentUser")
  @ApiOperation(value = "get current user information", nickname = "getCurrentUserInfo")
  public ResponseDTO<UserInfo> getUserInfo() {
    return ResponseDTO.newResponse(userInfoService.getCurrentUser());
  }

  @GET
  @Path("two-factor-auth/{authMechanism}")
  @ApiOperation(value = "get two factor auth settings", nickname = "getTwoFactorAuthSettings")
  public ResponseDTO<TwoFactorAuthSettingsInfo> getTwoFactorAuthSettingsInfo(
      @PathParam("authMechanism") TwoFactorAuthMechanismInfo authMechanism) {
    return ResponseDTO.newResponse(userInfoService.getTwoFactorAuthSettingsInfo(authMechanism));
  }

  @GET
  @Path("usermembership")
  @ApiOperation(value = "Check if user part of scope", nickname = "checkUserMembership", hidden = true)
  public ResponseDTO<Boolean> checkUserMembership(@QueryParam(NGCommonEntityConstants.USER_ID) String userId,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    Scope scope = Scope.builder()
                      .accountIdentifier(accountIdentifier)
                      .orgIdentifier(orgIdentifier)
                      .projectIdentifier(projectIdentifier)
                      .build();
    return ResponseDTO.newResponse(ngUserService.isUserAtScope(userId, scope));
  }

  @GET
  @Path("/aggregate/{userId}")
  @ApiOperation(value = "Get a user by userId for access control", nickname = "getAggregatedUser")
  public ResponseDTO<UserAggregateDTO> getAggregatedUser(@PathParam("userId") String userId,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    UserAggregateDTO aclUserAggregateDTOs =
        aggregateUserService.getAggregatedUser(userId, accountIdentifier, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(aclUserAggregateDTOs);
  }

  @GET
  @Path("currentgen")
  @ApiOperation(value = "Get users from current gen for an account", nickname = "getCurrentGenUsers")
  public ResponseDTO<PageResponse<UserSearchDTO>> getCurrentGenUsers(
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier,
      @QueryParam("searchString") @DefaultValue("") String searchString, @BeanParam PageRequest pageRequest) {
    Pageable pageable = getPageRequest(pageRequest);
    Page<UserInfo> users = ngUserService.listCurrentGenUsers(accountIdentifier, searchString, pageable);
    return ResponseDTO.newResponse(PageUtils.getNGPageResponse(users.map(UserSearchMapper::writeDTO)));
  }

  @GET
  @Path("projects")
  @ApiOperation(value = "get user project information", nickname = "getUserProjectInfo")
  public ResponseDTO<PageResponse<ProjectDTO>> getUserProjectInfo(
      @QueryParam("accountId") String accountId, @BeanParam PageRequest pageRequest) {
    return ResponseDTO.newResponse(PageUtils.getNGPageResponse(ngUserService.listProjects(accountId, pageRequest)));
  }

  @POST
  @Path("batch")
  @ApiOperation(value = "Get a list of users", nickname = "getUsers")
  public ResponseDTO<PageResponse<UserInfo>> getUsers(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Valid @BeanParam PageRequest pageRequest) {
    UserMembership.Scope scope = Scope.builder()
                                     .accountIdentifier(accountIdentifier)
                                     .orgIdentifier(orgIdentifier)
                                     .projectIdentifier(projectIdentifier)
                                     .build();
    return ResponseDTO.newResponse(ngUserService.listUsers(scope, pageRequest));
  }

  @POST
  @Path("aggregate")
  @ApiOperation(value = "Get a page of active users for access control", nickname = "getAggregatedUsers")
  public ResponseDTO<PageResponse<UserAggregateDTO>> getAggregatedUsers(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam("searchTerm") String searchTerm, @BeanParam PageRequest pageRequest,
      ACLAggregateFilter aclAggregateFilter) {
    PageResponse<UserAggregateDTO> aclUserAggregateDTOs = aggregateUserService.getAggregatedUsers(
        accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, pageRequest, aclAggregateFilter);
    return ResponseDTO.newResponse(aclUserAggregateDTOs);
  }

  @PUT
  @ApiOperation(value = "update user information", nickname = "updateUserInfo")
  public ResponseDTO<UserInfo> updateUserInfo(@Body UserInfo userInfo) {
    return ResponseDTO.newResponse(userInfoService.update(userInfo));
  }

  @PUT
  @Path("enable-two-factor-auth")
  @ApiOperation(value = "enable two factor auth settings", nickname = "enableTwoFactorAuth")
  public ResponseDTO<UserInfo> updateTwoFactorAuthInfo(@Body TwoFactorAuthSettingsInfo authSettingsInfo) {
    return ResponseDTO.newResponse(userInfoService.updateTwoFactorAuthInfo(authSettingsInfo));
  }

  @PUT
  @Path("disable-two-factor-auth")
  @ApiOperation(value = "disable two factor auth settings", nickname = "disableTwoFactorAuth")
  public ResponseDTO<UserInfo> disableTFA() {
    return ResponseDTO.newResponse(userInfoService.disableTFA());
  }

  @DELETE
  @Path("{userId}")
  @Produces("application/json")
  @Consumes()
  @ApiOperation(value = "Remove user as the collaborator from the scope", nickname = "removeUser")
  public ResponseDTO<Boolean> removeUser(@NotNull @PathParam("userId") String userId,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    Scope scope = Scope.builder()
                      .accountIdentifier(accountIdentifier)
                      .orgIdentifier(orgIdentifier)
                      .projectIdentifier(projectIdentifier)
                      .build();
    return ResponseDTO.newResponse(
        TRUE.equals(ngUserService.removeUserFromScope(userId, scope, UserMembershipUpdateMechanism.AUTHORIZED_USER)));
  }
}
