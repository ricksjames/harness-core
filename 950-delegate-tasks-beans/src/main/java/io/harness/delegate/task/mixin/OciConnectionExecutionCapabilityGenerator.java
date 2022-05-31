/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.mixin;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.KeyValuePair;
import io.harness.delegate.beans.executioncapability.OciConnectionExecutionCapability;
import io.harness.delegate.task.utils.KmsUtils;
import io.harness.expression.DummySubstitutor;
import io.harness.expression.ExpressionEvaluator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class OciConnectionExecutionCapabilityGenerator {
  public static OciConnectionExecutionCapability buildOciConnectionExecutionCapability(
      String urlString, ExpressionEvaluator maskingEvaluator) {
    return buildOciConnectionExecutionCapability(urlString, OciCapabilityDetailsLevel.PATH, maskingEvaluator);
  }

  public static OciConnectionExecutionCapability buildOciConnectionExecutionCapabilityForKms(
      String region, ExpressionEvaluator maskingEvaluator) {
    String kmsUrl = KmsUtils.generateKmsUrl(region);
    return buildOciConnectionExecutionCapability(kmsUrl, maskingEvaluator);
  }

  public static OciConnectionExecutionCapability buildOciConnectionExecutionCapability(
      String urlString, OciCapabilityDetailsLevel level, ExpressionEvaluator maskingEvaluator) {
    try {
      URI uri = new URI(DummySubstitutor.substitute(urlString));

      if (isNotBlank(uri.getScheme()) && isNotBlank(uri.getHost())) {
        OciConnectionExecutionCapability ociConnectionExecutionCapability =
            level.getOciConnectionExecutionCapability(urlString);
        if (!ociConnectionExecutionCapability.fetchCapabilityBasis().contains(DummySubstitutor.DUMMY_UUID)) {
          return ociConnectionExecutionCapability;
        }
      }
    } catch (Exception e) {
      log.error("conversion to java.net.URI failed for url: {}", maskedUrlString(maskingEvaluator, urlString), e);
    }
    // This is falling back to existing approach, where we test for entire URL
    return OciConnectionExecutionCapability.builder().url(maskedUrlString(maskingEvaluator, urlString)).build();
  }

  /***
   * Build HTTP Execution capability with headers. We will check for headers while doing capability check on the
   * delegate
   * @param urlString
   * @param headers
   * @param level
   * @param maskingEvaluator
   * @return
   */
  public static OciConnectionExecutionCapability buildOciConnectionExecutionCapability(String urlString,
      List<KeyValuePair> headers, OciCapabilityDetailsLevel level, ExpressionEvaluator maskingEvaluator) {
    try {
      URI uri = new URI(DummySubstitutor.substitute(urlString));

      if (isNotBlank(uri.getScheme()) && isNotBlank(uri.getHost())) {
        OciConnectionExecutionCapability ociConnectionExecutionCapability =
            level.getOciConnectionExecutionCapabilityWithMaskedHeaders(urlString, headers, maskingEvaluator);
        if (!ociConnectionExecutionCapability.fetchCapabilityBasis().contains(DummySubstitutor.DUMMY_UUID)) {
          return ociConnectionExecutionCapability;
        }
      }
    } catch (Exception e) {
      log.error("conversion to java.net.URI failed for url: {}", maskedUrlString(maskingEvaluator, urlString), e);
    }
    // This is falling back to existing approach, where we test for entire URL
    return OciConnectionExecutionCapability.builder()
        .url(maskedUrlString(maskingEvaluator, urlString))
        .headers(maskedHeaders(maskingEvaluator, headers))
        .build();
  }

  private static String maskedUrlString(ExpressionEvaluator maskingEvaluator, String urlString) {
    if (maskingEvaluator == null) {
      return urlString;
    }
    return maskingEvaluator.substitute(urlString, Collections.emptyMap());
  }

  private static List<KeyValuePair> maskedHeaders(ExpressionEvaluator maskingEvaluator, List<KeyValuePair> headers) {
    if (maskingEvaluator == null || headers == null) {
      return headers;
    }
    return headers.stream()
        .map(entry
            -> KeyValuePair.builder()
                   .key(maskingEvaluator.substitute(entry.getKey(), Collections.emptyMap()))
                   .value(maskingEvaluator.substitute(entry.getValue(), Collections.emptyMap()))
                   .build())
        .collect(Collectors.toList());
  }

  public enum OciCapabilityDetailsLevel {
    DOMAIN(false, false),
    PATH(true, false),
    QUERY(true, true);
    private boolean usePath, useQuery;

    OciCapabilityDetailsLevel(boolean usePath, boolean useQuery) {
      this.usePath = usePath;
      this.useQuery = useQuery;
    }

    private OciConnectionExecutionCapability getOciConnectionExecutionCapability(String urlString)
        throws URISyntaxException {
      URI uri = new URI(DummySubstitutor.substitute(urlString));
      return OciConnectionExecutionCapability.builder()
          .scheme(uri.getScheme())
          .host(uri.getHost())
          .port(uri.getPort())
          .path(usePath ? getPath(uri) : null)
          .query(useQuery ? uri.getQuery() : null)
          .build();
    }

    private OciConnectionExecutionCapability getOciConnectionExecutionCapabilityWithMaskedHeaders(
        String urlString, List<KeyValuePair> headers, ExpressionEvaluator maskingEvaluator) throws URISyntaxException {
      URI uri = new URI(DummySubstitutor.substitute(urlString));
      return OciConnectionExecutionCapability.builder()
          .headers(maskedHeaders(maskingEvaluator, headers))
          .scheme(uri.getScheme())
          .host(uri.getHost())
          .port(uri.getPort())
          .path(usePath ? getPath(uri) : null)
          .query(useQuery ? uri.getQuery() : null)
          .build();
    }

    private static String getPath(URI uri) {
      if (isBlank(uri.getPath())) {
        return null;
      }
      return uri.getPath().substring(1);
    }
  }
}
