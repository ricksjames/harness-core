/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.eraro.ErrorCode.EXPIRED_TOKEN;
import static io.harness.exception.WingsException.USER_ADMIN;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.RevokedTokenException;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

// TODO: ARPIT records cache hit and miss metrics using OpenCensus
@OwnedBy(DEL)
public class DelegateJWTCacheHelper {
  // cache is for storing serialised JWT directly (since agent will be sending the same jwt for 25 mins)
  // cache value is md5 hash of JWT
  private final Cache<String, DelegateJWTCacheValueObject> delegateJWTCache =
      Caffeine.newBuilder().maximumSize(50000).expireAfterWrite(15, TimeUnit.MINUTES).build();

  // setting validUntil doesnt makes sense, if token is not valid
  public void setDelegateJWTCache(String tokenHash, Boolean isValid, long validUntil) {
    delegateJWTCache.put(tokenHash, new DelegateJWTCacheValueObject(isValid, validUntil));
  }

  public boolean validateDelegateJWTString(String tokenHash) {
    DelegateJWTCacheValueObject delegateJWTCacheValueObject = delegateJWTCache.getIfPresent(tokenHash);

    // cache miss
    if (delegateJWTCacheValueObject == null) {
      return false;
    } else if (!delegateJWTCacheValueObject.isValid()) {
      // Not great, but, if token is not valid, then always send RevokedTokenException to freeze delegate. Don't send
      // InvalidRequestException
      throw new RevokedTokenException("Invalid delegate token. Delegate is using invalid token", USER_ADMIN);
    } else if (delegateJWTCacheValueObject.getValidUntil() < System.currentTimeMillis()) {
      throw new InvalidRequestException("Unauthorized", EXPIRED_TOKEN, null);
    }

    return true;
  }
}
