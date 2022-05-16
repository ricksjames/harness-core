/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service.instancesyncperpetualtaskinfo;

import io.harness.InstancesTestBase;
import io.harness.category.element.UnitTests;
import io.harness.dtos.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoDTO;
import io.harness.entities.instancesyncperpetualtaskinfo.DeploymentInfoDetails;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo;
import io.harness.mappers.DeploymentInfoDetailsMapper;
import io.harness.repositories.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoInfoRepository;
import io.harness.rule.Owner;
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

public class InstanceSyncPerpetualTaskInfoServiceImplTest  extends InstancesTestBase {

    private static final String ID = "id";
    private static final String ACCOUNT_IDENTIFIER = "iden";
    private static final String INFRASTRUCTURE_MAPPING_ID = "infra";
    private DeploymentInfoDetails deploymentInfoDetails = DeploymentInfoDetails.builder().build();
    private List<DeploymentInfoDetails> deploymentInfoDetailsList = Arrays.asList();
    private static final String PERPETUAL_TASK_ID = "taskId";
    private static final long CREATED_AT = 1323;
    private static final long LAST_UPDATED_AT = 1324;
    @Mock InstanceSyncPerpetualTaskInfoInfoRepository instanceSyncPerpetualTaskInfoInfoRepository;
    @InjectMocks InstanceSyncPerpetualTaskInfoServiceImpl instanceSyncPerpetualTaskInfoService;

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void findByInfrastructureMappingIdTest() {

        InstanceSyncPerpetualTaskInfo instanceSyncPerpetualTaskInfo = InstanceSyncPerpetualTaskInfo.builder()
                .id(ID).accountIdentifier(ACCOUNT_IDENTIFIER).infrastructureMappingId(INFRASTRUCTURE_MAPPING_ID)
                .deploymentInfoDetailsList(deploymentInfoDetailsList).perpetualTaskId(PERPETUAL_TASK_ID)
                .createdAt(CREATED_AT).lastUpdatedAt(LAST_UPDATED_AT).build();
        when(instanceSyncPerpetualTaskInfoInfoRepository.findByInfrastructureMappingId(INFRASTRUCTURE_MAPPING_ID)).thenReturn(Optional.of(instanceSyncPerpetualTaskInfo));
        InstanceSyncPerpetualTaskInfoDTO instanceSyncPerpetualTaskInfoDTO = InstanceSyncPerpetualTaskInfoDTO.builder()
                .id(ID).accountIdentifier(ACCOUNT_IDENTIFIER).infrastructureMappingId(INFRASTRUCTURE_MAPPING_ID)
                .deploymentInfoDetailsDTOList(Arrays.asList()).perpetualTaskId(PERPETUAL_TASK_ID)
                .createdAt(CREATED_AT).lastUpdatedAt(LAST_UPDATED_AT).build();
        assertThat(instanceSyncPerpetualTaskInfoService.findByInfrastructureMappingId(INFRASTRUCTURE_MAPPING_ID)).isEqualTo(Optional.of(instanceSyncPerpetualTaskInfoDTO));
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void findByPerpetualTaskIdTest() {

        InstanceSyncPerpetualTaskInfo instanceSyncPerpetualTaskInfo = InstanceSyncPerpetualTaskInfo.builder()
                .id(ID).accountIdentifier(ACCOUNT_IDENTIFIER).infrastructureMappingId(INFRASTRUCTURE_MAPPING_ID)
                .deploymentInfoDetailsList(deploymentInfoDetailsList).perpetualTaskId(PERPETUAL_TASK_ID)
                .createdAt(CREATED_AT).lastUpdatedAt(LAST_UPDATED_AT).build();
        when(instanceSyncPerpetualTaskInfoInfoRepository.findByAccountIdentifierAndPerpetualTaskId(ACCOUNT_IDENTIFIER, PERPETUAL_TASK_ID)).thenReturn(Optional.of(instanceSyncPerpetualTaskInfo));
        InstanceSyncPerpetualTaskInfoDTO instanceSyncPerpetualTaskInfoDTO = InstanceSyncPerpetualTaskInfoDTO.builder()
                .id(ID).accountIdentifier(ACCOUNT_IDENTIFIER).infrastructureMappingId(INFRASTRUCTURE_MAPPING_ID)
                .deploymentInfoDetailsDTOList(Arrays.asList()).perpetualTaskId(PERPETUAL_TASK_ID)
                .createdAt(CREATED_AT).lastUpdatedAt(LAST_UPDATED_AT).build();
        assertThat(instanceSyncPerpetualTaskInfoService.findByPerpetualTaskId(ACCOUNT_IDENTIFIER, PERPETUAL_TASK_ID)).isEqualTo(Optional.of(instanceSyncPerpetualTaskInfoDTO));
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void saveTest() {

        InstanceSyncPerpetualTaskInfoDTO instanceSyncPerpetualTaskInfoDTO = InstanceSyncPerpetualTaskInfoDTO.builder()
                .id(ID).accountIdentifier(ACCOUNT_IDENTIFIER).infrastructureMappingId(INFRASTRUCTURE_MAPPING_ID)
                .deploymentInfoDetailsDTOList(Arrays.asList()).perpetualTaskId(PERPETUAL_TASK_ID).build();
        InstanceSyncPerpetualTaskInfo instanceSyncPerpetualTaskInfo = InstanceSyncPerpetualTaskInfo.builder()
                .id(ID).accountIdentifier(ACCOUNT_IDENTIFIER).infrastructureMappingId(INFRASTRUCTURE_MAPPING_ID)
                .deploymentInfoDetailsList(deploymentInfoDetailsList).perpetualTaskId(PERPETUAL_TASK_ID).build();
        when(instanceSyncPerpetualTaskInfoInfoRepository.save(instanceSyncPerpetualTaskInfo)).thenReturn(instanceSyncPerpetualTaskInfo);
        assertThat(instanceSyncPerpetualTaskInfoService.save(instanceSyncPerpetualTaskInfoDTO)).isEqualTo(instanceSyncPerpetualTaskInfoDTO);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void deleteByIdTest() {

        instanceSyncPerpetualTaskInfoService.deleteById(ACCOUNT_IDENTIFIER, ID);
        verify(instanceSyncPerpetualTaskInfoInfoRepository, times(1)).deleteByAccountIdentifierAndId(ACCOUNT_IDENTIFIER, ID);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void updateDeploymentInfoDetailsListTest() {
        InstanceSyncPerpetualTaskInfoDTO instanceSyncPerpetualTaskInfoDTO = InstanceSyncPerpetualTaskInfoDTO.builder()
                .id(ID).accountIdentifier(ACCOUNT_IDENTIFIER).infrastructureMappingId(INFRASTRUCTURE_MAPPING_ID)
                .deploymentInfoDetailsDTOList(Arrays.asList()).perpetualTaskId(PERPETUAL_TASK_ID).build();
        InstanceSyncPerpetualTaskInfo instanceSyncPerpetualTaskInfo = InstanceSyncPerpetualTaskInfo.builder()
                .id(ID).accountIdentifier(ACCOUNT_IDENTIFIER).infrastructureMappingId(INFRASTRUCTURE_MAPPING_ID)
                .deploymentInfoDetailsList(deploymentInfoDetailsList).perpetualTaskId(PERPETUAL_TASK_ID).build();
        Criteria criteria = Criteria.where(InstanceSyncPerpetualTaskInfo.InstanceSyncPerpetualTaskInfoKeys.accountIdentifier)
                .is(instanceSyncPerpetualTaskInfoDTO.getAccountIdentifier())
                .and(InstanceSyncPerpetualTaskInfo.InstanceSyncPerpetualTaskInfoKeys.id)
                .is(instanceSyncPerpetualTaskInfoDTO.getId());
        Update update = new Update().set(InstanceSyncPerpetualTaskInfo.InstanceSyncPerpetualTaskInfoKeys.deploymentInfoDetailsList,
                DeploymentInfoDetailsMapper.toDeploymentInfoDetailsEntityList(
                        instanceSyncPerpetualTaskInfoDTO.getDeploymentInfoDetailsDTOList()));
        when(instanceSyncPerpetualTaskInfoInfoRepository.update(criteria, update)).thenReturn(instanceSyncPerpetualTaskInfo);
        assertThat(instanceSyncPerpetualTaskInfoService.updateDeploymentInfoDetailsList(instanceSyncPerpetualTaskInfoDTO)).isEqualTo(instanceSyncPerpetualTaskInfoDTO);
    }
}
