package software.wings.resources;

import static java.util.Arrays.asList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Permission.Builder.aPermission;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.security.PermissionAttribute.Action.ALL;
import static software.wings.security.PermissionAttribute.Action.READ;
import static software.wings.security.PermissionAttribute.Action.WRITE;
import static software.wings.security.PermissionAttribute.PermissionScope.APP;
import static software.wings.security.PermissionAttribute.PermissionScope.ENV;
import static software.wings.security.PermissionAttribute.ResourceType.ANY;
import static software.wings.security.PermissionAttribute.ResourceType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.ResourceType.RELEASE;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.ROLE_ID;
import static software.wings.utils.WingsTestConstants.ROLE_NAME;
import static software.wings.utils.WingsTestConstants.USER_EMAIL;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import org.apache.commons.jexl3.JxltEngine.Exception;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import software.wings.beans.Application;
import software.wings.beans.AuthToken;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ErrorCodes;
import software.wings.beans.RestResponse;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.common.AuditHelper;
import software.wings.dl.GenericDbCache;
import software.wings.exception.WingsException;
import software.wings.security.AuthRuleFilter;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.AuthServiceImpl;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * Created by anubhaw on 8/31/16.
 */
public class SecureResourceTest {
  public static final long TOKEN_EXPIRY_IN_MILLIS = 86400000L;
  public static final String VALID_TOKEN = "VALID_TOKEN";
  public static final Entity<Base> ENTITY = Entity.entity(new Base(), APPLICATION_JSON);

  private static AuditService auditService = mock(AuditService.class);
  private static AuditHelper auditHelper = mock(AuditHelper.class);
  private static GenericDbCache genericDbCache = mock(GenericDbCache.class);
  private static AuthService authService = new AuthServiceImpl(genericDbCache);

  private static EnvironmentService environmentService = mock(EnvironmentService.class);

  private static AuthRuleFilter authRuleFilter =
      new AuthRuleFilter(auditService, auditHelper, authService, environmentService);

  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder().addResource(new SecureResource()).addProvider(authRuleFilter).build();

  private final Role appAllResourceReadActionRole = aRole()
                                                        .withAppId(GLOBAL_APP_ID)
                                                        .withName(ROLE_NAME)
                                                        .withUuid(ROLE_ID)
                                                        .withPermissions(asList(aPermission()
                                                                                    .withAppId(APP_ID)
                                                                                    .withEnvId(ENV_ID)
                                                                                    .withPermissionScope(APP)
                                                                                    .withResourceType(ANY)
                                                                                    .withAction(READ)
                                                                                    .build()))
                                                        .build();
  private final Role appAllResourceWriteActionRole = aRole()
                                                         .withAppId(GLOBAL_APP_ID)
                                                         .withName(ROLE_NAME)
                                                         .withUuid(ROLE_ID)
                                                         .withPermissions(asList(aPermission()
                                                                                     .withAppId(APP_ID)
                                                                                     .withEnvId(ENV_ID)
                                                                                     .withPermissionScope(APP)
                                                                                     .withResourceType(ANY)
                                                                                     .withAction(WRITE)
                                                                                     .build()))
                                                         .build();

  private final Role envAllResourceReadActionRole = aRole()
                                                        .withAppId(GLOBAL_APP_ID)
                                                        .withName(ROLE_NAME)
                                                        .withUuid(ROLE_ID)
                                                        .withPermissions(asList(aPermission()
                                                                                    .withAppId(APP_ID)
                                                                                    .withEnvId(ENV_ID)
                                                                                    .withPermissionScope(ENV)
                                                                                    .withResourceType(ANY)
                                                                                    .withAction(READ)
                                                                                    .build()))
                                                        .build();
  private final Role envAllResourceWriteActionRole = aRole()
                                                         .withAppId(GLOBAL_APP_ID)
                                                         .withName(ROLE_NAME)
                                                         .withUuid(ROLE_ID)
                                                         .withPermissions(asList(aPermission()
                                                                                     .withAppId(APP_ID)
                                                                                     .withEnvId(ENV_ID)
                                                                                     .withPermissionScope(ENV)
                                                                                     .withResourceType(ANY)
                                                                                     .withAction(WRITE)
                                                                                     .build()))
                                                         .build();

  private User user =
      anUser().withAppId(GLOBAL_APP_ID).withEmail(USER_EMAIL).withName(USER_NAME).withPassword(PASSWORD).build();

  @Before
  public void setUp() throws Exception {
    when(genericDbCache.get(AuthToken.class, VALID_TOKEN)).thenReturn(new AuthToken(user, TOKEN_EXPIRY_IN_MILLIS));
    when(genericDbCache.get(Application.class, APP_ID))
        .thenReturn(anApplication().withUuid(APP_ID).withAppId(APP_ID).build());
    when(genericDbCache.get(Environment.class, ENV_ID))
        .thenReturn(
            anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withEnvironmentType(EnvironmentType.OTHER).build());
  }

  @After
  public void tearDown() throws Exception {
    UserThreadLocal.unset();
  }

  @Test
  public void shouldGetPublicResourceWithoutAuthorization() {
    Response response = resources.client().target("/secure-resources/publicApiAuthTokenNotRequired").request().get();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void shouldDenyAccessForNonPublicResourceWithoutValidToken() {
    Assertions.assertThatThrownBy(() -> resources.client().target("/secure-resources/NonPublicApi").request().get())
        .hasCauseInstanceOf(WingsException.class)
        .hasStackTraceContaining(ErrorCodes.INVALID_TOKEN.name());
  }

  @Test
  public void shouldRequireAuthorizationByDefaultForNonPublicResource() {
    RestResponse<User> response = resources.client()
                                      .target("/secure-resources/NonPublicApi")
                                      .request()
                                      .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                                      .get(new GenericType<RestResponse<User>>() {});
    assertThat(response.getResource().getEmail()).isEqualTo(USER_EMAIL);
  }

  @Test
  public void shouldAuthorizeAppScopeResourceReadRequestForUserWithRequiredPermission() {
    user.setRoles(asList(appAllResourceReadActionRole));

    RestResponse<User> response = resources.client()
                                      .target("/secure-resources/appResourceReadActionOnAppScope?appId=APP_ID")
                                      .request()
                                      .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                                      .get(new GenericType<RestResponse<User>>() {});
    assertThat(response.getResource().getEmail()).isEqualTo(USER_EMAIL);
  }

  @Test
  public void shouldAuthorizeAppScopeResourceWriteRequestForUserWithRequiredPermission() {
    user.setRoles(asList(appAllResourceWriteActionRole));

    RestResponse<User> response = resources.client()
                                      .target("/secure-resources/appResourceWriteActionOnAppScope?appId=APP_ID")
                                      .request()
                                      .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                                      .post(ENTITY, new GenericType<RestResponse<User>>() {});
    assertThat(response.getResource().getEmail()).isEqualTo(USER_EMAIL);
  }

  @Test
  public void shouldDenyAppScopeResourceReadRequestForUserWithoutRequiredPermission() {
    user.setRoles(asList());

    Assertions
        .assertThatThrownBy(()
                                -> resources.client()
                                       .target("/secure-resources/appResourceReadActionOnAppScope?appId=APP_ID")
                                       .request()
                                       .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                                       .get())
        .hasCauseInstanceOf(WingsException.class)
        .hasStackTraceContaining(ErrorCodes.ACCESS_DENIED.name());
  }

  @Test
  public void shouldDenyAppScopeResourceWriteRequestForUserWithoutRequiredPermission() {
    user.setRoles(asList());

    Assertions
        .assertThatThrownBy(()
                                -> resources.client()
                                       .target("/secure-resources/appResourceWriteActionOnAppScope?appId=APP_ID")
                                       .request()
                                       .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                                       .post(ENTITY))
        .hasCauseInstanceOf(WingsException.class)
        .hasStackTraceContaining(ErrorCodes.ACCESS_DENIED.name());
  }

  @Test
  public void shouldAuthorizeEnvScopeResourceReadRequestForUserWithRequiredPermission() {
    user.setRoles(asList(envAllResourceReadActionRole));

    RestResponse<User> response =
        resources.client()
            .target("/secure-resources/envResourceReadActionOnEnvScope?appId=APP_ID&envId=ENV_ID")
            .request()
            .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
            .get(new GenericType<RestResponse<User>>() {});
    assertThat(response.getResource().getEmail()).isEqualTo(USER_EMAIL);
  }

  @Test
  public void shouldAuthorizeEnvScopeResourceWriteRequestForUserWithRequiredPermission() {
    user.setRoles(asList(envAllResourceWriteActionRole));

    RestResponse<User> response =
        resources.client()
            .target("/secure-resources/envResourceWriteActionOnEnvScope?appId=APP_ID&envId=ENV_ID")
            .request()
            .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
            .post(ENTITY, new GenericType<RestResponse<User>>() {});
    assertThat(response.getResource().getEmail()).isEqualTo(USER_EMAIL);
  }

  @Test
  public void shouldDenyEnvScopeResourceReadRequestForUserWithoutRequiredPermission() {
    user.setRoles(asList());

    Assertions
        .assertThatThrownBy(
            ()
                -> resources.client()
                       .target("/secure-resources/envResourceReadActionOnEnvScope?appId=APP_ID&envId=ENV_ID")
                       .request()
                       .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                       .get())
        .hasCauseInstanceOf(WingsException.class)
        .hasStackTraceContaining(ErrorCodes.ACCESS_DENIED.name());
  }

  @Test
  public void shouldDenyEnvScopeResourceWriteRequestForUserWithoutRequiredPermission() {
    user.setRoles(asList());

    Assertions
        .assertThatThrownBy(
            ()
                -> resources.client()
                       .target("/secure-resources/envResourceWriteActionOnEnvScope?appId=APP_ID&envId=ENV_ID")
                       .request()
                       .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                       .post(ENTITY))
        .hasCauseInstanceOf(WingsException.class)
        .hasStackTraceContaining(ErrorCodes.ACCESS_DENIED.name());
  }

  @Test
  public void shouldAuthorizeEnvScopeReleaseResourceReadRequestForUserWithRequiredPermission() {
    Role envReleaseResourceReadActionRole = aRole()
                                                .withAppId(GLOBAL_APP_ID)
                                                .withName(ROLE_NAME)
                                                .withUuid(ROLE_ID)
                                                .withPermissions(asList(aPermission()
                                                                            .withAppId(APP_ID)
                                                                            .withEnvId(ENV_ID)
                                                                            .withPermissionScope(ENV)
                                                                            .withResourceType(RELEASE)
                                                                            .withAction(READ)
                                                                            .build()))
                                                .build();
    user.setRoles(asList(envReleaseResourceReadActionRole));

    RestResponse<User> response =
        resources.client()
            .target("/secure-resources/releaseResourceReadActionOnEnvScope?appId=APP_ID&envId=ENV_ID")
            .request()
            .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
            .get(new GenericType<RestResponse<User>>() {});
    assertThat(response.getResource().getEmail()).isEqualTo(USER_EMAIL);
  }

  @Test
  public void shouldAuthorizeEnvScopeReleaseResourceWriteRequestForUserWithRequiredPermission() {
    Role envReleaseResourceWriteActionRole = aRole()
                                                 .withAppId(GLOBAL_APP_ID)
                                                 .withName(ROLE_NAME)
                                                 .withUuid(ROLE_ID)
                                                 .withPermissions(asList(aPermission()
                                                                             .withAppId(APP_ID)
                                                                             .withEnvId(ENV_ID)
                                                                             .withPermissionScope(ENV)
                                                                             .withResourceType(RELEASE)
                                                                             .withAction(WRITE)
                                                                             .build()))
                                                 .build();
    user.setRoles(asList(envReleaseResourceWriteActionRole));

    RestResponse<User> response =
        resources.client()
            .target("/secure-resources/releaseResourceWriteActionOnEnvScope?appId=APP_ID&envId=ENV_ID")
            .request()
            .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
            .post(ENTITY, new GenericType<RestResponse<User>>() {});
    assertThat(response.getResource().getEmail()).isEqualTo(USER_EMAIL);
  }

  @Test
  public void shouldDenyEnvScopeReleaseResourceReadRequestForUserWithoutRequiredPermission() {
    Role envDeploymentResourceALLActionRole = aRole()
                                                  .withAppId(GLOBAL_APP_ID)
                                                  .withName(ROLE_NAME)
                                                  .withUuid(ROLE_ID)
                                                  .withPermissions(asList(aPermission()
                                                                              .withAppId(APP_ID)
                                                                              .withEnvId(ENV_ID)
                                                                              .withPermissionScope(ENV)
                                                                              .withResourceType(DEPLOYMENT)
                                                                              .withAction(ALL)
                                                                              .build()))
                                                  .build();
    user.setRoles(asList(envDeploymentResourceALLActionRole));

    Assertions
        .assertThatThrownBy(
            ()
                -> resources.client()
                       .target("/secure-resources/envResourceWriteActionOnEnvScope?appId=APP_ID&envId=ENV_ID")
                       .request()
                       .header(HttpHeaders.AUTHORIZATION, "Bearer VALID_TOKEN")
                       .post(ENTITY))
        .hasCauseInstanceOf(WingsException.class)
        .hasStackTraceContaining(ErrorCodes.ACCESS_DENIED.name());
  }
}
