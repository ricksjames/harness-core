package io.harness.cdng.common.step;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.MLUKIC;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.refobjects.RefType;
import io.harness.pms.data.OrchestrationRefType;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.rule.Owner;
import io.harness.steps.OutputExpressionConstants;
import io.harness.steps.StepHelper;
import io.harness.steps.environment.EnvironmentOutcome;
import io.harness.telemetry.TelemetryReporter;

import java.util.Map;
import org.joda.time.DateTimeUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(CDP)
public class StepHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @InjectMocks private StepHelper stepHelper;
  @Mock private TelemetryReporter telemetryReporter;
  private final Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", "test-account").build();

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetProdEnvType() {
    EnvironmentOutcome environmentOutcome = EnvironmentOutcome.builder().type(EnvironmentType.Production).build();

    RefObject envRef = RefObject.newBuilder()
                           .setName(OutputExpressionConstants.ENVIRONMENT)
                           .setKey(OutputExpressionConstants.ENVIRONMENT)
                           .setRefType(RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build())
                           .build();

    OptionalSweepingOutput optionalSweepingOutput =
        OptionalSweepingOutput.builder().found(true).output(environmentOutcome).build();

    doReturn(optionalSweepingOutput).when(executionSweepingOutputResolver).resolveOptional(ambiance, envRef);
    io.harness.beans.EnvironmentType env = stepHelper.getEnvironmentType(ambiance);
    assertThat(env).isNotNull();
    assertThat(env).isEqualTo(io.harness.beans.EnvironmentType.PROD);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetNonProdEnvType() {
    EnvironmentOutcome environmentOutcome = EnvironmentOutcome.builder().type(EnvironmentType.PreProduction).build();

    RefObject envRef = RefObject.newBuilder()
                           .setName(OutputExpressionConstants.ENVIRONMENT)
                           .setKey(OutputExpressionConstants.ENVIRONMENT)
                           .setRefType(RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build())
                           .build();

    OptionalSweepingOutput optionalSweepingOutput =
        OptionalSweepingOutput.builder().found(true).output(environmentOutcome).build();

    doReturn(optionalSweepingOutput).when(executionSweepingOutputResolver).resolveOptional(ambiance, envRef);
    io.harness.beans.EnvironmentType env = stepHelper.getEnvironmentType(ambiance);
    assertThat(env).isNotNull();
    assertThat(env).isEqualTo(io.harness.beans.EnvironmentType.NON_PROD);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testShouldGetNullEnvType() {
    EnvironmentOutcome environmentOutcome = EnvironmentOutcome.builder().build();

    RefObject envRef = RefObject.newBuilder()
                           .setName(OutputExpressionConstants.ENVIRONMENT)
                           .setKey(OutputExpressionConstants.ENVIRONMENT)
                           .setRefType(RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build())
                           .build();

    OptionalSweepingOutput optionalSweepingOutput =
        OptionalSweepingOutput.builder().found(true).output(environmentOutcome).build();

    doReturn(optionalSweepingOutput).when(executionSweepingOutputResolver).resolveOptional(ambiance, envRef);
    io.harness.beans.EnvironmentType env = stepHelper.getEnvironmentType(ambiance);
    assertThat(env).isNotNull();
    assertThat(env).isEqualTo(io.harness.beans.EnvironmentType.ALL);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetEnvTypeForNullEnvOutcome() {
    RefObject envRef = RefObject.newBuilder()
                           .setName(OutputExpressionConstants.ENVIRONMENT)
                           .setKey(OutputExpressionConstants.ENVIRONMENT)
                           .setRefType(RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build())
                           .build();

    OptionalSweepingOutput optionalSweepingOutput = OptionalSweepingOutput.builder().found(false).build();

    doReturn(optionalSweepingOutput).when(executionSweepingOutputResolver).resolveOptional(ambiance, envRef);
    io.harness.beans.EnvironmentType env = stepHelper.getEnvironmentType(ambiance);
    assertThat(env).isNotNull();
    assertThat(env).isEqualTo(io.harness.beans.EnvironmentType.ALL);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testSendRollbackTelemetryEventWithFailedStatus() throws Exception {
    testSendRollbackTelemetryEvent(Status.FAILED, Status.FAILED);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testSendRollbackTelemetryEventWithSucceededStatus() throws Exception {
    testSendRollbackTelemetryEvent(Status.SUCCEEDED, Status.SUCCEEDED);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testSendRollbackTelemetryEventWithAbortedStatus() throws Exception {
    testSendRollbackTelemetryEvent(Status.ABORTED, Status.ABORTED);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testSendRollbackTelemetryEventWithExpiredStatus() throws Exception {
    testSendRollbackTelemetryEvent(Status.EXPIRED, Status.EXPIRED);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testSendRollbackTelemetryEventWithEmptyAmbiance() throws Exception {
    testSendRollbackTelemetryEventWithInvalidParams(Ambiance.newBuilder().build(), Status.FAILED);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testSendRollbackTelemetryEventWithInvalidStatus() throws Exception {
    testSendRollbackTelemetryEventWithInvalidParams(Ambiance.newBuilder().build(), null);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testSendRollbackTelemetryEventWithInvalidAmbianceAndStatus() throws Exception {
    testSendRollbackTelemetryEventWithInvalidParams(null, null);
  }

  private void testSendRollbackTelemetryEvent(Status statusToSend, Status StatusToCheck) {
    Level level = Level.newBuilder().setGroup(StepOutcomeGroup.STEP.name()).setIdentifier("TestRollbackStep").build();
    Ambiance ambiance = Ambiance.newBuilder().setStartTs(DateTimeUtils.currentTimeMillis()).addLevels(level).build();
    Map<String, Object> props = stepHelper.sendRollbackTelemetryEvent(ambiance, statusToSend);

    assertNotNull(props);

    assertEquals(StepOutcomeGroup.STEP.name(), props.get(StepHelper.TELEMETRY_ROLLBACK_PROP_LEVEL));
    assertEquals("TestRollbackStep", props.get(StepHelper.TELEMETRY_ROLLBACK_PROP_NAME));
    assertEquals(String.valueOf(StatusToCheck), props.get(StepHelper.TELEMETRY_ROLLBACK_PROP_STATUS));

    assertThat(props).hasSize(3);
  }

  private void testSendRollbackTelemetryEventWithInvalidParams(Ambiance ambiance, Status status) {
    Map<String, Object> props = stepHelper.sendRollbackTelemetryEvent(ambiance, status);

    assertNull(props);
  }
}
