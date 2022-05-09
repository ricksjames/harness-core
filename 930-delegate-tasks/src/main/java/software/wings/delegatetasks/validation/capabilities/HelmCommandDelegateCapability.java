package software.wings.delegatetasks.validation.capabilities;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import lombok.Builder;
import lombok.Value;
import software.wings.delegatetasks.helm.HelmCommandRequestPrams;

import javax.validation.constraints.NotNull;
import java.time.Duration;

@Value
@Builder
public class HelmCommandDelegateCapability implements ExecutionCapability {
    @NotNull
    HelmCommandRequestPrams commandRequest;
    CapabilityType capabilityType = CapabilityType.HELM_COMMAND;

    @Override
    public EvaluationMode evaluationMode() {
        return EvaluationMode.AGENT;
    }

    @Override
    public String fetchCapabilityBasis() {
        return "Helm Installed. Version : " + commandRequest.getHelmVersion().name();
    }

    @Override
    public Duration getMaxValidityPeriod() {
        return Duration.ofHours(6);
    }

    @Override
    public Duration getPeriodUntilNextValidation() {
        return Duration.ofHours(4);
    }
}
