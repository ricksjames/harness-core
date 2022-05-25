package io.harness.delegate.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

public class DelegateJWTCacheHelper {
  // cache is for storing serialised JWT directly (since agent will be sending the same jwt for 25 mins)
  // cache value is md5 hash of JWT
  private final Cache<String, Boolean> delegateJWTCache =
      Caffeine.newBuilder().maximumSize(50000).expireAfterWrite(15, TimeUnit.MINUTES).build();

  public void setDelegateJWTCache(String tokenHash) {
    delegateJWTCache.put(tokenHash, true);
  }

  public boolean validateDelegateJWTString(String tokenHash) {
    Boolean isPresent = delegateJWTCache.getIfPresent(tokenHash);
    if (Boolean.TRUE.equals(isPresent)) {
      // log.info("Arpit: paas JWT , delegateId {} , jwt {}   ", delegateId, JWTString, new Exception());
      return true;
    }

    delegateJWTCache.invalidate(tokenHash);

    // log.info("Arpit: fail JWT , delegateId {} , jwt {}   ", delegateId, JWTString, new Exception());

    return false;
  }
}
