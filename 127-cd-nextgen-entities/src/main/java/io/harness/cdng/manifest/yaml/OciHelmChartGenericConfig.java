/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.cdng.manifest.yaml.ociHelmChartConfig.OciConfigType.GENERIC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.cdng.manifest.yaml.ociHelmChartConfig.OciHelmChartConfig;
import io.harness.filters.ConnectorRefExtractorHelper;
import io.harness.filters.WithConnectorRef;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(GENERIC)
@SimpleVisitorHelper(helperClass = ConnectorRefExtractorHelper.class)
@TypeAlias("OciGenericConfig")
@RecasterAlias("io/harness/cdng/manifest/yaml/OciHelmChartGenericConfig.java")
public class OciHelmChartGenericConfig implements OciHelmChartConfig, Visitable, WithConnectorRef {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> connectorRef;

  @Override
  public String getKind() {
    return GENERIC;
  }

  @Override
  public OciHelmChartConfig cloneInternal() {
    return OciHelmChartGenericConfig.builder().connectorRef(connectorRef).build();
  }

  @Override
  public ParameterField<String> getConnectorReference() {
    return connectorRef;
  }

  @Override
  public OciHelmChartConfig applyOverrides(OciHelmChartConfig overrideConfig) {
    OciHelmChartGenericConfig ociHelmChartGenericConfig = (OciHelmChartGenericConfig) overrideConfig;
    OciHelmChartGenericConfig resultantHelmOciHelmChart = this;
    if (!ParameterField.isNull(ociHelmChartGenericConfig.getConnectorReference())) {
      resultantHelmOciHelmChart =
          resultantHelmOciHelmChart.withConnectorRef(ociHelmChartGenericConfig.getConnectorReference());
    }
    return resultantHelmOciHelmChart;
  }

  @Override
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
    connectorRefMap.put(YAMLFieldNameConstants.CONNECTOR_REF, connectorRef);
    return connectorRefMap;
  }
}
