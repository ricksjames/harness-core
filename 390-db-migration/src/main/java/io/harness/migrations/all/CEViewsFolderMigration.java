/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.ccm.views.dao.CEViewFolderDao;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.CEView.CEViewKeys;
import io.harness.ccm.views.entities.CEViewFolder;
import io.harness.ccm.views.entities.ViewType;
import io.harness.migrations.Migration;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.List;

@Slf4j
@Singleton
public class CEViewsFolderMigration implements Migration {
  @Inject private HPersistence hPersistence;
  @Inject private CEViewFolderDao ceViewFolderDao;

  @Override
  public void migrate() {
    try {
      log.info("Starting migration of all CCM Perspectives");

      List<String> accountIds = hPersistence.createQuery(CEView.class).getCollection().distinct("accountId");
      for (String accountId: accountIds) {
        if (StringUtils.isEmpty(accountId)) {
          continue;
        }
        CEViewFolder defaultFolder = ceViewFolderDao.getDefaultFolder(accountId);
        String defaultFolderId;
        if (defaultFolder == null) {
          defaultFolderId = ceViewFolderDao.createDefaultOrSampleFolder(accountId, ViewType.DEFAULT);
        } else {
          defaultFolderId = defaultFolder.getUuid();
        }
        Query<CEView> query = hPersistence.createQuery(CEView.class)
            .field(CEViewKeys.accountId)
            .equal(accountId)
            .field(CEViewKeys.viewType)
            .equal(ViewType.CUSTOMER);
        UpdateOperations<CEView> updateOperations = hPersistence.createUpdateOperations(CEView.class)
            .set(CEViewKeys.folderId, defaultFolderId);
        hPersistence.update(query, updateOperations);

        CEViewFolder sampleFolder = ceViewFolderDao.getSampleFolder(accountId);
        String sampleFolderId;
        if (sampleFolder == null) {
          sampleFolderId = ceViewFolderDao.createDefaultOrSampleFolder(accountId, ViewType.SAMPLE);
        } else {
          sampleFolderId = sampleFolder.getUuid();
        }
        query = hPersistence.createQuery(CEView.class)
            .field(CEViewKeys.accountId)
            .equal(accountId)
            .field(CEViewKeys.viewType)
            .in(ImmutableList.of(ViewType.DEFAULT, ViewType.DEFAULT_AZURE, ViewType.SAMPLE));
        updateOperations = hPersistence.createUpdateOperations(CEView.class)
            .set(CEViewKeys.folderId, sampleFolderId);
        hPersistence.update(query, updateOperations);
      }
    } catch (Exception e) {
      log.error("Failure occurred in CEViewsFolderMigration", e);
    }
    log.info("CEViewsFolderMigration has completed");
  }
}
