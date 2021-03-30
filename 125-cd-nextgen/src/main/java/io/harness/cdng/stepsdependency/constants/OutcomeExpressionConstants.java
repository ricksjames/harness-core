package io.harness.cdng.stepsdependency.constants;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class OutcomeExpressionConstants {
  public final String SERVICE = "service";
  public final String INFRASTRUCTURE = "infrastructure";
  public final String ARTIFACTS = "ARTIFACTS";
  public final String K8S_ROLL_OUT = "rollingOutcome";
  public final String K8S_BLUE_GREEN_OUTCOME = "k8sBlueGreenOutcome";
  public final String K8S_APPLY_OUTCOME = "k8sApplyOutcome";
  public final String K8S_CANARY_OUTCOME = "k8sCanaryOutcome";
  public final String K8S_CANARY_DELETE_OUTCOME = "k8sCanaryDeleteOutcome";
  public final String K8S_BG_SWAP_SERVICES_OUTCOME = "k8sBGSwapServicesOutcome";
  public final String ENVIRONMENT = "environment";
  public final String OUTPUT = "output";
}
