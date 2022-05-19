/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.instancesyncperpetualtaskinfo;

import io.harness.InstancesTestBase;
import io.harness.category.element.UnitTests;
import io.harness.entities.Instance;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo;
import io.harness.models.CountByServiceIdAndEnvType;
import io.harness.models.EnvBuildInstanceCount;
import io.harness.models.InstancesByBuildId;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.repositories.instance.InstanceRepositoryCustomImpl;
import io.harness.rule.Owner;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Arrays;
import java.util.List;

import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class InstanceSyncPerpetualTaskInfoRepositoryCustomImplTest extends InstancesTestBase {

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
    private final long TIMESTAMP = 123L;
    private final long START_TIMESTAMP = 124L;
    private final long END_TIMESTAMP = 125L;
    @Mock InstanceSyncPerpetualTaskInfo instanceSyncPerpetualTaskInfo;
    @Mock MongoTemplate mongoTemplate;
    @InjectMocks
    InstanceSyncPerpetualTaskInfoRepositoryCustomImpl instanceSyncPerpetualTaskInfoRepositoryCustom;

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void updateTest() {
        Criteria criteria = Criteria.where("key");
        Query query = new Query(criteria);
        Update update = new Update();
        when(mongoTemplate.findAndModify(
                eq(query), eq(update), any(FindAndModifyOptions.class), eq(InstanceSyncPerpetualTaskInfo.class))).thenReturn(instanceSyncPerpetualTaskInfo);
        assertThat(instanceSyncPerpetualTaskInfoRepositoryCustom.update(criteria, update)).isEqualTo(instanceSyncPerpetualTaskInfo);
    }
}
