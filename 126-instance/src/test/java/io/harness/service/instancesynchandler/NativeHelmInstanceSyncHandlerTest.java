/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service.instancesynchandler;

import io.harness.InstancesTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.yaml.InfrastructureKind;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.K8sServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.NativeHelmServerInstanceInfo;
import io.harness.delegate.task.helm.HelmChartInfo;
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
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;
import static org.assertj.core.api.Assertions.assertThat;

@OwnedBy(HarnessTeam.CDP)
public class NativeHelmInstanceSyncHandlerTest extends InstancesTestBase {
    private static final String POD_NAME = "podName";
    private static final String NAMESPACE = "namespace";
    private static final String POD_IP = "podIP";
    private static final String RELEASE_NAME = "releaseName";
    private static final HelmChartInfo HELM_CHART_INFO = HelmChartInfo.builder().build();
    private static final HelmVersion HELM_VERSION = HelmVersion.V3;
    @InjectMocks private NativeHelmInstanceSyncHandler nativeHelmInstanceSyncHandler;

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void testGetInfrastructureMappingType() {
        String infrastructureMappingType = nativeHelmInstanceSyncHandler.getInfrastructureKind();

        assertThat(infrastructureMappingType).isEqualTo(InfrastructureKind.KUBERNETES_DIRECT);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void testGetPerpetualTaskType() {
        String perpetualTaskType = nativeHelmInstanceSyncHandler.getPerpetualTaskType();

        assertThat(perpetualTaskType).isEqualTo(PerpetualTaskType.NATIVE_HELM_INSTANCE_SYNC);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void testGetInstanceType() {
        InstanceType instanceType = nativeHelmInstanceSyncHandler.getInstanceType();

        assertThat(instanceType).isEqualTo(InstanceType.NATIVE_HELM_INSTANCE);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void testGetInstanceInfoForServerInstance() {
        InstanceInfoDTO instanceInfoForServerInstance =
                nativeHelmInstanceSyncHandler.getInstanceInfoForServerInstance(NativeHelmServerInstanceInfo.builder()
                        .podName(POD_NAME)
                        .namespace(NAMESPACE)
                        .ip(POD_IP)
                        .releaseName(RELEASE_NAME)
                        .helmVersion(HELM_VERSION)
                        .helmChartInfo(HELM_CHART_INFO)
                        .build());

        assertThat(instanceInfoForServerInstance).isNotNull();
        assertThat(instanceInfoForServerInstance).isInstanceOf(NativeHelmInstanceInfoDTO.class);
        NativeHelmInstanceInfoDTO nativeHelmInstanceInfoDTO = (NativeHelmInstanceInfoDTO) instanceInfoForServerInstance;
        assertThat(nativeHelmInstanceInfoDTO).isNotNull();
        assertThat(nativeHelmInstanceInfoDTO.getPodName()).isEqualTo(POD_NAME);
        assertThat(nativeHelmInstanceInfoDTO.getNamespace()).isEqualTo(NAMESPACE);
        assertThat(nativeHelmInstanceInfoDTO.getIp()).isEqualTo(POD_IP);
        assertThat(nativeHelmInstanceInfoDTO.getReleaseName()).isEqualTo(RELEASE_NAME);
        assertThat(nativeHelmInstanceInfoDTO.getHelmVersion()).isEqualTo(HELM_VERSION);
        assertThat(nativeHelmInstanceInfoDTO.getHelmChartInfo()).isEqualTo(HELM_CHART_INFO);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void testGetDeploymentInfo() {
        K8sDirectInfrastructureOutcome k8sDirectInfrastructureOutcome = K8sDirectInfrastructureOutcome.builder().build();
        DeploymentInfoDTO deploymentInfo =
                nativeHelmInstanceSyncHandler.getDeploymentInfo(k8sDirectInfrastructureOutcome, getServiceInstanceInfos());

        assertThat(deploymentInfo).isNotNull();
        assertThat(deploymentInfo).isInstanceOf(NativeHelmDeploymentInfoDTO.class);
        NativeHelmDeploymentInfoDTO nativeHelmDeploymentInfoDTO = (NativeHelmDeploymentInfoDTO) deploymentInfo;
        assertThat(nativeHelmDeploymentInfoDTO.getHelmChartInfo()).isEqualTo(HELM_CHART_INFO);
        assertThat(nativeHelmDeploymentInfoDTO.getHelmVersion()).isEqualTo(HELM_VERSION);
        assertThat(nativeHelmDeploymentInfoDTO.getReleaseName()).isEqualTo(RELEASE_NAME);
        assertThat(nativeHelmDeploymentInfoDTO.getNamespaces()).contains(NAMESPACE);
    }

    private List<ServerInstanceInfo> getServiceInstanceInfos() {
        return Collections.singletonList(NativeHelmServerInstanceInfo.builder()
                .podName(POD_NAME)
                .namespace(NAMESPACE)
                .releaseName(RELEASE_NAME)
                .helmChartInfo(HELM_CHART_INFO)
                .helmVersion(HELM_VERSION)
                .build());
    }
}
