/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service.instancesyncperpetualtask;

import com.google.protobuf.util.Durations;
import io.harness.InstancesTestBase;
import io.harness.category.element.UnitTests;
import io.harness.delegate.AccountId;
import io.harness.dtos.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoDTO;
import io.harness.entities.instancesyncperpetualtaskinfo.DeploymentInfoDetails;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo;
import io.harness.exception.UnexpectedException;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.mappers.DeploymentInfoDetailsMapper;
import io.harness.models.constants.InstanceSyncConstants;
import io.harness.perpetualtask.PerpetualTaskClientContextDetails;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.repositories.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoInfoRepository;
import io.harness.rule.Owner;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;
import io.harness.service.instancesynchandler.K8sInstanceSyncHandler;
import io.harness.service.instancesyncperpetualtask.instancesyncperpetualtaskhandler.k8s.K8SInstanceSyncPerpetualTaskHandler;
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


public class InstanceSyncPerpetualTaskServiceImplTest extends InstancesTestBase{

    private static final String ACCOUNT_IDENTIFIER = "account_identifier";
    private static final String ORG_IDENTIFIER = "org_identifier";
    private static final String PROJECT_IDENTIFIER = "project_identifier";
    private static final String SERVICE_IDENTIFIER = "service_identifier";
    private static final String ENVIRONMENT_IDENTIFIER = "environment_identifier";
    private static final String INFRASTRUCTURE_IDENTIFIER = "infrastructure_identifier";
    private static final String INFRASTRUCTURE_KIND = "infrastructure_kind";
    private static final String CONNECTOR_REF = "connector_ref";

    @Mock DelegateServiceGrpcClient delegateServiceGrpcClient;
    @Mock AbstractInstanceSyncHandler abstractInstanceSyncHandler;
    @Mock NativeHelmInstanceSyncPerpetualTaskHandler nativeHelmInstanceSyncPerpetualTaskHandler;
    @Mock K8SInstanceSyncPerpetualTaskHandler k8SInstanceSyncPerpetualTaskHandler;
    @InjectMocks InstanceSyncPerpetualTaskServiceRegister perpetualTaskServiceRegister;

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void createPerpetualTaskTest() {

        InstanceSyncPerpetualTaskServiceImpl instanceSyncPerpetualTaskService = new InstanceSyncPerpetualTaskServiceImpl(delegateServiceGrpcClient, perpetualTaskServiceRegister);
        InfrastructureMappingDTO infrastructureMappingDTO = InfrastructureMappingDTO.builder()
                .orgIdentifier(ORG_IDENTIFIER).accountIdentifier(ACCOUNT_IDENTIFIER)
                .projectIdentifier(PROJECT_IDENTIFIER).serviceIdentifier(SERVICE_IDENTIFIER)
                .envIdentifier(ENVIRONMENT_IDENTIFIER).infrastructureKey(INFRASTRUCTURE_IDENTIFIER)
                .infrastructureKind(INFRASTRUCTURE_KIND).connectorRef(CONNECTOR_REF).build();
        List<DeploymentInfoDTO> deploymentInfoDTOList = Arrays.asList();
        InfrastructureOutcome infrastructureOutcome = K8sDirectInfrastructureOutcome.builder().build();
        PerpetualTaskSchedule perpetualTaskSchedule = PerpetualTaskSchedule.newBuilder()
                .setInterval(Durations.fromMinutes(InstanceSyncConstants.INTERVAL_MINUTES))
                .setTimeout(Durations.fromSeconds(InstanceSyncConstants.TIMEOUT_SECONDS))
                .build();
        when(abstractInstanceSyncHandler.getPerpetualTaskType()).thenReturn(PerpetualTaskType.K8S_INSTANCE_SYNC);
        PerpetualTaskExecutionBundle perpetualTaskExecutionBundle = PerpetualTaskExecutionBundle.newBuilder().build();
        when(k8SInstanceSyncPerpetualTaskHandler.getExecutionBundle(infrastructureMappingDTO, deploymentInfoDTOList, infrastructureOutcome)).thenReturn(perpetualTaskExecutionBundle);
        PerpetualTaskId perpetualTaskId = PerpetualTaskId.newBuilder().setId("id").build();
        when(delegateServiceGrpcClient.createPerpetualTask(
                AccountId.newBuilder().setId(infrastructureMappingDTO.getAccountIdentifier()).build(),
                abstractInstanceSyncHandler.getPerpetualTaskType(), perpetualTaskSchedule,
                PerpetualTaskClientContextDetails.newBuilder().setExecutionBundle(perpetualTaskExecutionBundle).build(), true,
                String.format(
                        "OrgIdentifier: [%s], ProjectIdentifier: [%s], ServiceIdentifier: [%s], EnvironmentIdentifier: [%s], InfrastructureKey: [%s]",
                        infrastructureMappingDTO.getOrgIdentifier(), infrastructureMappingDTO.getProjectIdentifier(),
                        infrastructureMappingDTO.getServiceIdentifier(), infrastructureMappingDTO.getEnvIdentifier(),
                        infrastructureMappingDTO.getInfrastructureKey()))).thenReturn(perpetualTaskId);
        assertThat(instanceSyncPerpetualTaskService.createPerpetualTask(infrastructureMappingDTO, abstractInstanceSyncHandler, deploymentInfoDTOList, infrastructureOutcome)).isEqualTo(perpetualTaskId.getId());
    }
}
