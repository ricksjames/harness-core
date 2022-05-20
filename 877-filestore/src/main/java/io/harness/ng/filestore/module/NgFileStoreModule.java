/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.filestore.module;

import com.google.inject.AbstractModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.FilterType;
import io.harness.filter.mapper.FilterPropertiesMapper;
import io.harness.ng.filestore.api.FileActivityService;
import io.harness.ng.filestore.api.FileFailsafeService;
import io.harness.ng.filestore.api.FileStoreService;
import io.harness.ng.filestore.api.impl.FileActivityServiceImpl;
import io.harness.ng.filestore.api.impl.FileFailsafeServiceImpl;
import io.harness.ng.filestore.api.impl.FileStoreServiceImpl;
import io.harness.ng.filestore.dto.mapper.FilesFilterPropertiesMapper;
import io.harness.ng.filestore.repositories.custom.FileStoreRepositoryCustom;
import io.harness.ng.filestore.repositories.custom.FileStoreRepositoryCustomImpl;
import io.harness.persistence.HPersistence;

import com.google.inject.multibindings.MapBinder;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class NgFileStoreModule extends AbstractModule {
  @Override
  protected void configure() {
    registerRequiredBindings();
    bind(FileStoreService.class).to(FileStoreServiceImpl.class);
    bind(FileFailsafeService.class).to(FileFailsafeServiceImpl.class);
    bind(FileActivityService.class).to(FileActivityServiceImpl.class);
    bind(FileStoreRepositoryCustom.class).to(FileStoreRepositoryCustomImpl.class);
    MapBinder<String, FilterPropertiesMapper> filterPropertiesMapper =
        MapBinder.newMapBinder(binder(), String.class, FilterPropertiesMapper.class);
    filterPropertiesMapper.addBinding(FilterType.FILESTORE.toString()).to(FilesFilterPropertiesMapper.class);
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
