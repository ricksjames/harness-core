/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service.instancesync;

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
import io.harness.helper.InstanceSyncHelper;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.mappers.DeploymentInfoDetailsMapper;
import io.harness.mappers.InstanceDetailsMapper;
import io.harness.models.BuildsByEnvironment;
import io.harness.models.CountByServiceIdAndEnvType;
import io.harness.models.DeploymentEvent;
import io.harness.models.EnvBuildInstanceCount;
import io.harness.models.InstanceDetailsByBuildId;
import io.harness.models.InstanceStats;
import io.harness.models.InstancesByBuildId;
import io.harness.models.RollbackInfo;
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
import io.harness.service.deploymentsummary.DeploymentSummaryService;
import io.harness.service.infrastructuremapping.InfrastructureMappingService;
import io.harness.service.instance.InstanceService;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;
import io.harness.service.instancesynchandler.K8sInstanceSyncHandler;
import io.harness.service.instancesynchandlerfactory.InstanceSyncHandlerFactoryService;
import io.harness.service.instancesyncperpetualtask.InstanceSyncPerpetualTaskService;
import io.harness.service.instancesyncperpetualtask.instancesyncperpetualtaskhandler.k8s.K8SInstanceSyncPerpetualTaskHandler;
import io.harness.service.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoService;
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
import static org.mockito.Mockito.atLeast;
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


public class InstanceSyncServiceImplTest extends InstancesTestBase {

    @Mock AbstractInstanceSyncHandler abstractInstanceSyncHandler;
    @Mock AcquiredLock<?> acquiredLock;
    @Mock PersistentLocker persistentLocker;
    @Mock InstanceSyncPerpetualTaskService instanceSyncPerpetualTaskService;
    @Mock InstanceSyncPerpetualTaskInfoService instanceSyncPerpetualTaskInfoService;
    @Mock InstanceSyncHandlerFactoryService instanceSyncHandlerFactoryService;
    @Mock InfrastructureMappingService infrastructureMappingService;
    @Mock InstanceService instanceService;
    @Mock DeploymentSummaryService deploymentSummaryService;
    @Mock InstanceSyncHelper instanceSyncHelper;
    @InjectMocks InstanceSyncServiceImpl instanceSyncService;

    private static final int NEW_DEPLOYMENT_EVENT_RETRY = 3;
    private static final long TWO_WEEKS_IN_MILLIS = (long) 14 * 24 * 60 * 60 * 1000;
    private final String ACCOUNT_IDENTIFIER = "acc";
    private final String PERPETUAL_TASK = "perp";
    private final String PROJECT_IDENTIFIER =  "proj";
    private final String ORG_IDENTIFIER = "org";
    private final String SERVICE_IDENTIFIER = "serv";
    private final String ENV_IDENTIFIER = "env";
    private final String CONNECTOR_REF = "conn";
    private final String INFRASTRUCTURE_KEY = "key";
    private final String INFRASTRUCTURE_MAPPING_ID = "inframappingid";
    private final String ID = "id";

    enum OperationsOnInstances { ADD, DELETE, UPDATE }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void processInstanceSyncForNewDeploymentTest() {

        InfrastructureMappingDTO infrastructureMappingDTO = InfrastructureMappingDTO.builder().accountIdentifier(ACCOUNT_IDENTIFIER).id(ID).orgIdentifier(ORG_IDENTIFIER)
                .projectIdentifier(PROJECT_IDENTIFIER).envIdentifier(ENV_IDENTIFIER).serviceIdentifier(SERVICE_IDENTIFIER)
                .infrastructureKind(InfrastructureKind.KUBERNETES_DIRECT).connectorRef(CONNECTOR_REF).infrastructureKey(INFRASTRUCTURE_KEY).build();
        DeploymentInfoDTO deploymentInfoDTO = K8sDeploymentInfoDTO.builder().build();
        DeploymentSummaryDTO deploymentSummaryDTO = DeploymentSummaryDTO.builder().instanceSyncKey("sunc").infrastructureMapping(infrastructureMappingDTO).deploymentInfoDTO(deploymentInfoDTO).infrastructureMappingId(INFRASTRUCTURE_MAPPING_ID).build();
        RollbackInfo rollbackInfo = RollbackInfo.builder().build();
        InfrastructureOutcome infrastructureOutcome = K8sDirectInfrastructureOutcome.builder().build();
        DeploymentEvent deploymentEvent = new DeploymentEvent(deploymentSummaryDTO, rollbackInfo, infrastructureOutcome);
        when(persistentLocker.waitToAcquireLock(InstanceSyncConstants.INSTANCE_SYNC_PREFIX + deploymentSummaryDTO.getInfrastructureMappingId(),
                InstanceSyncConstants.INSTANCE_SYNC_LOCK_TIMEOUT, InstanceSyncConstants.INSTANCE_SYNC_WAIT_TIMEOUT)).thenReturn(acquiredLock);
        InstanceSyncPerpetualTaskInfoDTO instanceSyncPerpetualTaskInfoDTO = InstanceSyncPerpetualTaskInfoDTO.builder().build();
        when(instanceSyncPerpetualTaskInfoService.findByInfrastructureMappingId(infrastructureMappingDTO.getId())).thenReturn(Optional.of(instanceSyncPerpetualTaskInfoDTO));
        when(instanceSyncPerpetualTaskService.createPerpetualTask(infrastructureMappingDTO,
                        abstractInstanceSyncHandler, Collections.singletonList(deploymentSummaryDTO.getDeploymentInfoDTO()),
                        deploymentEvent.getInfrastructureOutcome())).thenReturn(PERPETUAL_TASK);
        when(instanceSyncPerpetualTaskInfoService.save(any())).thenReturn(instanceSyncPerpetualTaskInfoDTO);
        when(instanceSyncHandlerFactoryService.getInstanceSyncHandler(deploymentSummaryDTO.getDeploymentInfoDTO().getType())).thenReturn(abstractInstanceSyncHandler);
        instanceSyncService.processInstanceSyncForNewDeployment(deploymentEvent);
        verify(instanceSyncHandlerFactoryService, times(3)).getInstanceSyncHandler(deploymentSummaryDTO.getDeploymentInfoDTO().getType());
    }
}
