/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.instancestats;

import io.harness.InstancesTestBase;
import io.harness.category.element.UnitTests;
import io.harness.entities.Instance;
import io.harness.models.CountByServiceIdAndEnvType;
import io.harness.models.EnvBuildInstanceCount;
import io.harness.models.InstanceStats;
import io.harness.models.InstancesByBuildId;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.repositories.instance.InstanceRepositoryCustomImpl;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class InstanceStatsRepositoryImplTest extends InstancesTestBase {

    private final String INSTANCE_SYNC_KEY = "instanceSyncKey";
    private final String ACCOUNT_ID = "acc";
    private final String ORGANIZATION_ID = "org";
    private final String PROJECT_ID = "project";
    private final String SERVICE_ID = "service";
    private final String INFRASTRUCTURE_MAPPING_ID = "infra";
    private final String INSTANCE_INFO_PODNAME = "podname";
    private final String INSTANCE_INFO_POD_NAMESPACE = "podnamespace";
    private final String ENVIRONMENT_ID = "envID";
    private final String ENVIRONMENT_NAME = "envName";
    private final String TAG = "tag";
    private final int COUNT = 3;
    private final Timestamp timestamp = new Timestamp(1234l);
    private final long START_TIMESTAMP = 124L;
    private final long END_TIMESTAMP = 125L;
    private static final int MAX_RETRY_COUNT = 3;
    @Mock private TimeScaleDBService timeScaleDBService;
    @Mock private PreparedStatement statement;
    @Mock private Connection dbConnection;
    @Mock private ResultSet resultSet;
    @InjectMocks
    InstanceStatsRepositoryImpl instanceStatsRepository;

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getLatestRecordTest() throws SQLException {
        when(timeScaleDBService.getDBConnection()).thenReturn(dbConnection);
        when(dbConnection.prepareStatement(InstanceStatsQuery.FETCH_LATEST_RECORD.query())).thenReturn(statement);
        statement.setString(1, ACCOUNT_ID);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(io.harness.repositories.instancestats.InstanceStatsFields.ACCOUNTID.fieldName())).thenReturn(ACCOUNT_ID);
        when(resultSet.getString(io.harness.repositories.instancestats.InstanceStatsFields.ENVID.fieldName())).thenReturn(ENVIRONMENT_ID);
        when(resultSet.getString(io.harness.repositories.instancestats.InstanceStatsFields.SERVICEID.fieldName())).thenReturn(SERVICE_ID);
        when(resultSet.getTimestamp(InstanceStatsFields.REPORTEDAT.fieldName())).thenReturn(timestamp);
        InstanceStats instanceStats = instanceStatsRepository.getLatestRecord(ACCOUNT_ID);
        assertThat(instanceStats.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(instanceStats.getEnvId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(instanceStats.getServiceId()).isEqualTo(SERVICE_ID);
        assertThat(instanceStats.getReportedAt()).isEqualTo(timestamp);
    }
}
