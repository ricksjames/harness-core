package io.harness.cdng.gitops.steps;

import static com.google.common.base.Preconditions.checkArgument;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pms.execution.utils.AmbianceUtils.getAccountId;
import static io.harness.pms.execution.utils.AmbianceUtils.getOrgIdentifier;
import static io.harness.pms.execution.utils.AmbianceUtils.getProjectIdentifier;
import static java.lang.String.format;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.cdng.envGroup.services.EnvironmentGroupService;
import io.harness.cdng.gitops.service.ClusterService;
import io.harness.cdng.gitops.steps.ClusterStepParameters.EnvClusterRefs;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.gitops.models.Cluster;
import io.harness.gitops.models.ClusterQuery;
import io.harness.gitops.remote.GitopsResourceClient;
import io.harness.ng.beans.PageResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.executable.SyncExecutableWithRbac;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

@Slf4j
public class GitopsClustersStep implements SyncExecutableWithRbac<ClusterStepParameters> {
  private static final String GITOPS_SWEEPING_OUTCOME = "gitops";
  private static final int UNLIMITED_SIZE = 100000;

  @Inject private ClusterService clusterService;
  @Inject private EnvironmentGroupService environmentGroupService;
  @Inject private AccessControlClient accessControlClient;
  @Inject private GitopsResourceClient gitopsResourceClient;
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;

  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.GITOPS_CLUSTERS.getName())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Override
  public void validateResources(Ambiance ambiance, ClusterStepParameters stepParameters) {}

  @Override
  public StepResponse executeSyncAfterRbac(Ambiance ambiance, ClusterStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    log.info("Starting execution for GitopsClustersStep [{}]", stepParameters);
    Map<String, IndividualClusterInternal> validatedClusters = validatedClusters(ambiance, stepParameters);
    GitopsClustersOutcome outcome = toOutCome(validatedClusters);
    executionSweepingOutputResolver.consume(ambiance, GITOPS_SWEEPING_OUTCOME, outcome, StepOutcomeGroup.STAGE.name());
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  @Override
  public Class<ClusterStepParameters> getStepParametersClass() {
    return null;
  }

  private Map<String, IndividualClusterInternal> validatedClusters(Ambiance ambiance, ClusterStepParameters params) {
    final Collection<EnvClusterRefs> envClusterRefs;
    if (params.isDeployToAllEnvs()) {
      checkArgument(
          isNotEmpty(params.getEnvGroupRef()), "environment group must be provided when deploying to all environments");
      Optional<EnvironmentGroupEntity> egEntity = environmentGroupService.get(getAccountId(ambiance),
          getOrgIdentifier(ambiance), getProjectIdentifier(ambiance), params.getEnvGroupRef(), false);
      List<String> envs = egEntity.map(EnvironmentGroupEntity::getEnvIdentifiers).orElse(new ArrayList<>());
      envClusterRefs = envs.stream()
                           .map(e -> EnvClusterRefs.builder().envRef(e).deployToAll(true).build())
                           .collect(Collectors.toList());
    } else {
      envClusterRefs = params.getEnvClusterRefs();
    }

    if (isEmpty(envClusterRefs)) {
      return new HashMap<>();
    }

    // clusterId -> IndividualClusterInternal
    final Map<String, IndividualClusterInternal> individualClusters =
        fetchClusterRefs(params.getEnvGroupRef(), ambiance, envClusterRefs);
    final Set<String> clusterIdentifiers = individualClusters.keySet();

    Map<String, Object> filter = ImmutableMap.of("identifier", ImmutableMap.of("$in", clusterIdentifiers));
    try {
      final ClusterQuery query = ClusterQuery.builder()
                                     .accountId(getAccountId(ambiance))
                                     .orgIdentifier(getOrgIdentifier(ambiance))
                                     .projectIdentifier(getProjectIdentifier(ambiance))
                                     .pageIndex(0)
                                     .pageSize(clusterIdentifiers.size())
                                     .filter(filter)
                                     .build();
      final Response<PageResponse<Cluster>> response = gitopsResourceClient.listClusters(query).execute();
      if (response.isSuccessful() && response.body() != null) {
        List<Cluster> content = response.body().getContent();
        content.forEach(c -> {
          if (individualClusters.containsKey(c.getIdentifier())) {
            individualClusters.get(c.getIdentifier()).setOriginalCluster(c);
          }
        });
        individualClusters.values().removeIf(c -> c.getOriginalCluster() == null);
        return individualClusters;
      }
      throw new InvalidRequestException(format("Failed to fetch clusters from gitops. %s",
          response.errorBody() != null ? response.errorBody().string() : ""));
    } catch (IOException e) {
      throw new InvalidRequestException("Failed to fetch clusters from gitops. %s", e);
    }
  }

  private Map<String, IndividualClusterInternal> fetchClusterRefs(
      String envGroupRef, Ambiance ambiance, Collection<EnvClusterRefs> envClusterRefs) {
    List<IndividualClusterInternal> clusterRefs = new ArrayList<>();
    clusterRefs.addAll(envClusterRefs.stream()
                           .filter(ec -> !ec.isDeployToAll())
                           .map(ec
                               -> ec.getClusterRefs()
                                      .stream()
                                      .map(c
                                          -> IndividualClusterInternal.builder()
                                                 .envGroupRef(envGroupRef)
                                                 .envRef(ec.getEnvRef())
                                                 .clusterRef(c)
                                                 .build())
                                      .collect(Collectors.toList()))
                           .flatMap(List::stream)
                           .collect(Collectors.toList()));

    final Set<String> envsWithAllClustersAsTarget = envClusterRefs.stream()
                                                        .filter(EnvClusterRefs::isDeployToAll)
                                                        .map(EnvClusterRefs::getEnvRef)
                                                        .collect(Collectors.toSet());

    // Todo: Proper handling for large number of clusters
    if (isNotEmpty(envsWithAllClustersAsTarget)) {
      clusterRefs.addAll(clusterService
                             .listAcrossEnv(0, UNLIMITED_SIZE, getAccountId(ambiance), getOrgIdentifier(ambiance),
                                 getProjectIdentifier(ambiance), envsWithAllClustersAsTarget)
                             .stream()
                             .map(c
                                 -> IndividualClusterInternal.builder()
                                        .envGroupRef(envGroupRef)
                                        .envRef(c.getEnvRef())
                                        .clusterRef(c.getClusterRef())
                                        .build())
                             .collect(Collectors.toSet()));
    }

    return clusterRefs.stream().collect(
        Collectors.toMap(IndividualClusterInternal::getClusterRef, Function.identity()));
  }

  private GitopsClustersOutcome toOutCome(Map<String, IndividualClusterInternal> validatedClusters) {
    final GitopsClustersOutcome outcome = new GitopsClustersOutcome(new ArrayList<>());

    for (String clusterId : validatedClusters.keySet()) {
      IndividualClusterInternal clusterInternal = validatedClusters.get(clusterId);
      outcome.appendCluster(clusterInternal.getEnvGroupRef(), clusterInternal.getEnvRef(),
          clusterInternal.getOriginalCluster().getName());
    }

    //    log.info("{} clusters {} were processed", outcome.getClustersData().size(), outcome.getClustersData());
    //    log.warn("{} clusters {} were skipped as they were not present in gitops", skippedClusters.size(),
    //    skippedClusters);
    return outcome;
  }

  @Builder
  @Data
  static class IndividualClusterInternal {
    String envGroupRef;
    String envRef;
    String clusterRef;
    Cluster originalCluster;
  }
}
