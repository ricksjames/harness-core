package software.wings.beans;

import static software.wings.utils.CryptoUtil.secureRandAlphaNumString;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by anubhaw on 6/17/16.
 */
@Entity(value = "emailVerificationTokens")
public class EmailVerificationToken extends Base {
  @NotEmpty private String token;
  @NotEmpty private String userId;

  /**
   * Instantiates a new Email verification token.
   */
  public EmailVerificationToken() {}

  /**
   * Instantiates a new Email verification token.
   *
   * @param userId the user id
   */
  public EmailVerificationToken(String userId) {
    setAppId(Base.GLOBAL_APP_ID);
    this.userId = userId;
    this.token = secureRandAlphaNumString(32);
  }

  /**
   * Gets token.
   *
   * @return the token
   */
  public String getToken() {
    return token;
  }

  /**
   * Sets token.
   *
   * @param token the token
   */
  public void setToken(String token) {
    this.token = token;
  }

  /**
   * Gets user id.
   *
   * @return the user id
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Sets user id.
   *
   * @param userId the user id
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String token;
    private String userId;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * An email verification token builder.
     *
     * @return the builder
     */
    public static Builder anEmailVerificationToken() {
      return new Builder();
    }

    /**
     * With token builder.
     *
     * @param token the token
     * @return the builder
     */
    public Builder withToken(String token) {
      this.token = token;
      return this;
    }

    /**
     * With user id builder.
     *
     * @param userId the user id
     * @return the builder
     */
    public Builder withUserId(String userId) {
      this.userId = userId;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active builder.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anEmailVerificationToken()
          .withToken(token)
          .withUserId(userId)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build email verification token.
     *
     * @return the email verification token
     */
    public EmailVerificationToken build() {
      EmailVerificationToken emailVerificationToken = new EmailVerificationToken();
      emailVerificationToken.setToken(token);
      emailVerificationToken.setUserId(userId);
      emailVerificationToken.setUuid(uuid);
      emailVerificationToken.setAppId(appId);
      emailVerificationToken.setCreatedBy(createdBy);
      emailVerificationToken.setCreatedAt(createdAt);
      emailVerificationToken.setLastUpdatedBy(lastUpdatedBy);
      emailVerificationToken.setLastUpdatedAt(lastUpdatedAt);
      emailVerificationToken.setActive(active);
      return emailVerificationToken;
    }
  }
}
