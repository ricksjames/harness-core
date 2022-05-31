package io.harness.steps.matrix;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.NGCommonUtilitiesTestBase;
import io.harness.category.element.UnitTests;
import io.harness.plancreator.strategy.StrategyConfig;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ForLoopStrategyConfigServiceTest extends NGCommonUtilitiesTestBase {
  private ForLoopStrategyConfigService forLoopStrategyConfigService = new ForLoopStrategyConfigService();

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testFetchChildren() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipeline-with-strategy.yaml");
    assertThat(testFile).isNotNull();
    String pipelineYaml = Resources.toString(testFile, Charsets.UTF_8);
    String pipelineYamlWithUuid = YamlUtils.injectUuid(pipelineYaml);
    YamlField pipelineYamlField = YamlUtils.readTree(pipelineYamlWithUuid).getNode().getField("pipeline");
    assertThat(pipelineYamlField).isNotNull();
    YamlField stagesYamlField = pipelineYamlField.getNode().getField("stages");
    assertThat(stagesYamlField).isNotNull();
    List<YamlNode> stageYamlNodes = stagesYamlField.getNode().asArray();

    YamlField approvalStageYamlField = stageYamlNodes.get(2).getField("stage");

    YamlField strategyField = approvalStageYamlField.getNode().getField("strategy");
    StrategyConfig strategyConfig = YamlUtils.read(strategyField.getNode().toString(), StrategyConfig.class);

    List<ChildrenExecutableResponse.Child> children =
        forLoopStrategyConfigService.fetchChildren(strategyConfig, "childNodeId");
    assertThat(children.size()).isEqualTo(10);
  }
}