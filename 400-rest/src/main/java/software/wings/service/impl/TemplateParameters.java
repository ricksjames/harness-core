package software.wings.service.impl;

import io.harness.delegate.beans.K8sPermissionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TemplateParameters {
  private final String delegateXmx;
  private final String accountId;
  private final String version;
  private final String managerHost;
  private final String verificationHost;
  private final String delegateName;
  private final String delegateProfile;
  private final String delegateGroupId;
  private final String delegateType;
  private final boolean ceEnabled;
  private final boolean ciEnabled;
  private final String logStreamingServiceBaseUrl;
  private final String delegateOrgIdentifier;
  private final String delegateProjectIdentifier;
  private final String delegateDescription;
  private final String delegateSize;
  private final int delegateTaskLimit;
  private final int delegateReplicas;
  private final int delegateRam;
  private final double delegateCpu;
  private final int delegateRequestsRam;
  private final double delegateRequestsCpu;
  private final String delegateNamespace;
  private final String delegateTokenName;
  private final String delegateTags;
  private final K8sPermissionType k8sPermissionsType;
}
