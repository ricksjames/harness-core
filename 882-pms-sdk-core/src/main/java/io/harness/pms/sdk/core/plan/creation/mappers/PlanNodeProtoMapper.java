package io.harness.pms.sdk.core.plan.creation.mappers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.PmsSdkModuleUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.timeout.SdkTimeoutObtainment;
import io.harness.serializer.KryoSerializer;
import io.harness.timeout.contracts.TimeoutObtainment;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class PlanNodeProtoMapper {
  @Inject @Named(PmsSdkModuleUtils.SDK_SERVICE_NAME) String serviceName;
  @Inject private KryoSerializer kryoSerializer;

  public PlanNodeProto toPlanNodeProtoWithDecoratedFields(PlanNode node) {
    PlanNodeProto.Builder builder =
        PlanNodeProto.newBuilder()
            .setUuid(node.getUuid())
            .setName(isEmpty(node.getName()) ? "" : node.getName())
            .setStepType(node.getStepType())
            .setIdentifier(isEmpty(node.getIdentifier()) ? "" : node.getIdentifier())
            .setStepParameters(
                node.getStepParameters() == null ? "" : RecastOrchestrationUtils.toJson(node.getStepParameters()))
            .addAllRebObjects(CollectionUtils.emptyIfNull(node.getRefObjects()))
            .addAllAdviserObtainments(CollectionUtils.emptyIfNull(node.getAdviserObtainments()))
            .addAllFacilitatorObtainments(CollectionUtils.emptyIfNull(node.getFacilitatorObtainments()))
            .setSkipExpressionChain(node.isSkipExpressionChain())
            .setSkipType(node.getSkipGraphType())
            .setServiceName(serviceName)
            .addAllTimeoutObtainments(toTimeoutObtainments(node.getTimeoutObtainments()))
            .setSkipUnresolvedExpressionsCheck(node.isSkipUnresolvedExpressionsCheck());
    if (node.getWhenCondition() != null) {
      builder.setWhenCondition(node.getWhenCondition());
    }
    if (node.getSkipCondition() != null) {
      builder.setSkipCondition(node.getSkipCondition());
    }
    if (node.getGroup() != null) {
      builder.setGroup(node.getGroup());
    }
    if (node.getStepParameters() != null && node.getStepParameters().toViewJson() != null) {
      builder.setStepInputs(node.getStepParameters().toViewJson());
    }
    return builder.build();
  }

  private List<TimeoutObtainment> toTimeoutObtainments(List<SdkTimeoutObtainment> sdkTimeoutObtainments) {
    if (sdkTimeoutObtainments == null) {
      return new ArrayList<>();
    }
    return sdkTimeoutObtainments.stream()
        .map(entry
            -> TimeoutObtainment.newBuilder()
                   .setDimension(entry.getDimension())
                   .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(entry.getParameters())))
                   .build())
        .collect(Collectors.toList());
  }
}
