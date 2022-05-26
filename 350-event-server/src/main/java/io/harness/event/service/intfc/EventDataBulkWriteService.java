/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.service.intfc;

import io.harness.ccm.commons.entities.events.PublishedMessage;

import java.util.List;

public interface EventDataBulkWriteService {
  boolean insertPublishedMessages(List<PublishedMessage> publishedMessages);
  void saveIgnoringDuplicateKeys(List<PublishedMessage> publishedMessages);
  void saveBatch(List<PublishedMessage> publishedMessages);
}
