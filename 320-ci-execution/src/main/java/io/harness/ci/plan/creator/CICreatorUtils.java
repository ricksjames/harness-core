/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.plan.creator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.collect.Sets;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CI)
public class CICreatorUtils {
  public Set<String> getSupportedSteps() {
    return Sets.newHashSet("SaveCacheS3", "Test", "RunTests", "SaveCache", "liteEngineTask", "GitClone",
        "BuildAndPushGCR", "BuildAndPushECR", "BuildAndPushDockerRegistry", "Cleanup", "Plugin", "PublishArtifacts",
        "Security", "RestoreCacheGCS", "RestoreCacheS3", "RestoreCache", "SaveCacheGCS", "Run", "S3Upload", "GCSUpload",
        "ArtifactoryUpload");
  }
}
