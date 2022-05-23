package io.harness.delegate.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

@Slf4j
public class DelegateJWTCacheHelper {
  // cache is for storing serialised JWT directly (since agent will be sending the same jwt for 25 mins)
  // cache key is md5 hash of JWT
  private final Cache<String, String> delegateJWTCache =
      Caffeine.newBuilder().maximumSize(50000).expireAfterWrite(15, TimeUnit.MINUTES).build();

  public void setDelegateJWTCache(String JWTString) {
    String cacheKey = getCacheKey(JWTString);
    delegateJWTCache.put(cacheKey, JWTString);
  }

  public boolean validateDelegateJWTString(String JWTString) {
    String cacheKey = getCacheKey(JWTString);
    String JWTStringFromCache = delegateJWTCache.getIfPresent(cacheKey);
    if (JWTString.equals(JWTStringFromCache)) {
      log.info("Arpit: paas JWT {}   ", JWTString, new Exception());
      return true;
    }

    // call invalidate method only of there is a miss.
    if (JWTStringFromCache != null) {
      delegateJWTCache.invalidate(cacheKey);
    }
    log.info("Arpit: fail JWT {}   ", JWTString, new Exception());

    return false;
  }

  private String getCacheKey(String JWTString) {
    return DigestUtils.md5Hex(JWTString);
  }
}
