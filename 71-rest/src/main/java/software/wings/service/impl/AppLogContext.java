package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class AppLogContext extends AutoLogContext {
  public static final String ID = "appId";

  public AppLogContext(String appId, OverrideBehavior behavior) {
    super(ID, appId, behavior);
  }
}
