/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.services.impl;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.DHRUVX;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.cdng.beans.MonitoredServiceNode;
import io.harness.cvng.cdng.beans.MonitoredServiceSpec.MonitoredServiceSpecType;
import io.harness.cvng.cdng.beans.TemplateMonitoredServiceSpec;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask;
import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask.MonitoringSourcePerpetualTaskKeys;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.query.Query;

public class TemplateVerifyStepMonitoredServiceResolutionServiceImplTest extends CvNextGenTestBase {
  @Inject private MonitoredServiceService monitoredServiceService;
  @Inject private TemplateVerifyStepMonitoredServiceResolutionServiceImpl templateService;
  @Inject private MetricPackService metricPackService;
  @Inject HPersistence hPersistence;
  private MonitoredServiceService mockMonitoredServiceService;
  private BuilderFactory builderFactory;
  private String accountId;
  private String projectIdentifier;
  private String orgIdentifier;
  private String serviceIdentifier;
  private String envIdentifier;
  private MonitoredServiceNode monitoredServiceNode;
  private TemplateMonitoredServiceSpec templateMonitoredServiceSpec;
  private MonitoredServiceDTO monitoredServiceDTO;
  private ServiceEnvironmentParams serviceEnvironmentParams;

  @Before
  public void setup() throws IllegalAccessException {
    mockMonitoredServiceService = mock(MonitoredServiceService.class);
    builderFactory = BuilderFactory.getDefault();
    accountId = builderFactory.getContext().getAccountId();
    projectIdentifier = builderFactory.getContext().getProjectIdentifier();
    orgIdentifier = builderFactory.getContext().getOrgIdentifier();
    serviceIdentifier = builderFactory.getContext().getServiceIdentifier();
    envIdentifier = builderFactory.getContext().getEnvIdentifier();
    monitoredServiceDTO = builderFactory.monitoredServiceDTOBuilder().build();
    templateMonitoredServiceSpec = builderFactory.getTemplateMonitoredServiceSpecBuilder().build();
    monitoredServiceNode = getDefaultMonitoredServiceNode();
    serviceEnvironmentParams = getServiceEnvironmentParams();
    metricPackService.createDefaultMetricPackAndThresholds(accountId, orgIdentifier, projectIdentifier);
    FieldUtils.writeField(templateService, "monitoredServiceService", mockMonitoredServiceService, true);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testGetMonitoredServiceIdentifier() {
    String actualIdentifier = templateService.getResolvedCVConfigInfo(serviceEnvironmentParams, monitoredServiceNode)
                                  .getMonitoredServiceIdentifier();
    assertThat(actualIdentifier).isNull();
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testGetCVConfigs() {
    when(mockMonitoredServiceService.getExpandedMonitoredServiceFromYaml(any(), any())).thenReturn(monitoredServiceDTO);
    List<CVConfig> actualCvConfigs =
        templateService.getResolvedCVConfigInfo(serviceEnvironmentParams, monitoredServiceNode).getCvConfigs();
    assertThat(actualCvConfigs).hasSize(1);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testGetCVConfigs_monitoredServiceDtoDoesNotExist() {
    when(mockMonitoredServiceService.getExpandedMonitoredServiceFromYaml(any(), any())).thenReturn(null);
    List<CVConfig> actualCvConfigs =
        templateService.getResolvedCVConfigInfo(serviceEnvironmentParams, monitoredServiceNode).getCvConfigs();
    assertThat(actualCvConfigs).hasSize(0);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testGetCVConfigs_healthSourcesDoNotExist() {
    monitoredServiceDTO.getSources().setHealthSources(Collections.emptySet());
    when(mockMonitoredServiceService.getExpandedMonitoredServiceFromYaml(any(), any())).thenReturn(monitoredServiceDTO);
    List<CVConfig> actualCvConfigs =
        templateService.getResolvedCVConfigInfo(serviceEnvironmentParams, monitoredServiceNode).getCvConfigs();
    assertThat(actualCvConfigs).hasSize(0);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testGetCVConfigs_SourcesDoNotExist() {
    monitoredServiceDTO.setSources(null);
    when(mockMonitoredServiceService.getExpandedMonitoredServiceFromYaml(any(), any())).thenReturn(monitoredServiceDTO);
    List<CVConfig> actualCvConfigs =
        templateService.getResolvedCVConfigInfo(serviceEnvironmentParams, monitoredServiceNode).getCvConfigs();
    assertThat(actualCvConfigs).hasSize(0);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testGetCVConfigs_verifyPerpetualTasksGotCreated() {
    when(mockMonitoredServiceService.getExpandedMonitoredServiceFromYaml(any(), any())).thenReturn(monitoredServiceDTO);
    List<CVConfig> actualCvConfigs =
        templateService.getResolvedCVConfigInfo(serviceEnvironmentParams, monitoredServiceNode).getCvConfigs();
    assertThat(actualCvConfigs).hasSize(1);
    Query<MonitoringSourcePerpetualTask> query =
        hPersistence.createQuery(MonitoringSourcePerpetualTask.class, excludeAuthority)
            .filter(MonitoringSourcePerpetualTaskKeys.accountId, accountId)
            .filter(MonitoringSourcePerpetualTaskKeys.projectIdentifier, projectIdentifier)
            .filter(MonitoringSourcePerpetualTaskKeys.orgIdentifier, orgIdentifier);
    List<MonitoringSourcePerpetualTask> savedPerpetualTasks = query.asList();
    assertThat(savedPerpetualTasks).hasSize(2);
  }

  private MonitoredServiceNode getDefaultMonitoredServiceNode() {
    MonitoredServiceNode monitoredServiceNode = new MonitoredServiceNode();
    monitoredServiceNode.setSpec(templateMonitoredServiceSpec);
    monitoredServiceNode.setType(MonitoredServiceSpecType.TEMPLATE.name());
    return monitoredServiceNode;
  }

  private ServiceEnvironmentParams getServiceEnvironmentParams() {
    return ServiceEnvironmentParams.builder()
        .serviceIdentifier(serviceIdentifier)
        .environmentIdentifier(envIdentifier)
        .orgIdentifier(orgIdentifier)
        .accountIdentifier(accountId)
        .projectIdentifier(projectIdentifier)
        .build();
  }
}
