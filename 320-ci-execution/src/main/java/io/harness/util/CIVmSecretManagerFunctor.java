package io.harness.util;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionFunctor;
import io.harness.ng.core.NGAccess;

import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIVmSecretManagerFunctor implements ExpressionFunctor {
  private long expressionFunctorToken;
  private NGAccess ngAccess;

  @Builder.Default private Set<String> secrets = new HashSet<>();

  public Set<String> getSecrets() {
    return secrets;
  }

  public Object obtain(String secretIdentifier, int token) {
    String expr = "${ngSecretManager.obtain(\"" + secretIdentifier + "\", " + expressionFunctorToken + ")}";
    secrets.add(expr);
    return expr;
  }
}
