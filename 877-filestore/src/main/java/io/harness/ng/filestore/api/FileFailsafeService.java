/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.filestore.api;

import io.harness.ng.core.filestore.dto.FileDTO;
import io.harness.ng.filestore.entities.NGFile;

public interface FileFailsafeService {
  /**
   * Save NG file in DB and publish file crete event
   *
   * @param ngFile the NG file
   * @return published file
   */
  FileDTO saveAndPublish(io.harness.ng.filestore.entities.NGFile ngFile);

  /**
   * Update NG file in DB and publish file update event
   *
   * @param oldNGFile the existing NG file in DB
   * @param newNGFile the new NG file
   * @return published file
   */
  FileDTO updateAndPublish(
      io.harness.ng.filestore.entities.NGFile oldNGFile, io.harness.ng.filestore.entities.NGFile newNGFile);

  /**
   * Delete NG file in DB and publish file delete event
   *
   * @param ngFile the NG file
   * @return if file delete event is published and file deleted form DB
   */
  boolean deleteAndPublish(NGFile ngFile);
}
