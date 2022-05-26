/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.filestore.config;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.FileUploadLimit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Singleton;
import io.dropwizard.Configuration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.reflections.Reflections;

import javax.ws.rs.Path;
import java.util.Collection;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
@OwnedBy(CDP)
public class FileStoreConfiguration extends Configuration {
  public static final String RESOURCE_PACKAGE = "io.harness.filestore.api.resource";
  @JsonProperty private FileUploadLimit fileUploadLimits = new FileUploadLimit();

  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(RESOURCE_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }
}
