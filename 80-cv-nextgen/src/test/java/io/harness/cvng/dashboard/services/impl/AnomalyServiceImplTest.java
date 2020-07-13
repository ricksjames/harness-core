package io.harness.cvng.dashboard.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.PERFORMANCE_PACK_IDENTIFIER;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.CVNextGenBaseTest;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.dashboard.entities.Anomaly;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalousMetric;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalyDetail;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalyStatus;
import io.harness.cvng.dashboard.services.api.AnomalyService;
import io.harness.data.structure.CollectionUtils;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class AnomalyServiceImplTest extends CVNextGenBaseTest {
  @Inject private AnomalyService anomalyService;
  @Inject private MetricPackService metricPackService;
  @Inject private CVConfigService cvConfigService;

  private String projectIdentifier;
  private String serviceIdentifier;
  private String envIdentifier;
  private String accountId;
  @Inject private HPersistence hPersistence;

  @Before
  public void setUp() {
    projectIdentifier = generateUuid();
    serviceIdentifier = generateUuid();
    envIdentifier = generateUuid();
    accountId = generateUuid();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testOpenAndCloseanomaly_withSingleCvConfig() {
    Instant instant = Instant.now();
    CVConfig cvConfig = createAndSaveAppDConfig(CVMonitoringCategory.PERFORMANCE);
    List<AnomalousMetric> anomalousMetrics = getAnomalousMetrics(10);

    // open anomaly multiple times
    for (int i = 0; i < 5; i++) {
      anomalyService.openAnomaly(accountId, cvConfig.getUuid(), instant, anomalousMetrics);
    }
    List<Anomaly> anomalies = hPersistence.createQuery(Anomaly.class, excludeAuthority).asList();
    assertThat(anomalies.size()).isEqualTo(1);
    Anomaly anomaly = anomalies.get(0);
    assertThat(anomaly.getAccountId()).isEqualTo(accountId);
    assertThat(anomaly.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(anomaly.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(anomaly.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(anomaly.getCategory()).isEqualTo(cvConfig.getCategory());
    assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.OPEN);
    assertThat(anomaly.getAnomalyStartTime()).isEqualTo(instant);
    assertThat(anomaly.getAnomalyEndTime()).isNull();
    assertThat(anomaly.getAnomalyDetails().size()).isEqualTo(1);

    AnomalyDetail anomalyDetail = anomaly.getAnomalyDetails().stream().findFirst().orElse(null);
    assertThat(anomalyDetail).isNotNull();
    assertThat(anomalyDetail.getCvConfigId()).isEqualTo(cvConfig.getUuid());
    assertThat(anomalyDetail.getReportedTime()).isEqualTo(instant);
    assertThat(CollectionUtils.isEqualCollection(anomalyDetail.getAnomalousMetrics(), anomalousMetrics)).isTrue();

    anomalyService.closeAnomaly(accountId, cvConfig.getUuid(), instant);
    anomalies = hPersistence.createQuery(Anomaly.class, excludeAuthority).asList();
    assertThat(anomalies.size()).isEqualTo(1);
    anomaly = anomalies.get(0);
    assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.CLOSED);
    assertThat(anomaly.getAnomalyEndTime()).isEqualTo(instant);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testOpenAndCloseAnomaly_withMultipleConfigsAndInstants() {
    Instant now = Instant.now();
    int numOfCvConfigs = 5;
    List<CVConfig> cvConfigs = new ArrayList<>();
    for (int i = 0; i < numOfCvConfigs; i++) {
      cvConfigs.add(createAndSaveAppDConfig(CVMonitoringCategory.PERFORMANCE));
    }
    List<AnomalousMetric> anomalousMetrics = getAnomalousMetrics(5);
    for (Instant instant = now; instant.toEpochMilli() < now.plus(1, ChronoUnit.HOURS).toEpochMilli();
         instant = instant.plus(5, ChronoUnit.MINUTES)) {
      for (int i = 0; i < cvConfigs.size(); i++) {
        anomalyService.openAnomaly(accountId, cvConfigs.get(i).getUuid(), instant, anomalousMetrics);
      }
    }
    List<Anomaly> anomalies = hPersistence.createQuery(Anomaly.class, excludeAuthority).asList();
    assertThat(anomalies.size()).isEqualTo(1);
    Anomaly anomaly = anomalies.get(0);
    assertThat(anomaly.getAccountId()).isEqualTo(accountId);
    assertThat(anomaly.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(anomaly.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(anomaly.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(anomaly.getCategory()).isEqualTo(CVMonitoringCategory.PERFORMANCE);
    assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.OPEN);
    assertThat(anomaly.getAnomalyStartTime()).isEqualTo(now);
    assertThat(anomaly.getAnomalyEndTime()).isNull();
    assertThat(anomaly.getAnomalyDetails().size())
        .isEqualTo(12 * numOfCvConfigs); // 5 min boundaries in an hour * numOfCvConfigs

    for (Instant instant = now; instant.toEpochMilli() < now.plus(1, ChronoUnit.HOURS).toEpochMilli();
         instant = instant.plus(5, ChronoUnit.MINUTES)) {
      for (int i = 0; i < cvConfigs.size(); i++) {
        assertThat(anomaly.getAnomalyDetails())
            .contains(AnomalyDetail.builder().reportedTime(instant).cvConfigId(cvConfigs.get(i).getUuid()).build());
      }
    }

    anomaly.getAnomalyDetails().forEach(anomalyDetail
        -> assertThat(CollectionUtils.isEqualCollection(anomalyDetail.getAnomalousMetrics(), anomalousMetrics))
               .isTrue());

    // close anomalies and test
    for (int i = 0; i < numOfCvConfigs; i++) {
      anomalyService.closeAnomaly(accountId, cvConfigs.get(i).getUuid(), now.plus(i * 10, ChronoUnit.MINUTES));
      anomalies = hPersistence.createQuery(Anomaly.class, excludeAuthority).asList();
      assertThat(anomalies.size()).isEqualTo(1);
      anomaly = anomalies.get(0);
      if (i == numOfCvConfigs - 1) {
        assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.CLOSED);
        assertThat(anomaly.getAnomalyEndTime()).isEqualTo(now.plus(i * 10, ChronoUnit.MINUTES));
      } else {
        assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.OPEN);
      }
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testAnoamlies_withMultipleCategory() {
    Instant now = Instant.now();
    List<CVConfig> cvConfigs = new ArrayList<>();
    for (CVMonitoringCategory category : CVMonitoringCategory.values()) {
      cvConfigs.add(createAndSaveAppDConfig(category));
    }
    List<AnomalousMetric> anomalousMetrics = getAnomalousMetrics(5);
    for (Instant instant = now; instant.toEpochMilli() < now.plus(1, ChronoUnit.HOURS).toEpochMilli();
         instant = instant.plus(5, ChronoUnit.MINUTES)) {
      for (int i = 0; i < cvConfigs.size(); i++) {
        anomalyService.openAnomaly(accountId, cvConfigs.get(i).getUuid(), instant, anomalousMetrics);
      }
    }
    List<Anomaly> anomalies = hPersistence.createQuery(Anomaly.class, excludeAuthority).asList();
    assertThat(anomalies.size()).isEqualTo(CVMonitoringCategory.values().length);

    for (int i = 0; i < CVMonitoringCategory.values().length; i++) {
      CVMonitoringCategory category = CVMonitoringCategory.values()[i];
      Anomaly anomaly = anomalies.get(i);
      assertThat(anomaly.getAccountId()).isEqualTo(accountId);
      assertThat(anomaly.getServiceIdentifier()).isEqualTo(serviceIdentifier);
      assertThat(anomaly.getEnvIdentifier()).isEqualTo(envIdentifier);
      assertThat(anomaly.getProjectIdentifier()).isEqualTo(projectIdentifier);
      assertThat(anomaly.getCategory()).isEqualTo(category);
      assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.OPEN);
      assertThat(anomaly.getAnomalyStartTime()).isEqualTo(now);
      assertThat(anomaly.getAnomalyEndTime()).isNull();
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testCloseAnomaly_whenCvConfigDeleted() {
    Instant instant = Instant.now();
    CVConfig cvConfig = createAndSaveAppDConfig(CVMonitoringCategory.PERFORMANCE);
    List<AnomalousMetric> anomalousMetrics = getAnomalousMetrics(10);

    anomalyService.openAnomaly(accountId, cvConfig.getUuid(), instant, anomalousMetrics);
    List<Anomaly> anomalies = hPersistence.createQuery(Anomaly.class, excludeAuthority).asList();
    assertThat(anomalies.size()).isEqualTo(1);
    Anomaly anomaly = anomalies.get(0);
    assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.OPEN);

    cvConfigService.delete(cvConfig.getUuid());
    anomaly = hPersistence.createQuery(Anomaly.class, excludeAuthority).asList().get(0);
    assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.CLOSED);
  }

  private CVConfig createAndSaveAppDConfig(CVMonitoringCategory category) {
    metricPackService.getMetricPacks(accountId, projectIdentifier, DataSourceType.APP_DYNAMICS);
    AppDynamicsCVConfig appDynamicsCVConfig = new AppDynamicsCVConfig();
    appDynamicsCVConfig.setServiceIdentifier(serviceIdentifier);
    appDynamicsCVConfig.setEnvIdentifier(envIdentifier);
    appDynamicsCVConfig.setProjectIdentifier(projectIdentifier);
    appDynamicsCVConfig.setAccountId(accountId);
    appDynamicsCVConfig.setCategory(category);
    appDynamicsCVConfig.setMetricPack(MetricPack.builder()
                                          .identifier(PERFORMANCE_PACK_IDENTIFIER)
                                          .metrics(Sets.newHashSet(MetricPack.MetricDefinition.builder().build()))
                                          .build());
    appDynamicsCVConfig.setConnectorId(generateUuid());
    appDynamicsCVConfig.setGroupId(generateUuid());
    appDynamicsCVConfig.setApplicationName(generateUuid());
    appDynamicsCVConfig.setTierName(generateUuid());
    return cvConfigService.save(appDynamicsCVConfig);
  }

  private List<AnomalousMetric> getAnomalousMetrics(int numOfMetrics) {
    List<AnomalousMetric> rv = new ArrayList<>();
    for (int i = 0; i < numOfMetrics; i++) {
      rv.add(AnomalousMetric.builder().groupName("g-" + i).metricName("m-" + i).riskScore(0.1 * i).build());
    }

    return rv;
  }
}
