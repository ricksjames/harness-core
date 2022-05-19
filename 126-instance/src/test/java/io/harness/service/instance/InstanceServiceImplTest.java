/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service.instance;

import com.google.protobuf.Option;
import com.google.protobuf.util.Durations;
import com.mongodb.DuplicateKeyException;
import graphql.execution.Execution;
import io.harness.InstancesTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.service.steps.ServiceStepOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
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
import io.harness.entities.instanceinfo.InstanceInfo;
import io.harness.entities.instanceinfo.K8sInstanceInfo;
import io.harness.entities.instancesyncperpetualtaskinfo.DeploymentInfoDetails;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo;
import io.harness.exception.UnexpectedException;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.mappers.DeploymentInfoDetailsMapper;
import io.harness.models.CountByServiceIdAndEnvType;
import io.harness.models.DeploymentEvent;
import io.harness.models.EnvBuildInstanceCount;
import io.harness.models.InstanceStats;
import io.harness.models.InstancesByBuildId;
import io.harness.models.constants.InstanceSyncConstants;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.perpetualtask.PerpetualTaskClientContextDetails;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.TriggerInfo;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.repositories.deploymentsummary.DeploymentSummaryRepository;
import io.harness.repositories.infrastructuremapping.InfrastructureMappingRepository;
import io.harness.repositories.instance.InstanceRepository;
import io.harness.repositories.instancestats.InstanceStatsRepository;
import io.harness.repositories.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoInfoRepository;
import io.harness.rule.Owner;
import io.harness.service.deploymentsummary.DeploymentSummaryService;
import io.harness.service.infrastructuremapping.InfrastructureMappingService;
import io.harness.service.instancesync.InstanceSyncService;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;
import io.harness.service.instancesynchandler.K8sInstanceSyncHandler;
import io.harness.service.instancesynchandlerfactory.InstanceSyncHandlerFactoryService;
import io.harness.service.instancesyncperpetualtask.instancesyncperpetualtaskhandler.k8s.K8SInstanceSyncPerpetualTaskHandler;
import io.harness.steps.environment.EnvironmentOutcome;
import org.bson.BsonDocument;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;
import static org.assertj.core.api.Assertions.assertThat;

public class InstanceServiceImplTest extends InstancesTestBase {

    private final String INSTANCE_KEY = "instance_key";
    @Mock InstanceRepository instanceRepository;
    @InjectMocks InstanceServiceImpl instanceService;

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void saveTest() {
        InstanceInfoDTO instanceInfoDTO = K8sInstanceInfoDTO.builder().build();
        InstanceDTO instanceDTO = InstanceDTO.builder().instanceInfoDTO(instanceInfoDTO).build();
        InstanceInfo instanceInfo = K8sInstanceInfo.builder().build();
        Instance instance = Instance.builder().instanceInfo(instanceInfo).deletedAt(234L).createdAt(123L).lastModifiedAt(3245L).build();
        when(instanceRepository.save(any())).thenReturn(instance);
        InstanceDTO actualInstanceDTO = instanceService.save(instanceDTO);
        assertThat(actualInstanceDTO.getCreatedAt()).isEqualTo(123L);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void saveAllTest() {
        InstanceInfo instanceInfo = K8sInstanceInfo.builder().build();
        Instance instance = Instance.builder().instanceInfo(instanceInfo).deletedAt(234L).createdAt(123L).lastModifiedAt(3245L).build();
        InstanceInfoDTO instanceInfoDTO = K8sInstanceInfoDTO.builder().build();
        InstanceDTO instanceDTO = InstanceDTO.builder().instanceInfoDTO(instanceInfoDTO).build();
        List<InstanceDTO> instanceDTOList = Arrays.asList(instanceDTO);
        when(instanceRepository.saveAll(anyList())).thenReturn(Arrays.asList(instance));
        List<InstanceDTO> actualInstanceDTOList = instanceService.saveAll(instanceDTOList);
        assertThat(actualInstanceDTOList.size()).isEqualTo(1);
        assertThat(actualInstanceDTOList.get(0).getCreatedAt()).isEqualTo(123L);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void saveOrReturnEmptyIfAlreadyExists() {

        InstanceInfo instanceInfo = K8sInstanceInfo.builder().build();
        Instance instance = Instance.builder().instanceInfo(instanceInfo).deletedAt(234L).createdAt(123L).lastModifiedAt(3245L).build();
        InstanceInfoDTO instanceInfoDTO = K8sInstanceInfoDTO.builder().build();
        InstanceDTO instanceDTO = InstanceDTO.builder().instanceInfoDTO(instanceInfoDTO).build();
        when(instanceRepository.save(any())).thenReturn(instance);
        Optional<InstanceDTO> respone = instanceService.saveOrReturnEmptyIfAlreadyExists(instanceDTO);
        assertThat(respone.get().getLastModifiedAt()).isEqualTo(3245L);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void deleteByIdTest() {

        String id = "id";
        ArgumentCaptor<String> idCaptor = new ArgumentCaptor<>();
        instanceService.deleteById(id);
        verify(instanceRepository, times(1)).deleteById(idCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(id);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void deleteAllTest() {

        InstanceInfoDTO instanceInfoDTO = K8sInstanceInfoDTO.builder().build();
        InstanceDTO instanceDTO = InstanceDTO.builder().instanceInfoDTO(instanceInfoDTO).instanceKey(INSTANCE_KEY).build();
        List<InstanceDTO> instanceDTOList = Arrays.asList(instanceDTO);
        ArgumentCaptor<String> captor = new ArgumentCaptor<>();
        instanceService.deleteAll(instanceDTOList);
        verify(instanceRepository, times(1)).deleteByInstanceKey(captor.capture());
        assertThat(captor.getValue()).isEqualTo(INSTANCE_KEY);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void softDeleteTest() {

        ArgumentCaptor<Criteria> criteriaArgumentCaptor = new ArgumentCaptor<>();
        ArgumentCaptor<Update> updateArgumentCaptor = new ArgumentCaptor<>();
        InstanceInfo instanceInfo = K8sInstanceInfo.builder().build();
        Instance instance = Instance.builder().instanceInfo(instanceInfo).deletedAt(234L).createdAt(123L).lastModifiedAt(3245L).build();
        when(instanceRepository.findAndModify(any(), any())).thenReturn(instance);
        Optional<InstanceDTO> expectedInstanceDTO = instanceService.softDelete(INSTANCE_KEY);
        assertThat(expectedInstanceDTO.get().getLastModifiedAt()).isEqualTo(3245L);
        verify(instanceRepository, times(1)).findAndModify(criteriaArgumentCaptor.capture(), updateArgumentCaptor.capture());
        Criteria criteria = criteriaArgumentCaptor.getValue();
        assertThat(criteria.getKey()).isEqualTo(Instance.InstanceKeys.instanceKey);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getActiveInstancesByAccountTest() {
        String accountIdentifier = "Acc";
        long timestamp = 123L;
        InstanceInfo instanceInfo = K8sInstanceInfo.builder().build();
        Instance instance = Instance.builder().instanceInfo(instanceInfo).deletedAt(234L).createdAt(123L).lastModifiedAt(3245L).build();
        when(instanceRepository.getActiveInstancesByAccount(accountIdentifier, timestamp)).thenReturn(Arrays.asList(instance));
        List<InstanceDTO> instanceDTOList = instanceService.getActiveInstancesByAccount(accountIdentifier, timestamp);
        assertThat(instanceDTOList.size()).isEqualTo(1);
        assertThat(instanceDTOList.get(0).getCreatedAt()).isEqualTo(123L);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getInstancesDeployedInIntervalTest() {
        String accountIdentifier = "Acc";
        long startTimestamp = 123L;
        long endTimestamp = 123L;
        InstanceInfo instanceInfo = K8sInstanceInfo.builder().build();
        Instance instance = Instance.builder().instanceInfo(instanceInfo).deletedAt(234L).createdAt(123L).lastModifiedAt(3245L).build();
        when(instanceRepository.getInstancesDeployedInInterval(accountIdentifier, startTimestamp, endTimestamp)).thenReturn(Arrays.asList(instance));
        List<InstanceDTO> instanceDTOList = instanceService.getInstancesDeployedInInterval(accountIdentifier, startTimestamp, endTimestamp);
        assertThat(instanceDTOList.size()).isEqualTo(1);
        assertThat(instanceDTOList.get(0).getCreatedAt()).isEqualTo(123L);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getInstancesTest() {
        String accountIdentifier = "Acc";
        String orgIdentifier = "org";
        String projectIdentifier = "pro";
        String infrastructureMappingId = "infraMappingId";
        InstanceInfo instanceInfo = K8sInstanceInfo.builder().build();
        Instance instance = Instance.builder().instanceInfo(instanceInfo).deletedAt(234L).createdAt(123L).lastModifiedAt(3245L).build();
        when(instanceRepository.getInstances(accountIdentifier, orgIdentifier, projectIdentifier, infrastructureMappingId)).thenReturn(Arrays.asList(instance));
        List<InstanceDTO> instanceDTOList = instanceService.getInstances(accountIdentifier, orgIdentifier, projectIdentifier, infrastructureMappingId);
        assertThat(instanceDTOList.size()).isEqualTo(1);
        assertThat(instanceDTOList.get(0).getCreatedAt()).isEqualTo(123L);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getActiveInstancesTest() {
        String accountIdentifier = "Acc";
        String orgIdentifier = "org";
        long timestamp = 123L;
        String projectIdentifier = "pro";
        InstanceInfo instanceInfo = K8sInstanceInfo.builder().build();
        Instance instance = Instance.builder().instanceInfo(instanceInfo).deletedAt(234L).createdAt(123L).lastModifiedAt(3245L).build();
        when(instanceRepository.getActiveInstances(accountIdentifier, orgIdentifier, projectIdentifier, timestamp)).thenReturn(Arrays.asList(instance));
        List<InstanceDTO> instanceDTOList = instanceService.getActiveInstances(accountIdentifier, orgIdentifier, projectIdentifier, timestamp);
        assertThat(instanceDTOList.size()).isEqualTo(1);
        assertThat(instanceDTOList.get(0).getCreatedAt()).isEqualTo(123L);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getActiveInstancesByServiceIdTest() {
        String accountIdentifier = "Acc";
        String orgIdentifier = "org";
        long timestamp = 123L;
        String projectIdentifier = "pro";
        String serviceId = "serviceId";
        InstanceInfo instanceInfo = K8sInstanceInfo.builder().build();
        Instance instance = Instance.builder().instanceInfo(instanceInfo).deletedAt(234L).createdAt(123L).lastModifiedAt(3245L).build();
        when(instanceRepository.getActiveInstancesByServiceId(accountIdentifier, orgIdentifier, projectIdentifier, serviceId, timestamp)).thenReturn(Arrays.asList(instance));
        List<InstanceDTO> instanceDTOList = instanceService.getActiveInstancesByServiceId(accountIdentifier, orgIdentifier, projectIdentifier, serviceId, timestamp);
        assertThat(instanceDTOList.size()).isEqualTo(1);
        assertThat(instanceDTOList.get(0).getCreatedAt()).isEqualTo(123L);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getActiveInstancesByInfrastructureMappingIdTest() {
        String accountIdentifier = "Acc";
        String orgIdentifier = "org";
        long timestamp = 123L;
        String projectIdentifier = "pro";
        String infrastructureMappingId = "infraMappingId";
        InstanceInfo instanceInfo = K8sInstanceInfo.builder().build();
        Instance instance = Instance.builder().instanceInfo(instanceInfo).deletedAt(234L).createdAt(123L).lastModifiedAt(3245L).build();
        when(instanceRepository.getActiveInstancesByInfrastructureMappingId(accountIdentifier, orgIdentifier, projectIdentifier, infrastructureMappingId, timestamp)).thenReturn(Arrays.asList(instance));
        List<InstanceDTO> instanceDTOList = instanceService.getActiveInstancesByInfrastructureMappingId(accountIdentifier, orgIdentifier, projectIdentifier, infrastructureMappingId, timestamp);
        assertThat(instanceDTOList.size()).isEqualTo(1);
        assertThat(instanceDTOList.get(0).getCreatedAt()).isEqualTo(123L);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getActiveInstancesByInstanceInfoTest() {
        String accountIdentifier = "Acc";
        String instanceInfoNamespace = "instanceInfoNamespace";
        String instanceInfoPodName = "instanceInfoPodName";
        InstanceInfo instanceInfo = K8sInstanceInfo.builder().build();
        Instance instance = Instance.builder().instanceInfo(instanceInfo).deletedAt(234L).createdAt(123L).lastModifiedAt(3245L).build();
        when(instanceRepository.getActiveInstancesByInstanceInfo(
                accountIdentifier, instanceInfoNamespace, instanceInfoPodName)).thenReturn(Arrays.asList(instance));
        List<InstanceDTO> instanceDTOList = instanceService.getActiveInstancesByInstanceInfo(accountIdentifier, instanceInfoNamespace, instanceInfoPodName);
        assertThat(instanceDTOList.size()).isEqualTo(1);
        assertThat(instanceDTOList.get(0).getCreatedAt()).isEqualTo(123L);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getEnvBuildInstanceCountByServiceIdTest() {
        String accountIdentifier = "Acc";
        String orgIdentifier = "org";
        long timestamp = 123L;
        String projectIdentifier = "pro";
        String serviceId = "serviceId";
        EnvBuildInstanceCount envBuildInstanceCount = new EnvBuildInstanceCount("envIden", "envName", "tag", 1);
        AggregationResults<EnvBuildInstanceCount> idAggregationResults = new AggregationResults<>(Arrays.asList(envBuildInstanceCount), new Document());
        when(instanceRepository.getEnvBuildInstanceCountByServiceId(accountIdentifier, orgIdentifier, projectIdentifier, serviceId, timestamp)).thenReturn(idAggregationResults);
        assertThat(instanceService.getEnvBuildInstanceCountByServiceId(accountIdentifier, orgIdentifier, projectIdentifier, serviceId, timestamp)).isEqualTo(idAggregationResults);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getActiveInstancesByServiceIdEnvIdAndBuildIdsTest() {
        String accountIdentifier = "Acc";
        String orgIdentifier = "org";
        long timestamp = 123L;
        String projectIdentifier = "pro";
        String serviceId = "serviceId";
        String envId = "envId";
        int limit = 1;
        List<String> buildIds = Arrays.asList();
        InstancesByBuildId instancesByBuildId = new InstancesByBuildId("buildId", Arrays.asList());
        AggregationResults<InstancesByBuildId> idAggregationResults = new AggregationResults<>(Arrays.asList(instancesByBuildId), new Document());
        when(instanceRepository.getActiveInstancesByServiceIdEnvIdAndBuildIds(accountIdentifier, orgIdentifier, projectIdentifier, serviceId, envId, buildIds, timestamp, limit)).thenReturn(idAggregationResults);
        assertThat(instanceService.getActiveInstancesByServiceIdEnvIdAndBuildIds(accountIdentifier, orgIdentifier, projectIdentifier, serviceId, envId, buildIds, timestamp, limit)).isEqualTo(idAggregationResults);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getActiveServiceInstanceCountBreakdownTest() {
        String accountIdentifier = "Acc";
        String orgIdentifier = "org";
        long timestamp = 123L;
        String projectIdentifier = "pro";
        String serviceId = "serviceId";
        List<String> serviceIdsList = Arrays.asList(serviceId);
        CountByServiceIdAndEnvType countByServiceIdAndEnvType = new CountByServiceIdAndEnvType(serviceId, EnvironmentType.Production, 1);
        AggregationResults<CountByServiceIdAndEnvType> idAggregationResults = new AggregationResults<>(Arrays.asList(countByServiceIdAndEnvType), new Document());
        when(instanceRepository.getActiveServiceInstanceCountBreakdown(accountIdentifier, orgIdentifier, projectIdentifier, serviceIdsList, timestamp)).thenReturn(idAggregationResults);
        assertThat(instanceService.getActiveServiceInstanceCountBreakdown(accountIdentifier, orgIdentifier, projectIdentifier, serviceIdsList, timestamp)).isEqualTo(idAggregationResults);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void findFirstInstanceTest() {
        Criteria criteria = new Criteria();
        InstanceInfo instanceInfo = K8sInstanceInfo.builder().build();
        Instance instance = Instance.builder().instanceInfo(instanceInfo).deletedAt(234L).createdAt(123L).lastModifiedAt(3245L).build();
        when(instanceRepository.findFirstInstance(criteria)).thenReturn(instance);
        assertThat(instanceService.findFirstInstance(criteria).getCreatedAt()).isEqualTo(123L);
    }
}
