/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("serverlessManifestOutcome")
@JsonTypeName(ManifestType.ServerlessYaml)
@OwnedBy(CDP)
@RecasterAlias("io.harness.cdng.manifest.yaml.ServerlessManifestOutcome")
public class ServerlessManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.ServerlessYaml;
  StoreConfig store;
  int order;
}
