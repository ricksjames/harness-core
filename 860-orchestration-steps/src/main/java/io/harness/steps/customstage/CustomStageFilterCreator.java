package io.harness.steps.customstage;

import io.harness.filters.GenericStageFilterJsonCreatorV2;
import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.StepSpecTypeConstants;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomStageFilterCreator extends GenericStageFilterJsonCreatorV2<CustomStageNode> {
  @Override
  public Set<String> getSupportedStageTypes() {
    return Collections.singleton(StepSpecTypeConstants.CUSTOM_STAGE);
  }

  @Override
  public PipelineFilter getFilter(FilterCreationContext filterCreationContext, CustomStageNode stageNode) {
    // No filter required for custom stage, will be shown in all modules CI, CD etc.
    return null;
  }

  @Override
  public Class<CustomStageNode> getFieldClass() {
    return CustomStageNode.class;
  }

  @Override
  protected Map<String, YamlField> getDependencies(YamlField stageField) {
    YamlField specYamlField = stageField.getNode().getField(YAMLFieldNameConstants.SPEC);
    if (specYamlField == null) {
      return null;
    }
    YamlField stepsYamlField = specYamlField.getNode().getField(YAMLFieldNameConstants.STEPS);
    if (stepsYamlField == null) {
      return null;
    }
    List<YamlNode> yamlNodes = Optional.of(stepsYamlField.getNode().asArray()).orElse(Collections.emptyList());
    List<YamlField> stepYamlFields = PlanCreatorUtils.getStepYamlFields(yamlNodes);
    return stepYamlFields.stream().collect(Collectors.toMap(field -> field.getNode().getUuid(), field -> field));
  }
}
