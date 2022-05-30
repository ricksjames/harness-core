package io.harness.cdng.gitops.steps;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.cdng.envGroup.services.EnvironmentGroupService;
import io.harness.cdng.gitops.service.ClusterService;
import io.harness.gitops.models.Cluster;
import io.harness.gitops.models.ClusterQuery;
import io.harness.gitops.remote.GitopsResourceClient;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jooq.tools.reflect.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;
import retrofit2.Response;

@RunWith(JUnitParamsRunner.class)
public class GitopsClustersStepTest extends CategoryTest {
  public static final int EXPECTED_PAGE_SIZE = 100000;
  @Mock private ExecutionSweepingOutputService sweepingOutputService;
  @Mock private EnvironmentGroupService environmentGroupService;
  @Mock private GitopsResourceClient gitopsResourceClient;
  @Mock private ClusterService clusterService;

  /*
  envgroup -> envGroupId
  environments -> env1, env2, env3
  env1 -> clusters: c1, c2
  env2 -> clusters: c3, c4, c5 (c5 does not exist on gitops)
  env3 -> no gitops clusters
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    doReturn(Optional.of(EnvironmentGroupEntity.builder().envIdentifiers(asList("env1", "env2", "env3")).build()))
        .when(environmentGroupService)
        .get("accountId", "orgId", "projId", "envGroupId", false);

    // mock cluster list call from gitops service
    Call<ResponseDTO<List<Cluster>>> rmock1 = mock(Call.class);
    doReturn(rmock1)
        .when(gitopsResourceClient)
        .listClusters(ClusterQuery.builder()
                          .accountId("accountId")
                          .orgIdentifier("orgId")
                          .projectIdentifier("projId")
                          .pageIndex(0)
                          .pageSize(2)
                          .filter(ImmutableMap.of("identifier", ImmutableMap.of("$in", ImmutableSet.of("c1", "c2"))))
                          .build());
    doReturn(Response.success(PageResponse.builder()
                                  .content(asList(Cluster.builder().identifier("c1").name("c1-name").build(),
                                      Cluster.builder().identifier("c2").name("c2-name").build()))
                                  .build()))
        .when(rmock1)
        .execute();

    // mock cluster list call from gitops service
    Call<ResponseDTO<List<Cluster>>> rmock2 = mock(Call.class);
    doReturn(rmock2)
        .when(gitopsResourceClient)
        .listClusters(
            ClusterQuery.builder()
                .accountId("accountId")
                .orgIdentifier("orgId")
                .projectIdentifier("projId")
                .pageIndex(0)
                .pageSize(3)
                .filter(ImmutableMap.of("identifier", ImmutableMap.of("$in", ImmutableSet.of("c3", "c4", "c5"))))
                .build());
    doReturn(Response.success(PageResponse.builder()
                                  .content(asList(Cluster.builder().identifier("c3").name("c3-name").build(),
                                      Cluster.builder().identifier("c4").name("c4-name").build()))
                                  .build()))
        .when(rmock2)
        .execute();

    Call<ResponseDTO<List<Cluster>>> rmock3 = mock(Call.class);
    doReturn(rmock3)
        .when(gitopsResourceClient)
        .listClusters(ClusterQuery.builder()
                          .accountId("accountId")
                          .orgIdentifier("orgId")
                          .projectIdentifier("projId")
                          .pageIndex(0)
                          .pageSize(5)
                          .filter(ImmutableMap.of(
                              "identifier", ImmutableMap.of("$in", ImmutableSet.of("c3", "c4", "c5", "c1", "c2"))))
                          .build());
    doReturn(Response.success(PageResponse.builder()
                                  .content(asList(Cluster.builder().identifier("c1").name("c1-name").build(),
                                      Cluster.builder().identifier("c2").name("c2-name").build(),
                                      Cluster.builder().identifier("c3").name("c3-name").build(),
                                      Cluster.builder().identifier("c4").name("c4-name").build()))
                                  .build()))
        .when(rmock3)
        .execute();

    Call<ResponseDTO<List<Cluster>>> rmock4 = mock(Call.class);
    doReturn(rmock4)
        .when(gitopsResourceClient)
        .listClusters(ClusterQuery.builder()
                          .accountId("accountId")
                          .orgIdentifier("orgId")
                          .projectIdentifier("projId")
                          .pageIndex(0)
                          .pageSize(1)
                          .filter(ImmutableMap.of("identifier", ImmutableMap.of("$in", ImmutableSet.of("c4"))))
                          .build());
    doReturn(
        Response.success(
            PageResponse.builder().content(asList(Cluster.builder().identifier("c4").name("c4-name").build())).build()))
        .when(rmock4)
        .execute();

    // mock cluster list call from ng manager
    doReturn(
        new PageImpl(asList(io.harness.cdng.gitops.entity.Cluster.builder().clusterRef("c1").envRef("env1").build(),
            io.harness.cdng.gitops.entity.Cluster.builder().clusterRef("c2").envRef("env1").build())))
        .when(clusterService)
        .listAcrossEnv(0, EXPECTED_PAGE_SIZE, "accountId", "orgId", "projId", ImmutableSet.of("env1"));

    doReturn(
        new PageImpl(asList(io.harness.cdng.gitops.entity.Cluster.builder().clusterRef("c1").envRef("env1").build(),
            io.harness.cdng.gitops.entity.Cluster.builder().clusterRef("c2").envRef("env1").build(),
            io.harness.cdng.gitops.entity.Cluster.builder().clusterRef("c3").envRef("env2").build(),
            io.harness.cdng.gitops.entity.Cluster.builder().clusterRef("c4").envRef("env2").build(),
            io.harness.cdng.gitops.entity.Cluster.builder().clusterRef("c5").envRef("env2").build())))
        .when(clusterService)
        .listAcrossEnv(0, EXPECTED_PAGE_SIZE, "accountId", "orgId", "projId", ImmutableSet.of("env1", "env2", "env3"));

    doReturn(
        new PageImpl(asList(io.harness.cdng.gitops.entity.Cluster.builder().clusterRef("c3").envRef("env2").build(),
            io.harness.cdng.gitops.entity.Cluster.builder().clusterRef("c4").envRef("env2").build(),
            io.harness.cdng.gitops.entity.Cluster.builder().clusterRef("c5").envRef("env2").build())))
        .when(clusterService)
        .listAcrossEnv(0, EXPECTED_PAGE_SIZE, "accountId", "orgId", "projId", ImmutableSet.of("env2"));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  @Parameters(method = "getData")
  public void testExecuteSyncAfterRbac(ClusterStepParameters input, GitopsClustersOutcome expectedOutcome) {
    GitopsClustersStep step = new GitopsClustersStep();

    Reflect.on(step).set("executionSweepingOutputResolver", sweepingOutputService);
    Reflect.on(step).set("environmentGroupService", environmentGroupService);
    Reflect.on(step).set("clusterService", clusterService);
    Reflect.on(step).set("gitopsResourceClient", gitopsResourceClient);

    step.executeSyncAfterRbac(buildAmbiance(), input, StepInputPackage.builder().build(), null);

    verify(sweepingOutputService).consume(any(), eq("gitops"), eq(expectedOutcome), eq("STAGE"));
    reset(sweepingOutputService);
  }

  // Test cases
  private Object[][] getData() {
    final Object[] set1 =
        new Object[] {ClusterStepParameters.builder().build(), new GitopsClustersOutcome(new ArrayList<>())};
    final Object[] set2 = new Object[] {
        ClusterStepParameters.builder().envGroupRef("envGroupId").deployToAllEnvs(true).build(),
        new GitopsClustersOutcome(new ArrayList<>())
            .appendCluster("envGroupId", "env2", "c3-name")
            .appendCluster("envGroupId", "env2", "c4-name")
            .appendCluster("envGroupId", "env1", "c1-name")
            .appendCluster("envGroupId", "env1", "c2-name"),

    };
    final Object[] set3 = new Object[] {
        ClusterStepParameters.builder()
            .envClusterRefs(
                asList(ClusterStepParameters.EnvClusterRefs.builder().envRef("env1").deployToAll(true).build()))
            .deployToAllEnvs(false)
            .build(),
        new GitopsClustersOutcome(new ArrayList<>()).appendCluster("env1", "c1-name").appendCluster("env1", "c2-name"),

    };

    final Object[] set4 = new Object[] {
        ClusterStepParameters.builder()
            .envClusterRefs(
                asList(ClusterStepParameters.EnvClusterRefs.builder().envRef("env2").deployToAll(true).build()))
            .deployToAllEnvs(false)
            .build(),
        new GitopsClustersOutcome(new ArrayList<>()).appendCluster("env2", "c3-name").appendCluster("env2", "c4-name"),
    };

    final Object[] set5 = new Object[] {
        ClusterStepParameters.builder()
            .envClusterRefs(asList(ClusterStepParameters.EnvClusterRefs.builder()
                                       .envRef("env2")
                                       .deployToAll(false)
                                       .clusterRefs(asList("c4"))
                                       .build()))
            .deployToAllEnvs(false)
            .build(),
        new GitopsClustersOutcome(new ArrayList<>()).appendCluster("env2", "c4-name"),
    };

    return new Object[][] {set1, set2, set3, set4, set5};
  }

  private Ambiance buildAmbiance() {
    return Ambiance.newBuilder()
        .setPlanExecutionId("PLAN_EXECUTION_ID")
        .setPlanId("PLAN_ID")
        .putAllSetupAbstractions(ImmutableMap.of(
            "accountId", "accountId", "orgIdentifier", "orgId", "projectIdentifier", "projId", "appId", "APP_ID"))
        .build();
  }
}