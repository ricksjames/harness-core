/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.environment.services;

import io.harness.ng.core.environment.beans.NGServiceOverridesEntity;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface ServiceOverrideService {
  Optional<NGServiceOverridesEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentRef, String serviceRef);

  NGServiceOverridesEntity upsert(NGServiceOverridesEntity requestServiceOverride);

  Page<NGServiceOverridesEntity> list(Criteria criteria, Pageable pageable);
}
