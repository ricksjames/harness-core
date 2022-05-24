package io.harness.delegate.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DelegateJWTCacheHelper {
  // cache is for storing serialised JWT directly (since agent will be sending the same jwt for 25 mins)
  // cache value is md5 hash of JWT
  private final Cache<String, String> delegateJWTCache =
      Caffeine.newBuilder().maximumSize(50000).expireAfterWrite(15, TimeUnit.MINUTES).build();

  public void setDelegateJWTCache(String delegateId, String tokenHash) {
    delegateJWTCache.put(delegateId, tokenHash);
  }

  public boolean validateDelegateJWTString(String delegateId, String JWTString, String tokenHash) {
    String tokenHashFromCache = delegateJWTCache.getIfPresent(delegateId);
    if (tokenHash.equals(tokenHashFromCache)) {
      log.info("Arpit: paas JWT {}   ", JWTString, new Exception());
      return true;
    }

    // call invalidate method only of there is a miss.
    if (tokenHashFromCache != null) {
      delegateJWTCache.invalidate(delegateId);
    }
    log.info("Arpit: fail JWT {}   ", JWTString, new Exception());

    return false;
  }
}
