/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.migrations;

import io.harness.InstancesTestBase;
import io.harness.category.element.UnitTests;
import io.harness.migration.MigrationDetails;
import io.harness.models.InstanceStats;
import io.harness.repositories.instancestats.InstanceStatsFields;
import io.harness.repositories.instancestats.InstanceStatsQuery;
import io.harness.repositories.instancestats.InstanceStatsRepositoryImpl;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class InstanceMigrationProviderTest extends InstancesTestBase {

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
    @InjectMocks
    InstanceMigrationProvider instanceMigrationProvider;

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getServiceNameTest() {
        assertThat(instanceMigrationProvider.getServiceName()).isEqualTo("instance");
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getSchemaClassTest() {
        assertThat(instanceMigrationProvider.getSchemaClass()).isEqualTo(NGInstanceSchema.class);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getMigrationDetailsListTest() {
        List<Class<? extends MigrationDetails>> response = instanceMigrationProvider.getMigrationDetailsList();
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.get(0)).isEqualTo(InstanceStatsTimeScaleMigrationDetails.class);
    }
}
