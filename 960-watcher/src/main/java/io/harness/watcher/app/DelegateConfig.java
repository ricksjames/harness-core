package io.harness.watcher.app;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@lombok.Getter
@lombok.Setter
@lombok.ToString
public class DelegateConfig {
  public void init() {
    isNg = isNotBlank(System.getenv().get("DELEGATE_SESSION_IDENTIFIER"))
        || (isNotBlank(System.getenv().get("NEXT_GEN")) && Boolean.parseBoolean(System.getenv().get("NEXT_GEN")));
    delegateName = isNotBlank(System.getenv().get("DELEGATE_NAME")) ? System.getenv().get("DELEGATE_NAME") : "";
    final String deployMode = System.getenv().get("DEPLOY_MODE");
    multiversion = isEmpty(deployMode) || !deployMode.equals("KUBERNETES_ONPREM");
  }

  private boolean isNg;
  private boolean multiversion;
  private String delegateName;
  private String delegateId;
  private String watcherJreVersion = System.getProperty("java.version");
  private String version;
}
