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
import io.harness.migration.NGMigration;
import io.harness.migration.beans.MigrationType;
import io.harness.migrations.timescale.CreateInstanceStatsDayTable;
import io.harness.migrations.timescale.CreateInstanceStatsHourTable;
import io.harness.migrations.timescale.CreateInstanceStatsTable;
import io.harness.migrations.timescale.InitTriggerFunctions;
import io.harness.rule.Owner;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

import java.sql.Timestamp;
import java.util.List;

import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;
import static org.assertj.core.api.Assertions.assertThat;

public class InstanceStatsTimeScaleMigrationDetailsTest extends InstancesTestBase {

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
    InstanceStatsTimeScaleMigrationDetails instanceStatsTimeScaleMigrationDetails;

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getMigrationTypeNameTest() {
        assertThat(instanceStatsTimeScaleMigrationDetails.getMigrationTypeName()).isEqualTo(MigrationType.TimeScaleMigration);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void isBackgroundTest() {
        assertThat(instanceStatsTimeScaleMigrationDetails.isBackground()).isEqualTo(false);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void getMigrationsTest() {
        List<Pair<Integer, Class<? extends NGMigration>>> response = instanceStatsTimeScaleMigrationDetails.getMigrations();
        assertThat(response.size()).isEqualTo(4);
        assertThat(response.contains(Pair.of(1, InitTriggerFunctions.class))).isTrue();
        assertThat(response.contains(Pair.of(2, CreateInstanceStatsTable.class))).isTrue();
        assertThat(response.contains(Pair.of(3, CreateInstanceStatsHourTable.class))).isTrue();
        assertThat(response.contains(Pair.of(4, CreateInstanceStatsDayTable.class))).isTrue();
    }
}
