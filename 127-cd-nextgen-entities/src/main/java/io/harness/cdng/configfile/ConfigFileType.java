/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.configfile;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@OwnedBy(HarnessTeam.CDP)
public enum ConfigFileType {
  LOCAL_FILE("LocalFile"),
  ENCRYPTED("Encrypted"),
  REMOTE("Remote");

  ConfigFileType(String value) {
    this.value = value;
  }

  private final String value;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static ConfigFileType fromString(final String value) {
    for (ConfigFileType type : ConfigFileType.values()) {
      if (type.toString().equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException(String.format("Unrecognized Config File type scope type, value: %s,", value));
  }

  @JsonValue
  @Override
  public String toString() {
    return this.value;
  }
}
