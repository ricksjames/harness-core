/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.ccm.views.entities.CEViewFolder;
import io.harness.ccm.views.entities.CEViewFolder.CEViewFolderKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.List;

@Slf4j
@Singleton
public class CEViewFolderDao {
  @Inject private HPersistence hPersistence;

  public boolean save(CEViewFolder ceViewFolder) {
    return hPersistence.save(ceViewFolder) != null;
  }

  public List<CEViewFolder> getFoldersByAccountId(String accountId) {
    return hPersistence.createQuery(CEViewFolder.class)
        .field(CEViewFolderKeys.accountId)
        .equal(accountId)
        .asList();
  }

  public CEViewFolder updateFolderName(String accountId, String uuid, String newName) {
    Query<CEViewFolder> query = hPersistence.createQuery(CEViewFolder.class)
        .field(CEViewFolderKeys.accountId)
        .equal(accountId)
        .field(CEViewFolderKeys.uuid)
        .equal(uuid);

    UpdateOperations<CEViewFolder> updateOperations =
        hPersistence.createUpdateOperations(CEViewFolder.class).set(CEViewFolderKeys.name, newName);
    hPersistence.update(query, updateOperations);
    return query.asList().get(0);
  }

  public CEViewFolder updateFolderPinStatus(String accountId, String uuid, boolean pinStatus) {
    Query<CEViewFolder> query = hPersistence.createQuery(CEViewFolder.class)
        .field(CEViewFolderKeys.accountId)
        .equal(accountId)
        .field(CEViewFolderKeys.uuid)
        .equal(uuid);

    UpdateOperations<CEViewFolder> updateOperations =
        hPersistence.createUpdateOperations(CEViewFolder.class).set(CEViewFolderKeys.pinned, pinStatus);
    hPersistence.update(query, updateOperations);
    return query.asList().get(0);
  }

  public boolean delete(String accountId, String uuid) {
    Query<CEViewFolder> query = hPersistence.createQuery(CEViewFolder.class)
        .field(CEViewFolderKeys.accountId)
        .equal(accountId)
        .field(CEViewFolderKeys.uuid)
        .equal(uuid);
    return hPersistence.delete(query);
  }
}
