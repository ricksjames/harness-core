/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.yaml.extended;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TIDotNetVersion {
  @JsonProperty("5.0") FIVEPOINTZERO("5.0"),
  @JsonProperty("6.0") SIXPOINTZERO("6.0");

  private final String yamlName;

  TIDotNetVersion(String yamlName) {
    this.yamlName = yamlName;
  }

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static TIDotNetVersion getDotNetVersion(@JsonProperty("frameworkVersion") String yamlName) {
    for (TIDotNetVersion dotNetVersion : TIDotNetVersion.values()) {
      if (dotNetVersion.yamlName.equalsIgnoreCase(yamlName)) {
        return dotNetVersion;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + yamlName);
  }

  @JsonValue
  public String getYamlName() {
    return yamlName;
  }

  @Override
  public String toString() {
    return yamlName;
  }

  public static TIDotNetVersion fromString(final String s) {
    return TIDotNetVersion.getDotNetVersion(s);
  }
}
