package software.wings.signup;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_EMAIL;
import static io.harness.exception.WingsException.USER;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.harness.exception.WingsException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.http.client.utils.URIBuilder;
import software.wings.app.MainConfiguration;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInvite.UserInviteKeys;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.SignupService;
import software.wings.service.intfc.signup.SignupException;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class SignupServiceImpl implements SignupService {
  @Inject EmailNotificationService emailNotificationService;
  @Inject MainConfiguration configuration;
  @Inject WingsPersistence wingsPersistence;
  @Inject BlackListedDomainChecker blackListedDomainChecker;

  private static final String TRIAL_SIGNUP_COMPLETED_TEMPLATE_NAME = "trial_signup_completed";
  private static final String SETUP_PASSWORD_FOR_SIGNUP = "setup_password_for_signup";

  private List<Character> ILLEGAL_CHARACTERS = Collections.unmodifiableList(Arrays.asList(
      '$', '&', ',', '/', ':', ';', '=', '?', '<', '>', '#', '{', '}', '|', '^', '~', '(', ')', ']', '`', '\'', '\"'));

  public void sendTrialSignupCompletedEmail(UserInvite userInvite) {
    try {
      Map<String, String> templateModel = getTrialSignupCompletedTemplateModel(userInvite);
      sendEmail(userInvite, TRIAL_SIGNUP_COMPLETED_TEMPLATE_NAME, templateModel);
    } catch (URISyntaxException e) {
      logger.error("Trial sign-up completed email couldn't be sent ", e);
    }
  }

  private Map<String, String> getTrialSignupCompletedTemplateModel(UserInvite userInvite) throws URISyntaxException {
    Map<String, String> model = new HashMap<>();
    String loginUrl = buildAbsoluteUrl("/login");
    model.put("name", userInvite.getEmail());
    model.put("url", loginUrl);
    return model;
  }

  private String buildAbsoluteUrl(String fragment) throws URISyntaxException {
    String baseUrl = getBaseUrl();
    URIBuilder uriBuilder = new URIBuilder(baseUrl);
    uriBuilder.setFragment(fragment);
    return uriBuilder.toString();
  }

  private String getBaseUrl() {
    String baseUrl = configuration.getPortal().getUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    return baseUrl;
  }

  public void sendEmail(UserInvite userInvite, String templateName, Map<String, String> templateModel) {
    List<String> toList = new ArrayList<>();
    toList.add(userInvite.getEmail());
    EmailData emailData =
        EmailData.builder().to(toList).templateName(templateName).templateModel(templateModel).build();

    emailData.setCc(Collections.emptyList());
    emailData.setRetries(2);
    emailData.setAccountId(userInvite.getAccountId());

    emailNotificationService.send(emailData);
  }

  public UserInvite getUserInviteByEmail(String email) {
    UserInvite userInvite = null;
    if (isNotEmpty(email)) {
      userInvite = wingsPersistence.createQuery(UserInvite.class).filter(UserInviteKeys.email, email).get();
    }
    return userInvite;
  }

  public void validateEmail(String email) {
    // Only validate if the email address is valid. Won't check if the email has been registered already.
    checkIfEmailIsValid(email);

    if (containsIllegalCharacters(email)) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "The email used for trial registration contains illegal characters.");
    }
    blackListedDomainChecker.check(email);
  }

  public void validateCluster() {
    if (!configuration.isTrialRegistrationAllowed()) {
      throw new SignupException("Signup not allowed in this cluster");
    }
  }

  private boolean containsIllegalCharacters(String email) {
    for (Character illegalChar : ILLEGAL_CHARACTERS) {
      if (email.indexOf(illegalChar) >= 0) {
        return true;
      }
    }
    return false;
  }

  public void checkIfEmailIsValid(String email) {
    if (isBlank(email)) {
      throw new WingsException(INVALID_EMAIL, USER).addParam("email", email);
    }

    final String emailAddress = email.trim();
    if (!EmailValidator.getInstance().isValid(emailAddress)) {
      throw new WingsException(INVALID_EMAIL, USER).addParam("email", emailAddress);
    }
  }

  public void sendPasswordSetupMailForSignup(UserInvite userInvite) {
    String jwtPasswordSecret = configuration.getPortal().getJwtPasswordSecret();
    try {
      String token = createSignupTokeFromSecret(jwtPasswordSecret, userInvite.getEmail(), 30);
      String resetPasswordUrl = getResetPasswordUrl(token);
      sendMail(userInvite, resetPasswordUrl, SETUP_PASSWORD_FOR_SIGNUP);
    } catch (URISyntaxException | UnsupportedEncodingException e) {
      logger.error("Password setup mail for signup could't be sent", e);
    }
  }

  public String createSignupTokeFromSecret(String jwtPasswordSecret, String email, int expireAfterDays)
      throws UnsupportedEncodingException {
    Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
    return JWT.create()
        .withIssuer("Harness Inc")
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + (long) expireAfterDays * 24 * 60 * 60 * 1000)) // 24 hrs
        .withClaim("email", email)
        .sign(algorithm);
  }

  private void sendMail(UserInvite userInvite, String resetPasswordUrl, String resetPasswordTemplateName) {
    Map<String, String> templateModel = new HashMap<>();
    templateModel.put("url", resetPasswordUrl);
    templateModel.put("name", userInvite.getName());
    templateModel.put("companyName", userInvite.getCompanyName());
    List<String> toList = new ArrayList();
    toList.add(userInvite.getEmail());
    EmailData emailData =
        EmailData.builder().to(toList).templateName(resetPasswordTemplateName).templateModel(templateModel).build();
    emailData.setCc(Collections.emptyList());
    emailData.setRetries(2);

    emailNotificationService.send(emailData);
  }

  private String getResetPasswordUrl(String token) throws URISyntaxException {
    // always the call should go to the free cluster because a trial account will be created.
    String mode = "?mode=signup";
    return buildAbsoluteUrl("/complete-signup/" + token + mode);
  }

  public void validatePassword(char[] password) {
    if (isEmpty(password)) {
      throw new WingsException(GENERAL_ERROR, USER).addParam("message", "Empty password has been provided.");
    }

    if (password.length < 8) {
      throw new WingsException(GENERAL_ERROR, USER).addParam("message", "Password should at least be 8 characters");
    }
  }
}
