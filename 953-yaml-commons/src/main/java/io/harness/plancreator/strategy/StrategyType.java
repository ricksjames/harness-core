package io.harness.plancreator.strategy;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum StrategyType {
  FOR("for"),
  MATRIX("matrix");

  String displayName;

  StrategyType(String displayName) {
    this.displayName = displayName;
  }
}
