/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.execution.ExecutionInputService;
import io.harness.execution.ExecutionInputInstance;
import io.harness.expression.LateBindingValue;
import io.harness.jackson.JsonNodeUtils;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class ExecutionInputExpressionFunctor implements LateBindingValue {
  private final Ambiance ambiance;
  private final ExecutionInputService executionInputService;

  public ExecutionInputExpressionFunctor(ExecutionInputService executionInputService, Ambiance ambiance) {
    this.executionInputService = executionInputService;
    this.ambiance = ambiance;
  }

  @Override
  public Object bind() {
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    ExecutionInputInstance inputInstance = executionInputService.getExecutionInputInstance(nodeExecutionId);

    ObjectMapper objectMapper = new YAMLMapper();
    JsonNode jsonNode;
    JsonNode templateNode;
    try {
      jsonNode = objectMapper.readTree(inputInstance.getUserInput());
      templateNode = objectMapper.readTree(inputInstance.getTemplate());
      // Merging user input with template.
      JsonNodeUtils.merge(templateNode, jsonNode);
    } catch (JsonProcessingException e) {
      log.warn(
          "Could not covert the execution user input to Map. Expressions might not be resolved for nodeExecutionId {}",
          nodeExecutionId, e);
      return Collections.emptyMap();
    }
    return RecastOrchestrationUtils.fromJson(templateNode.toString());
  }
}
