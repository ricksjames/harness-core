/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.configfile;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper.StoreConfigWrapperParameters;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.visitor.helpers.configfile.ConfigFileAttributesVisitorHelper;
import io.harness.common.ParameterFieldHelper;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.SkipAutoEvaluation;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.intfc.OverridesApplier;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ConfigFileAttributesKeys")
@SimpleVisitorHelper(helperClass = ConfigFileAttributesVisitorHelper.class)
@TypeAlias("configFileAttributes")
@OwnedBy(CDP)
@RecasterAlias("io.harness.cdng.configfile.ConfigFileAttributes")
public class ConfigFileAttributes implements OverridesApplier<ConfigFileAttributes>, Visitable {
  @Wither
  @ApiModelProperty(dataType = "io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper")
  @JsonProperty("store")
  @SkipAutoEvaluation
  ParameterField<StoreConfigWrapper> store;

  @Wither
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
  @JsonProperty("hostDestination")
  private ParameterField<String> hostDestination;

  @Wither
  @ApiModelProperty(dataType = "io.harness.cdng.configfile.ConfigFileType")
  @JsonProperty("fileType")
  ParameterField<ConfigFileType> type;

  @Override
  public ConfigFileAttributes applyOverrides(ConfigFileAttributes overrideConfig) {
    ConfigFileAttributes configFileAttributes = this;

    if (overrideConfig.getStore() != null && overrideConfig.getStore().getValue() != null) {
      configFileAttributes = configFileAttributes.withStore(
          ParameterField.createValueField(this.store.getValue().applyOverrides(overrideConfig.getStore().getValue())));
    }

    if (overrideConfig.getHostDestination() != null) {
      configFileAttributes = configFileAttributes.withHostDestination(overrideConfig.getHostDestination());
    }

    if (overrideConfig.getType() != null) {
      configFileAttributes = configFileAttributes.withType(overrideConfig.getType());
    }

    return configFileAttributes;
  }

  // For Visitor Framework Impl
  String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add(YAMLFieldNameConstants.STORE, store.getValue());
    return children;
  }

  public ConfigFileAttributeStepParameters getConfigFileAttributeStepParameters() {
    return new ConfigFileAttributeStepParameters(StoreConfigWrapperParameters.fromStoreConfigWrapper(store.getValue()),
        ParameterFieldHelper.getParameterFieldValue(type),
        ParameterFieldHelper.getParameterFieldValue(hostDestination));
  }

  @Value
  public static class ConfigFileAttributeStepParameters {
    StoreConfigWrapperParameters store;
    ConfigFileType type;
    String hostDestination;
  }
}
