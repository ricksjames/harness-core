package software.wings.filter;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.network.Localhost.getLocalHostAddress;
import static io.harness.network.Localhost.getLocalHostName;

import static java.util.Arrays.asList;

import io.harness.delegate.beans.FileBucket;
import io.harness.exception.WingsException;
import io.harness.security.annotations.DelegateAuth;
import io.harness.security.annotations.LearningEngineAuth;
import io.harness.stream.BoundedInputStream;

import software.wings.app.MainConfiguration;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.audit.AuditSkip;
import software.wings.beans.HttpMethod;
import software.wings.common.AuditHelper;
import software.wings.service.intfc.FileService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

/**
 * AuditRequestFilter preserves the rest endpoint header and payload.
 *
 * @author Rishi
 */
@Singleton
@Provider
@Priority(1500) // Authorization > Audit > Authentication
@Slf4j
public class AuditRequestFilter implements ContainerRequestFilter {
  private static final String FILE_CONTENT_NOT_STORED = "__FILE_CONTENT_NOT_STORED__";

  @Context private ResourceContext resourceContext;
  @Context private ResourceInfo resourceInfo;

  @Inject private AuditHelper auditHelper;
  @Inject private FileService fileService;
  @Inject private MainConfiguration configuration;

  /* (non-Javadoc)
   * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
   */
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    if ((isAuditExemptedHttpMethod(requestContext) && isAllowWhitelistedIP(requestContext))
        || isAuditExemptedResource()) {
      // do not audit idempotent HttpMethod until we have finer control auditing.
      // audit non-whitelisted IP login
      return;
    }

    AuditHeader header = new AuditHeader();
    String url = requestContext.getUriInfo().getAbsolutePath().toString();
    header.setUrl(url);

    String headerString = getHeaderString(requestContext.getHeaders());
    header.setHeaderString(headerString);

    String query = getQueryParams(requestContext.getUriInfo().getQueryParameters());
    header.setQueryParams(query);

    HttpMethod method = HttpMethod.valueOf(requestContext.getMethod());
    header.setRequestMethod(method);
    header.setResourcePath(requestContext.getUriInfo().getPath());
    header.setRequestTime(System.currentTimeMillis());

    HttpServletRequest request = resourceContext.getResource(HttpServletRequest.class);

    header.setRemoteHostName(request.getRemoteHost());
    header.setRemoteIpAddress(request.getRemoteAddr());
    header.setRemoteHostPort(request.getRemotePort());
    header.setLocalHostName(getLocalHostName());
    header.setLocalIpAddress(getLocalHostAddress());

    header = auditHelper.create(header);

    if (!configuration.getAuditConfig().isStoreRequestPayload()) {
      return;
    }

    try {
      if (headerString.contains("multipart/form-data")) {
        // don't store file content in audit logs
        auditHelper.create(
            header, RequestType.REQUEST, IOUtils.toInputStream(FILE_CONTENT_NOT_STORED, Charset.defaultCharset()));
      } else {
        try (BoundedInputStream inputStream = new BoundedInputStream(
                 requestContext.getEntityStream(), configuration.getFileUploadLimits().getAppContainerLimit())) {
          String fileId = auditHelper.create(header, RequestType.REQUEST, inputStream);
          requestContext.setEntityStream(fileService.openDownloadStream(fileId, FileBucket.AUDITS));
        }
      }
    } catch (Exception exception) {
      throw new WingsException(exception);
    }
  }

  private boolean isAuditExemptedResource() {
    return resourceInfo.getResourceMethod().getAnnotation(DelegateAuth.class) != null
        || resourceInfo.getResourceMethod().getAnnotation(LearningEngineAuth.class) != null
        || resourceInfo.getResourceMethod().getAnnotation(AuditSkip.class) != null
        || resourceInfo.getResourceClass().getAnnotation(AuditSkip.class) != null;
  }

  private boolean isAllowWhitelistedIP(ContainerRequestContext requestContext) {
    return !requestContext.getUriInfo().getPath().contains("whitelist/isEnabled");
  }
  private boolean isAuditExemptedHttpMethod(ContainerRequestContext requestContext) {
    return asList(HttpMethod.GET.name(), HttpMethod.OPTIONS.name(), HttpMethod.HEAD.name())
        .contains(requestContext.getMethod());
  }

  private String getHeaderString(MultivaluedMap<String, String> headers) {
    if (isEmpty(headers)) {
      return "";
    }

    StringBuilder headerString = new StringBuilder();
    for (Entry<String, List<String>> entry : headers.entrySet()) {
      String key = entry.getKey();
      headerString.append(key).append('=');
      for (String value : entry.getValue()) {
        headerString.append(';');
        headerString.append(key.equalsIgnoreCase("Authorization") ? "********" : value);
      }
      headerString.substring(1);
      headerString.append(',');
    }
    String headerStr = headerString.toString();
    if (headerStr.length() > 0) {
      headerStr = headerStr.substring(0, headerStr.length() - 1);
    }
    return headerStr;
  }

  private String getQueryParams(MultivaluedMap<String, String> queryParameters) {
    StringBuilder queryParams = new StringBuilder();
    for (Entry<String, List<String>> entry : queryParameters.entrySet()) {
      String key = entry.getKey();
      StringBuilder temp = new StringBuilder();
      for (String value : queryParameters.get(key)) {
        temp.append('&').append(key).append('=').append(value);
      }
      queryParams.append('&').append(temp.substring(1));
    }
    if (isEmpty(queryParams.toString())) {
      return null;
    } else {
      return queryParams.substring(1);
    }
  }
}
