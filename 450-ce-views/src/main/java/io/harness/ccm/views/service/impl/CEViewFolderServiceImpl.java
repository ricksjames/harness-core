/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.dao.CEViewDao;
import io.harness.ccm.views.dao.CEViewFolderDao;
import io.harness.ccm.views.entities.*;
import io.harness.ccm.views.service.CEViewFolderService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import static io.harness.annotations.dev.HarnessTeam.CE;

@Slf4j
@Singleton
@OwnedBy(CE)
public class CEViewFolderServiceImpl implements CEViewFolderService {
  @Inject private CEViewDao ceViewDao;
  @Inject private CEViewFolderDao ceViewFolderDao;

  @Override
  public boolean save(CEViewFolder ceViewFolder) {
    return ceViewFolderDao.save(ceViewFolder);
  }

  @Override
  public long numberOfFolders(String accountId) {
    return ceViewFolderDao.getNumberOfFolders(accountId);
  }

  @Override
  public long numberOfFolders(String accountId, List<String> folderIds) {
    return ceViewFolderDao.getNumberOfFolders(accountId, folderIds);
  }

  @Override
  public List<CEViewFolder> getFolders(String accountId, long pageNo) {
    return ceViewFolderDao.getFolders(accountId, pageNo);
  }

  @Override
  public List<CEViewFolder> getFolders(String accountId, List<String> folderIds, long pageNo) {
    return ceViewFolderDao.getFolders(accountId, folderIds, pageNo);
  }

  @Override
  public List<CEView> getPerspectivesForFolder(String accountId, String folderId) {
    return ceViewDao.findByAccountIdAndFolderId(accountId, folderId);
  }

  @Override
  public CEViewFolder updateFolder(String accountId, CEViewFolder ceViewFolder) {
    return ceViewFolderDao.updateFolder(accountId, ceViewFolder);
  }

  @Override
  public CEViewFolder updateFolderName(String accountId, String uuid, String newName) {
    return ceViewFolderDao.updateFolderName(accountId, uuid, newName);
  }

  @Override
  public CEViewFolder pinFolder(String accountId, String uuid, boolean pinStatus) {
    return ceViewFolderDao.updateFolderPinStatus(accountId, uuid, pinStatus);
  }

  @Override
  public CEView moveCEView(String accountId, String ceViewId, String toFolderId) {
    // check toFolderId
    return ceViewDao.movePerspectiveFolder(accountId, ceViewId, toFolderId);
  }

  @Override
  public List<CEView> moveMultipleCEViews(String accountId, List<String> ceViewIds, String toFolderId) {
    return ceViewDao.moveMultiplePerspectiveFolder(accountId, ceViewIds, toFolderId);
  }

  @Override
  public boolean delete(String accountId, String uuid) {
    List<CEView> perspectives = ceViewDao.findByAccountIdAndFolderId(accountId, uuid);
    List<String> perspectiveIds = perspectives.stream().map(CEView::getUuid).collect(Collectors.toList());
    CEViewFolder folder = ceViewFolderDao.getDefaultFolder(accountId);
    ceViewDao.moveMultiplePerspectiveFolder(accountId, perspectiveIds, String.valueOf(folder.getUuid()));
    return ceViewFolderDao.delete(accountId, uuid);
  }
}
