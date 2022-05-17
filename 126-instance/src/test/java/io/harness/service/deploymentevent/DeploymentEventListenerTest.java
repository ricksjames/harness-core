/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service.deploymentevent;

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
import io.harness.dtos.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoDTO;
import io.harness.entities.ArtifactDetails;
import io.harness.entities.DeploymentSummary;
import io.harness.entities.InfrastructureMapping;
import io.harness.entities.deploymentinfo.DeploymentInfo;
import io.harness.entities.deploymentinfo.K8sDeploymentInfo;
import io.harness.entities.instancesyncperpetualtaskinfo.DeploymentInfoDetails;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo;
import io.harness.exception.UnexpectedException;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.mappers.DeploymentInfoDetailsMapper;
import io.harness.models.DeploymentEvent;
import io.harness.models.InstanceStats;
import io.harness.models.constants.InstanceSyncConstants;
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
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

import static io.harness.delegate.beans.NgSetupFields.NG;
import static io.harness.delegate.beans.NgSetupFields.OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.in;
import static org.mockito.Matchers.any;
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

public class DeploymentEventListenerTest extends InstancesTestBase {

    private final String ACCOUNT_ID = "acc";
    private final String ORG_IDENTIFIER = "org";
    private final String PROJECT_IDENTIFIER = "proj";
    private final String SERVICE_IDENTIFIER = "serv";
    private final String ENVIRONMENT_IDENTIFIER = "env";
    private final String INFRASTRUCTURE_KEY = "infkey";
    private final String INFRASTRUCTURE_ID = "infraid";
    private final String CONNECTOR_REF = "conn";
    private final String PLAN_EXECUTION_ID = "plan";
    private final String PIPELINE_IDENTIFIER = "pipe";
    private final String TRIGGERED_BY_IDENTIFER = "triggeriden";
    private final String UUID = "dsafergasvfd124";
    private final long START_TS = 1234L;
    private final String RELEASE_NAME = "release";
    @Mock AbstractInstanceSyncHandler abstractInstanceSyncHandler;
    @Mock OutcomeService outcomeService;
    @Mock InstanceSyncHandlerFactoryService instanceSyncHandlerFactoryService;
    @Mock InfrastructureMappingService infrastructureMappingService;
    @Mock InstanceSyncService instanceSyncService;
    @Mock InstanceInfoService instanceInfoService;
    @Mock DeploymentSummaryService deploymentSummaryService;
    @InjectMocks DeploymentEventListener deploymentEventListener;

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void handleEventTest() {

        LinkedHashSet<String> namespaces = new LinkedHashSet<>();
        namespaces.add("namespace1");
        StepType stepType = StepType.newBuilder().build();
        Level level = Level.newBuilder().setStepType(stepType).setStartTs(START_TS).build();
        TriggeredBy triggeredBy = TriggeredBy.newBuilder().setIdentifier(TRIGGERED_BY_IDENTIFER).setUuid(UUID).build();
        ExecutionTriggerInfo triggerInfo = ExecutionTriggerInfo.newBuilder().setTriggeredBy(triggeredBy).build();
        ExecutionMetadata executionMetadata = ExecutionMetadata.newBuilder().setPipelineIdentifier(PIPELINE_IDENTIFIER)
                .setTriggerInfo(triggerInfo).build();
        Ambiance ambiance = Ambiance.newBuilder().setMetadata(executionMetadata).addLevels(level).setPlanExecutionId(PLAN_EXECUTION_ID).putSetupAbstractions(SetupAbstractionKeys.accountId, ACCOUNT_ID)
                .putSetupAbstractions(SetupAbstractionKeys.orgIdentifier, ORG_IDENTIFIER).putSetupAbstractions(SetupAbstractionKeys.projectIdentifier, PROJECT_IDENTIFIER).build();
        OrchestrationEvent orchestrationEvent = OrchestrationEvent.builder().status(Status.SUCCEEDED)
                .ambiance(ambiance).build();
        ServerInstanceInfo serverInstanceInfo = K8sServerInstanceInfo.builder().build();
        ServiceStepOutcome serviceStepOutcome = ServiceStepOutcome.builder().identifier(SERVICE_IDENTIFIER).build();
        EnvironmentOutcome environmentOutcome = EnvironmentOutcome.builder().identifier(ENVIRONMENT_IDENTIFIER).build();
        InfrastructureOutcome infrastructureOutcome = K8sDirectInfrastructureOutcome.builder().infrastructureKey(INFRASTRUCTURE_KEY)
                .environment(environmentOutcome).connectorRef(CONNECTOR_REF).build();
        when(instanceInfoService.listServerInstances(ambiance, stepType)).thenReturn(Arrays.asList(serverInstanceInfo));
        when(outcomeService.resolve(ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE))).thenReturn(serviceStepOutcome);
        when(outcomeService.resolve(ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME))).thenReturn(infrastructureOutcome);

        InfrastructureMappingDTO infrastructureMappingDTO =
                InfrastructureMappingDTO.builder()
                        .id(INFRASTRUCTURE_ID)
                        .orgIdentifier(ORG_IDENTIFIER)
                        .accountIdentifier(ACCOUNT_ID)
                        .projectIdentifier(PROJECT_IDENTIFIER)
                        .serviceIdentifier(SERVICE_IDENTIFIER)
                        .envIdentifier(ENVIRONMENT_IDENTIFIER)
                        .infrastructureKey(INFRASTRUCTURE_KEY)
                        .infrastructureKind(infrastructureOutcome.getKind())
                        .connectorRef(CONNECTOR_REF)
                        .build();
        final ArgumentCaptor<InfrastructureMappingDTO> captor = ArgumentCaptor.forClass(InfrastructureMappingDTO.class);
        when(infrastructureMappingService.createNewOrReturnExistingInfrastructureMapping(any())).thenReturn(Optional.of(infrastructureMappingDTO));
        when(instanceSyncHandlerFactoryService.getInstanceSyncHandler(serviceStepOutcome.getType())).thenReturn(abstractInstanceSyncHandler);
        DeploymentInfoDTO deploymentInfoDTO = K8sDeploymentInfoDTO.builder().releaseName(RELEASE_NAME).namespaces(namespaces).build();
        when(abstractInstanceSyncHandler.getDeploymentInfo(infrastructureOutcome, Arrays.asList(serverInstanceInfo))).thenReturn(deploymentInfoDTO);

        DeploymentSummaryDTO deploymentSummaryDTO =
                DeploymentSummaryDTO.builder()
                        .accountIdentifier(ACCOUNT_ID)
                        .orgIdentifier(ORG_IDENTIFIER)
                        .projectIdentifier(PROJECT_IDENTIFIER)
                        .pipelineExecutionId(PLAN_EXECUTION_ID)
                        .pipelineExecutionName(PIPELINE_IDENTIFIER)
                        .deployedByName(TRIGGERED_BY_IDENTIFER)
                        .deployedById(UUID)
                        .infrastructureMappingId(INFRASTRUCTURE_ID)
                        .instanceSyncKey(deploymentInfoDTO.prepareInstanceSyncHandlerKey())
                        .deploymentInfoDTO(deploymentInfoDTO)
                        .deployedAt(START_TS)
                        .build();

        DeploymentSummaryDTO deploymentSummaryDTO1 =
                DeploymentSummaryDTO.builder()
                        .accountIdentifier(ACCOUNT_ID)
                        .orgIdentifier(ORG_IDENTIFIER)
                        .projectIdentifier(PROJECT_IDENTIFIER)
                        .pipelineExecutionId(PLAN_EXECUTION_ID)
                        .pipelineExecutionName(PIPELINE_IDENTIFIER)
                        .deployedByName(TRIGGERED_BY_IDENTIFER)
                        .deployedById(UUID)
                        .infrastructureMappingId(INFRASTRUCTURE_ID)
                        .instanceSyncKey(deploymentInfoDTO.prepareInstanceSyncHandlerKey())
                        .deploymentInfoDTO(deploymentInfoDTO)
                        .deployedAt(START_TS)
                        .build();

        OptionalOutcome optionalOutcome = OptionalOutcome.builder().found(false).build();
        when(outcomeService.resolveOptional(
                ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.ARTIFACTS))).thenReturn(optionalOutcome);
        deploymentSummaryDTO.setArtifactDetails(ArtifactDetails.builder().artifactId("").tag("").build());
        when(deploymentSummaryService.save(any())).thenReturn(deploymentSummaryDTO1);
        deploymentSummaryDTO1.setServerInstanceInfoList(Arrays.asList(serverInstanceInfo));
        deploymentSummaryDTO1.setInfrastructureMapping(infrastructureMappingDTO);

        deploymentEventListener.handleEvent(orchestrationEvent);

        final ArgumentCaptor<DeploymentSummaryDTO> deploymentSummaryDTOArgumentCaptor = ArgumentCaptor.forClass(DeploymentSummaryDTO.class);
        verify(deploymentSummaryService, times(1)).save(deploymentSummaryDTOArgumentCaptor.capture());
        DeploymentSummaryDTO actualDeploymentSummaryDTO = deploymentSummaryDTOArgumentCaptor.getValue();
        assertThat(actualDeploymentSummaryDTO.getAccountIdentifier()).isEqualTo(ACCOUNT_ID);
        verify(outcomeService, times(1)).resolveOptional(ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.ARTIFACTS));
        verify(infrastructureMappingService, times(1)).createNewOrReturnExistingInfrastructureMapping(captor.capture());
        InfrastructureMappingDTO actualMappingDTO = captor.getValue();
        assertThat(actualMappingDTO.getConnectorRef()).isEqualTo(infrastructureMappingDTO.getConnectorRef());
        verify(abstractInstanceSyncHandler, times(1)).getDeploymentInfo(infrastructureOutcome, Arrays.asList(serverInstanceInfo));
        verify(instanceSyncService, times(1)).processInstanceSyncForNewDeployment(
                new DeploymentEvent(deploymentSummaryDTO1, null, infrastructureOutcome));
    }
}
