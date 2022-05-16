/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service.instancedashboardservice;

import com.google.protobuf.Option;
import com.google.protobuf.util.Durations;
import com.mongodb.DuplicateKeyException;
import io.harness.InstancesTestBase;
import io.harness.category.element.UnitTests;
import io.harness.delegate.AccountId;
import io.harness.dtos.DeploymentSummaryDTO;
import io.harness.dtos.InstanceDTO;
import io.harness.dtos.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoDTO;
import io.harness.entities.ArtifactDetails;
import io.harness.entities.DeploymentSummary;
import io.harness.entities.InfrastructureMapping;
import io.harness.entities.Instance;
import io.harness.entities.deploymentinfo.DeploymentInfo;
import io.harness.entities.deploymentinfo.K8sDeploymentInfo;
import io.harness.entities.instanceinfo.K8sInstanceInfo;
import io.harness.entities.instancesyncperpetualtaskinfo.DeploymentInfoDetails;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo;
import io.harness.exception.UnexpectedException;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.mappers.DeploymentInfoDetailsMapper;
import io.harness.mappers.InstanceDetailsMapper;
import io.harness.models.BuildsByEnvironment;
import io.harness.models.CountByServiceIdAndEnvType;
import io.harness.models.EnvBuildInstanceCount;
import io.harness.models.InstanceDetailsByBuildId;
import io.harness.models.InstanceStats;
import io.harness.models.InstancesByBuildId;
import io.harness.models.constants.InstanceSyncConstants;
import io.harness.models.dashboard.InstanceCountDetails;
import io.harness.models.dashboard.InstanceCountDetailsByEnvTypeAndServiceId;
import io.harness.models.dashboard.InstanceCountDetailsByEnvTypeBase;
import io.harness.models.dashboard.InstanceCountDetailsByService;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.perpetualtask.PerpetualTaskClientContextDetails;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.repositories.deploymentsummary.DeploymentSummaryRepository;
import io.harness.repositories.infrastructuremapping.InfrastructureMappingRepository;
import io.harness.repositories.instancestats.InstanceStatsRepository;
import io.harness.repositories.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoInfoRepository;
import io.harness.rule.Owner;
import io.harness.service.instance.InstanceService;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;
import io.harness.service.instancesynchandler.K8sInstanceSyncHandler;
import io.harness.service.instancesyncperpetualtask.instancesyncperpetualtaskhandler.k8s.K8SInstanceSyncPerpetualTaskHandler;
import org.bson.BsonDocument;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.harness.InstancesTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.yaml.InfrastructureKind;
import io.harness.cdng.k8s.K8sEntityHelper;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesCredential;
import io.harness.delegate.Capability;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HelmInstallationCapability;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.K8sServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.NativeHelmServerInstanceInfo;
import io.harness.delegate.task.helm.HelmChartInfo;
import io.harness.delegate.task.helm.HelmInstanceSyncRequest;
import io.harness.delegate.task.helm.NativeHelmDeploymentReleaseData;
import io.harness.delegate.task.k8s.DirectK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sDeploymentReleaseData;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sInstanceSyncRequest;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.K8sDeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.NativeHelmDeploymentInfoDTO;
import io.harness.dtos.instanceinfo.InstanceInfoDTO;
import io.harness.dtos.instanceinfo.K8sInstanceInfoDTO;
import io.harness.dtos.instanceinfo.NativeHelmInstanceInfoDTO;
import io.harness.entities.InstanceType;
import io.harness.entities.instanceinfo.NativeHelmInstanceInfo;
import io.harness.k8s.model.HelmVersion;
import io.harness.k8s.model.K8sContainer;
import io.harness.ng.core.BaseNGAccess;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.instancesync.K8sDeploymentRelease;
import io.harness.perpetualtask.instancesync.K8sInstanceSyncPerpetualTaskParams;
import io.harness.perpetualtask.instancesync.NativeHelmDeploymentRelease;
import io.harness.perpetualtask.instancesync.NativeHelmInstanceSyncPerpetualTaskParams;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.service.instancesynchandler.NativeHelmInstanceSyncHandler;
import io.harness.service.instancesyncperpetualtask.instancesyncperpetualtaskhandler.helm.NativeHelmInstanceSyncPerpetualTaskHandler;
import org.apache.groovy.util.Maps;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.opensaml.xmlsec.signature.P;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

import static io.harness.delegate.beans.NgSetupFields.NG;
import static io.harness.delegate.beans.NgSetupFields.OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.in;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;
import static org.assertj.core.api.Assertions.assertThat;

public class InstanceDashboardServiceImplTest extends InstancesTestBase {

    private final String ACCOUNT_IDENTIFIER = "acc";
    private final String PROJECT_IDENTIFIER =  "proj";
    private final String ORG_IDENTIFIER = "org";
    private final String SERVICE_IDENTIFIER = "serv";
    private final String ENV_IDENTIFIER = "env";
    private final List<String> BUILD_IDS = Arrays.asList("id1", "id2");
    @Mock InstanceService instanceService;
    @Mock InstanceDetailsMapper instanceDetailsMapper;
    @InjectMocks InstanceDashboardServiceImpl instanceDashboardService;

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getActiveInstanceCountDetailsByEnvTypeTest() {

        InstanceDTO instanceDTO = InstanceDTO.builder().serviceIdentifier(SERVICE_IDENTIFIER).envIdentifier(ENV_IDENTIFIER).envType(EnvironmentType.Production).build();
        InstanceDTO instanceDTO1 = InstanceDTO.builder().serviceIdentifier(SERVICE_IDENTIFIER).envIdentifier(ENV_IDENTIFIER).envType(EnvironmentType.PreProduction).build();
        InstanceDTO instanceDTO2 = InstanceDTO.builder().serviceIdentifier(SERVICE_IDENTIFIER + "2").envIdentifier(ENV_IDENTIFIER).envType(EnvironmentType.Production).build();
        InstanceDTO instanceDTO3 = InstanceDTO.builder().serviceIdentifier(SERVICE_IDENTIFIER + "2").envIdentifier(ENV_IDENTIFIER).envType(EnvironmentType.PreProduction).build();
        InstanceDTO instanceDTO4 = InstanceDTO.builder().serviceIdentifier(SERVICE_IDENTIFIER + "2").envIdentifier(ENV_IDENTIFIER + "2").envType(EnvironmentType.Production).build();
        when(instanceService.getActiveInstances(anyString(), anyString(), anyString(), anyLong())).thenReturn(Arrays.asList(instanceDTO, instanceDTO1, instanceDTO2, instanceDTO3, instanceDTO4));

        InstanceCountDetails instanceCountDetails = instanceDashboardService.getActiveInstanceCountDetailsByEnvType(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
        assertThat(instanceCountDetails.getProdInstances()).isEqualTo(3);
        assertThat(instanceCountDetails.getNonProdInstances()).isEqualTo(2);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getActiveInstancesByServiceIdGroupedByEnvironmentAndBuildTest() {

        InstanceDTO instanceDTO = InstanceDTO.builder().serviceIdentifier(SERVICE_IDENTIFIER).envIdentifier(ENV_IDENTIFIER).primaryArtifact(ArtifactDetails.builder().tag("tag1").build()).envType(EnvironmentType.Production).build();
        InstanceDTO instanceDTO1 = InstanceDTO.builder().serviceIdentifier(SERVICE_IDENTIFIER).envIdentifier(ENV_IDENTIFIER).primaryArtifact(ArtifactDetails.builder().tag("tag1").build()).envType(EnvironmentType.PreProduction).build();
        InstanceDTO instanceDTO2 = InstanceDTO.builder().serviceIdentifier(SERVICE_IDENTIFIER + "2").envIdentifier(ENV_IDENTIFIER).primaryArtifact(ArtifactDetails.builder().tag("tag2").build()).envType(EnvironmentType.Production).build();
        InstanceDTO instanceDTO3 = InstanceDTO.builder().serviceIdentifier(SERVICE_IDENTIFIER + "2").envIdentifier(ENV_IDENTIFIER).primaryArtifact(ArtifactDetails.builder().tag("tag2").build()).envType(EnvironmentType.PreProduction).build();
        InstanceDTO instanceDTO4 = InstanceDTO.builder().serviceIdentifier(SERVICE_IDENTIFIER + "2").envIdentifier(ENV_IDENTIFIER + "2").primaryArtifact(ArtifactDetails.builder().tag("tag3").build()).envType(EnvironmentType.Production).build();
        when(instanceService.getActiveInstancesByServiceId(anyString(), anyString(), anyString(), anyString(), anyLong())).thenReturn(Arrays.asList(instanceDTO, instanceDTO1, instanceDTO2, instanceDTO3, instanceDTO4));

        List<BuildsByEnvironment> buildsByEnvironmentList = instanceDashboardService.getActiveInstancesByServiceIdGroupedByEnvironmentAndBuild(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, SERVICE_IDENTIFIER, 10);
        assertThat(buildsByEnvironmentList.size()).isEqualTo(2);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getEnvBuildInstanceCountByServiceIdTest() {
        EnvBuildInstanceCount envBuildInstanceCount = new EnvBuildInstanceCount(ENV_IDENTIFIER, "ENV", "TAG", 3);
        AggregationResults<EnvBuildInstanceCount> envBuildInstanceCountAggregationResults =  new AggregationResults<>(Arrays.asList(envBuildInstanceCount), new Document());
        when(instanceService.getEnvBuildInstanceCountByServiceId(anyString(), anyString(), anyString(), anyString(), anyLong())).thenReturn(envBuildInstanceCountAggregationResults);
        assertThat(instanceDashboardService.getEnvBuildInstanceCountByServiceId(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, SERVICE_IDENTIFIER, 10).size()).isEqualTo(1);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getActiveInstancesByServiceIdEnvIdAndBuildIdsTest() {
        Instance instance = Instance.builder().
                accountIdentifier(ACCOUNT_IDENTIFIER).projectIdentifier(PROJECT_IDENTIFIER)
                .orgIdentifier(ORG_IDENTIFIER).serviceIdentifier(SERVICE_IDENTIFIER).envIdentifier(ENV_IDENTIFIER).envName("env1").envType(EnvironmentType.Production)
                .instanceKey("key").connectorRef("connector").id("id").createdAt(5L).deletedAt(10)
                .instanceType(InstanceType.K8S_INSTANCE).infrastructureMappingId("mappingId").lastDeployedAt(3).lastDeployedByName("asdf").lastPipelineExecutionId("sdf")
                .lastPipelineExecutionName("sdfasd").serviceName("serv").isDeleted(false).instanceInfo(K8sInstanceInfo.builder().build())
                .lastModifiedAt(10L).build();
        InstancesByBuildId instanceDetailsByBuildId = new InstancesByBuildId("build1", Arrays.asList(instance));
        AggregationResults<InstancesByBuildId> instanceDetailsByBuildIdAggregationResults = new AggregationResults<>(Arrays.asList(instanceDetailsByBuildId), new Document());
        when(instanceService.getActiveInstancesByServiceIdEnvIdAndBuildIds(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, SERVICE_IDENTIFIER, ENV_IDENTIFIER, BUILD_IDS, 10, InstanceSyncConstants.INSTANCE_LIMIT)).thenReturn(instanceDetailsByBuildIdAggregationResults);
        List<InstanceDetailsByBuildId> instanceDetailsByBuildIdList = instanceDashboardService.getActiveInstancesByServiceIdEnvIdAndBuildIds(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER,
                SERVICE_IDENTIFIER, ENV_IDENTIFIER, BUILD_IDS, 10);
        assertThat(instanceDetailsByBuildIdList.size()).isEqualTo(1);
        assertThat(instanceDetailsByBuildIdList.get(0).getBuildId()).isEqualTo("build1");
        assertThat(instanceDetailsByBuildIdList.get(0).getInstances().size()).isEqualTo(0);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getActiveServiceInstanceCountBreakdownTest() {
        CountByServiceIdAndEnvType countByServiceIdAndEnvType = new CountByServiceIdAndEnvType(SERVICE_IDENTIFIER, EnvironmentType.Production, 2);
        AggregationResults<CountByServiceIdAndEnvType> countByServiceIdAndEnvTypeAggregationResults = new AggregationResults<>(Arrays.asList(countByServiceIdAndEnvType), new Document());
        when(instanceService.getActiveServiceInstanceCountBreakdown(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, Arrays.asList(SERVICE_IDENTIFIER), 10)).thenReturn(countByServiceIdAndEnvTypeAggregationResults);
        Map<EnvironmentType, Integer> envTypeVsInstanceCountMap = new HashMap<>();
        envTypeVsInstanceCountMap.put(EnvironmentType.Production, 2);
        Map<String, InstanceCountDetailsByEnvTypeBase> instanceCountDetailsByEnvTypeBaseMap = new HashMap<>();
        instanceCountDetailsByEnvTypeBaseMap.put(SERVICE_IDENTIFIER, InstanceCountDetailsByEnvTypeBase.builder().envTypeVsInstanceCountMap(envTypeVsInstanceCountMap).build());
        InstanceCountDetailsByEnvTypeAndServiceId instanceCountDetailsByEnvTypeAndServiceId = instanceDashboardService.getActiveServiceInstanceCountBreakdown(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, Arrays.asList(SERVICE_IDENTIFIER), 10);
        assertThat(instanceCountDetailsByEnvTypeAndServiceId.getInstanceCountDetailsByEnvTypeBaseMap().containsKey(SERVICE_IDENTIFIER)).isTrue();
        assertThat(instanceCountDetailsByEnvTypeAndServiceId.getInstanceCountDetailsByEnvTypeBaseMap().get(SERVICE_IDENTIFIER).getProdInstances()).isEqualTo(2);
    }
}
