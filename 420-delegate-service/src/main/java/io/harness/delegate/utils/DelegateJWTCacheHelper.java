/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

// TODO: ARPIT record cache hit and miss metrics using OpenCensus
@OwnedBy(DEL)
public class DelegateJWTCacheHelper {
  private final Cache<String, DelegateJWTCacheValueObject> delegateJWTCache =
      Caffeine.newBuilder().maximumSize(50000).expireAfterWrite(15, TimeUnit.MINUTES).build();

  public void setDelegateJWTCache(String tokenHash, Boolean isValid, long expiryInMillis) {
    delegateJWTCache.put(tokenHash, new DelegateJWTCacheValueObject(isValid, expiryInMillis));
  }

  public DelegateJWTCacheValueObject getDelegateJWTCache(String cacheKey) {
    return delegateJWTCache.getIfPresent(cacheKey);
  }
}
