/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.service.instancesyncperpetualtask.instancesyncperpetualtaskhandler.helm;

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

import static io.harness.delegate.beans.NgSetupFields.NG;
import static io.harness.delegate.beans.NgSetupFields.OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;
import static org.assertj.core.api.Assertions.assertThat;

@OwnedBy(HarnessTeam.CDP)
public class NativeHelmInstanceSyncPerpetualTaskHandlerTest extends InstancesTestBase {


    private static final String POD_NAME = "podName";
    private static final String BLUE_GREEN_COLOR = "blueGreenColor";
    private static final String NAMESPACE = "namespace";
    private static final String POD_IP = "podIP";
    private static final String RELEASE_NAME = "releaseName";
    private static final String HELM_INSTANCE_SYNC_COMMAND_NAME = "Instance Sync";
    private static final HelmChartInfo HELM_CHART_INFO = HelmChartInfo.builder().build();
    private static final HelmVersion HELM_VERSION = HelmVersion.V3;

    private static final String K8S_INSTANCE_SYNC_COMMAND_NAME = "Instance Sync";
    private static final int DEFAULT_TIMEOUT_IN_MIN = 10;
    private static final String DEFAULT_NAMESPACE = "default";

    private static final String PROJECT_IDENTIFIER = "project";
    private static final String ACCOUNT_IDENTIFIER = "account";
    private static final String ORG_IDENTIFIER = "org";

    @Mock K8sEntityHelper k8sEntityHelper;
    @Mock KryoSerializer kryoSerializer;
    @InjectMocks private NativeHelmInstanceSyncPerpetualTaskHandler nativeHelmInstanceSyncPerpetualTaskHandler;

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void testGetExecutionBundle() throws InvalidProtocolBufferException {

        InfrastructureMappingDTO infrastructureMappingDTO = InfrastructureMappingDTO.builder().projectIdentifier(PROJECT_IDENTIFIER)
                .orgIdentifier(ORG_IDENTIFIER).accountIdentifier(ACCOUNT_IDENTIFIER).infrastructureKind("KUBERNETES_DIRECT").connectorRef("connector").
                envIdentifier("env").serviceIdentifier("service").infrastructureKey("key").build();
        LinkedHashSet<String> namespaces = new LinkedHashSet<>();
        namespaces.add(NAMESPACE);
        DeploymentInfoDTO deploymentInfoDTO = NativeHelmDeploymentInfoDTO.builder().namespaces(namespaces).releaseName(RELEASE_NAME).helmVersion(HELM_VERSION).helmChartInfo(HELM_CHART_INFO).build();
        List<DeploymentInfoDTO> deploymentInfoDTOList = Arrays.asList(deploymentInfoDTO);
        InfrastructureOutcome infrastructureOutcome = K8sDirectInfrastructureOutcome.builder().build();
        KubernetesCredentialDTO kubernetesCredentialDTO = KubernetesCredentialDTO.builder().kubernetesCredentialType(KubernetesCredentialType.INHERIT_FROM_DELEGATE).build();
        KubernetesClusterConfigDTO kubernetesClusterConfigDTO = KubernetesClusterConfigDTO.builder().credential(kubernetesCredentialDTO).build();
        K8sInfraDelegateConfig k8sInfraDelegateConfig = DirectK8sInfraDelegateConfig.builder().kubernetesClusterConfigDTO(kubernetesClusterConfigDTO).build();
        byte[] bytes = {70};
        byte[] bytes2 = {71};
        byte[] bytes3 = {72};
        BaseNGAccess baseNGAccess = BaseNGAccess.builder().accountIdentifier(ACCOUNT_IDENTIFIER).orgIdentifier(ORG_IDENTIFIER).projectIdentifier(PROJECT_IDENTIFIER).build();
        NativeHelmDeploymentReleaseData nativeHelmDeploymentReleaseData = NativeHelmDeploymentReleaseData.builder().k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                .namespaces(((NativeHelmDeploymentInfoDTO) deploymentInfoDTO).getNamespaces())
                .releaseName(((NativeHelmDeploymentInfoDTO) deploymentInfoDTO).getReleaseName())
                .helmChartInfo(((NativeHelmDeploymentInfoDTO) deploymentInfoDTO).getHelmChartInfo())
                .build();

       NativeHelmDeploymentRelease nativeHelmDeploymentRelease = NativeHelmDeploymentRelease.newBuilder()
                .setReleaseName(nativeHelmDeploymentReleaseData.getReleaseName())
                .addAllNamespaces(nativeHelmDeploymentReleaseData.getNamespaces())
                .setK8SInfraDelegateConfig(ByteString.copyFrom(bytes))
                .setHelmChartInfo(ByteString.copyFrom(bytes2))
                .build();
        List<NativeHelmDeploymentRelease> nativeHelmDeploymentReleaseList = Arrays.asList(nativeHelmDeploymentRelease);
        NativeHelmInstanceSyncPerpetualTaskParams nativeHelmInstanceSyncPerpetualTaskParams = NativeHelmInstanceSyncPerpetualTaskParams.newBuilder()
                .setAccountId(ACCOUNT_IDENTIFIER)
                .setHelmVersion(HELM_VERSION.toString())
                .addAllDeploymentReleaseList(nativeHelmDeploymentReleaseList)
                .build();
        Any perpetualTaskPack = Any.pack(nativeHelmInstanceSyncPerpetualTaskParams);
        HelmInstanceSyncRequest helmInstanceSyncRequest = HelmInstanceSyncRequest.builder()
                .k8sInfraDelegateConfig(nativeHelmDeploymentReleaseData.getK8sInfraDelegateConfig())
                .commandName(HELM_INSTANCE_SYNC_COMMAND_NAME)
                .build();
        List<ExecutionCapability> expectedExecutionCapabilityList = helmInstanceSyncRequest.fetchRequiredExecutionCapabilities(null);
        expectedExecutionCapabilityList.add(HelmInstallationCapability.builder()
                .version(HELM_VERSION)
                .criteria(String.format("Helm %s Installed", HELM_VERSION))
                .build());

        when(k8sEntityHelper.getK8sInfraDelegateConfig(infrastructureOutcome, baseNGAccess)).thenReturn(k8sInfraDelegateConfig);
        when(kryoSerializer.asBytes(nativeHelmDeploymentReleaseData.getK8sInfraDelegateConfig())).thenReturn(bytes);
        when(kryoSerializer.asBytes(nativeHelmDeploymentReleaseData.getHelmChartInfo())).thenReturn(bytes2);
        when(kryoSerializer.asDeflatedBytes(expectedExecutionCapabilityList.get(0))).thenReturn(bytes3);

        PerpetualTaskExecutionBundle.Builder builder = PerpetualTaskExecutionBundle.newBuilder();
        expectedExecutionCapabilityList.forEach(executionCapability
                -> builder
                .addCapabilities(
                        Capability.newBuilder()
                                .setKryoCapability(ByteString.copyFrom(bytes3))
                                .build())
                .build());
        PerpetualTaskExecutionBundle expectedPerpetualTaskExecutionBundle = builder.setTaskParams(perpetualTaskPack)
                .putAllSetupAbstractions(Maps.of(NG, "true", OWNER, ORG_IDENTIFIER + "/" + PROJECT_IDENTIFIER))
                .build();

        PerpetualTaskExecutionBundle perpetualTaskExecutionBundle = nativeHelmInstanceSyncPerpetualTaskHandler.getExecutionBundle(infrastructureMappingDTO, deploymentInfoDTOList, infrastructureOutcome);
        assertThat(perpetualTaskExecutionBundle).isEqualTo(expectedPerpetualTaskExecutionBundle);
//        PerpetualTaskExecutionBundle perpetualTaskExecutionBundle = PerpetualTaskExecutionBundle.newBuilder().addCapabilities(
//        Capability.newBuilder()
//                .setKryoCapability(ByteString.copyFrom(bytes2)).build()).setTaskParams(perpetualTaskPack)
//                .putAllSetupAbstractions(Maps.of(NG, "true", OWNER, ORG_IDENTIFIER + "/" + PROJECT_IDENTIFIER)).build();
//        PerpetualTaskExecutionBundle perpetualTaskExecutionBundle = nativeHelmInstanceSyncPerpetualTaskHandler.getExecutionBundle(infrastructureMappingDTO, deploymentInfoDTOList, infrastructureOutcome);
//        List<Capability> executionCapabilityList = perpetualTaskExecutionBundle.getCapabilitiesList();
//        assertThat(executionCapabilityList.size()).isEqualTo(1);
//        Any perpetualTaskPack = perpetualTaskExecutionBundle.getTaskParams();
//        NativeHelmInstanceSyncPerpetualTaskParams nativeHelmInstanceSyncPerpetualTaskParams = perpetualTaskPack.unpack(NativeHelmInstanceSyncPerpetualTaskParams.class);
//        assertThat(nativeHelmInstanceSyncPerpetualTaskParams.getAccountId()).isEqualTo(ACCOUNT_IDENTIFIER);
//        assertThat(nativeHelmInstanceSyncPerpetualTaskParams.getHelmVersion()).isEqualTo(HELM_VERSION);
//        assertThat(nativeHelmInstanceSyncPerpetualTaskParams.getDeploymentReleaseListCount()).isEqualTo(1);
//        assertThat(nativeHelmInstanceSyncPerpetualTaskParams.getDeploymentReleaseList(0).getReleaseName()).isEqualTo(RELEASE_NAME);
//        assertThat(nativeHelmInstanceSyncPerpetualTaskParams.getDeploymentReleaseList(0).getNamespaces(0)).isEqualTo(NAMESPACE);
//        assertThat(nativeHelmInstanceSyncPerpetualTaskHandler.getExecutionBundle(infrastructureMappingDTO, deploymentInfoDTOList, infrastructureOutcome).getCapabilities(0)).isEqualTo(1);
    }

}
