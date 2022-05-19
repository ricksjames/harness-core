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
import io.harness.ccm.views.entities.ViewType;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class CEViewFolderDao {
  @Inject private HPersistence hPersistence;

  private static final int FOLDERS_PER_PAGE = 1;

  public CEViewFolder save(CEViewFolder ceViewFolder) {
    String id = hPersistence.save(ceViewFolder);
    return hPersistence.createQuery(CEViewFolder.class)
        .field(CEViewFolderKeys.uuid)
        .equal(new ObjectId(id))
        .get();
  }

  public long getNumberOfFolders(String accountId) {
    return hPersistence.createQuery(CEViewFolder.class)
        .field(CEViewFolderKeys.accountId)
        .equal(accountId)
        .count();
  }

  public long getNumberOfFolders(String accountId, List<String> folderIds) {
    List<ObjectId> folderIdsToQuery = folderIds.stream().map(ObjectId::new).collect(Collectors.toList());
    return hPersistence.createQuery(CEViewFolder.class)
        .field(CEViewFolderKeys.accountId)
        .equal(accountId)
        .field(CEViewFolderKeys.uuid)
        .in(folderIdsToQuery)
        .count();
  }

  public List<CEViewFolder> getFolders(String accountId, long pageNo) {
    return hPersistence.createQuery(CEViewFolder.class)
        .field(CEViewFolderKeys.accountId)
        .equal(accountId)
        .offset(((int) pageNo - 1) * FOLDERS_PER_PAGE)
        .limit(FOLDERS_PER_PAGE)
        .asList();
  }

  public CEViewFolder getDefaultFolder(String accountId) {
    return hPersistence.createQuery(CEViewFolder.class)
        .field(CEViewFolderKeys.accountId)
        .equal(accountId)
        .field(CEViewFolderKeys.viewType)
        .equal(ViewType.DEFAULT)
        .get();
  }

  public List<CEViewFolder> getFolders(String accountId, List<String> folderIds, long pageNo) {
    List<ObjectId> folderIdsToQuery = folderIds.stream().map(ObjectId::new).collect(Collectors.toList());
    return hPersistence.createQuery(CEViewFolder.class)
        .field(CEViewFolderKeys.accountId)
        .equal(accountId)
        .field(CEViewFolderKeys.uuid)
        .in(folderIdsToQuery)
        .offset(((int) pageNo - 1) * FOLDERS_PER_PAGE)
        .limit(FOLDERS_PER_PAGE)
        .asList();
  }

  public CEViewFolder updateFolder(String accountId, CEViewFolder ceViewFolder) {
    Query<CEViewFolder> query = hPersistence.createQuery(CEViewFolder.class)
        .field(CEViewFolderKeys.accountId)
        .equal(accountId)
        .field(CEViewFolderKeys.uuid)
        .equal(ceViewFolder.getUuid());

    UpdateOperations<CEViewFolder> updateOperations = hPersistence.createUpdateOperations(CEViewFolder.class);
    if (ceViewFolder.getName() != null) {
      updateOperations = updateOperations.set(CEViewFolderKeys.name, ceViewFolder.getName());
    }
    if (ceViewFolder.getTags() != null) {
      updateOperations = updateOperations.set(CEViewFolderKeys.tags, ceViewFolder.getTags());
    }
    if (ceViewFolder.getDescription() != null) {
      updateOperations = updateOperations.set(CEViewFolderKeys.description, ceViewFolder.getDescription());
    }
    if (ceViewFolder.getLastUpdatedBy() != null) {
      updateOperations = updateOperations.set(CEViewFolderKeys.lastUpdatedBy, ceViewFolder.getLastUpdatedBy());
    }
    updateOperations = updateOperations.set(CEViewFolderKeys.pinned, ceViewFolder.isPinned());

    hPersistence.update(query, updateOperations);
    return query.asList().get(0);
  }

  public CEViewFolder updateFolderName(String accountId, String uuid, String newName) {
    Query<CEViewFolder> query = hPersistence.createQuery(CEViewFolder.class)
        .field(CEViewFolderKeys.accountId)
        .equal(accountId)
        .field(CEViewFolderKeys.uuid)
        .equal(new ObjectId(uuid));

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
        .equal(new ObjectId(uuid));

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
        .equal(new ObjectId(uuid));
    return hPersistence.delete(query);
  }
}
