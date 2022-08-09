/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.stateutils.buildstate;


import static io.harness.ci.buildstate.PluginSettingUtils.TAG_BUILD_EVENT;
import static io.harness.ci.commonconstants.BuildEnvironmentConstants.DRONE_BUILD_EVENT;
import static io.harness.ci.commonconstants.BuildEnvironmentConstants.DRONE_COMMIT_BRANCH;
import static io.harness.ci.commonconstants.BuildEnvironmentConstants.DRONE_TAG;
import static io.harness.ci.commonconstants.CIExecutionConstants.GIT_SSL_NO_VERIFY;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.JAMES_RICKS;
import static io.harness.rule.OwnerRule.RAGHAV_GUPTA;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.stepinfo.DockerStepInfo;
import io.harness.beans.steps.stepinfo.ECRStepInfo;
import io.harness.beans.steps.stepinfo.GCRStepInfo;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheGCSStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheS3StepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheGCSStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheS3StepInfo;
import io.harness.beans.steps.stepinfo.UploadToArtifactoryStepInfo;
import io.harness.beans.steps.stepinfo.UploadToGCSStepInfo;
import io.harness.beans.steps.stepinfo.UploadToS3StepInfo;
import io.harness.beans.sweepingoutputs.StageInfraDetails.Type;
import io.harness.beans.yaml.extended.ArchiveFormat;
import io.harness.category.element.UnitTests;
import io.harness.ci.buildstate.CodebaseUtils;
import io.harness.ci.buildstate.ConnectorUtils;
import io.harness.ci.buildstate.PluginSettingUtils;
import io.harness.ci.executionplan.CIExecutionTestBase;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.yaml.extended.ci.codebase.Build;
import io.harness.yaml.extended.ci.codebase.BuildSpec;
import io.harness.yaml.extended.ci.codebase.BuildType;
import io.harness.yaml.extended.ci.codebase.impl.BranchBuildSpec;
import io.harness.yaml.extended.ci.codebase.impl.PRBuildSpec;
import io.harness.yaml.extended.ci.codebase.impl.TagBuildSpec;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CI)
public class PluginSettingUtilsTest extends CIExecutionTestBase {

  @Inject public PluginSettingUtils pluginSettingUtils;

  @Mock private CodebaseUtils codebaseUtils;
  @Mock private ConnectorUtils connectorUtils;

  @Before
  public void setUp() {
    on(pluginSettingUtils).set("codebaseUtils", codebaseUtils);
    on(codebaseUtils).set("connectorUtils", connectorUtils);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetUploadToArtifactoryStepInfoStepEnvVariables() {
    UploadToArtifactoryStepInfo uploadToArtifactoryStepInfo =
        UploadToArtifactoryStepInfo.builder()
            .target(ParameterField.createValueField("repo/wings/software/module/1.0.0-SNAPSHOT"))
            .sourcePath(ParameterField.createValueField("target/libmodule.jar"))
            .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_TARGET", "repo/wings/software/module/1.0.0-SNAPSHOT");
    expected.put("PLUGIN_SOURCE", "target/libmodule.jar");
    expected.put("PLUGIN_ARTIFACT_FILE", "/addon/tmp/.plugin/artifact");
    expected.put("PLUGIN_FLAT", "true");
    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
        pluginSettingUtils.getPluginCompatibleEnvVariables(uploadToArtifactoryStepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetGCRStepInfoEnvVariables() {
    GCRStepInfo gcrStepInfo =
        GCRStepInfo.builder()
            .host(ParameterField.createValueField("gcr.io/"))
            .projectID(ParameterField.createValueField("/ci"))
            .imageName(ParameterField.createValueField("harness"))
            .tags(ParameterField.createValueField(asList("tag1", "tag2")))
            .dockerfile(ParameterField.createValueField("Dockerfile"))
            .context(ParameterField.createValueField("context"))
            .target(ParameterField.createValueField("target"))
            .buildArgs(ParameterField.createValueField(Collections.singletonMap("arg1", "value1")))
            .labels(ParameterField.createValueField(Collections.singletonMap("label", "label1")))
            .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_REGISTRY", "gcr.io/ci");
    expected.put("PLUGIN_REPO", "harness");
    expected.put("PLUGIN_TAGS", "tag1,tag2");
    expected.put("PLUGIN_DOCKERFILE", "Dockerfile");
    expected.put("PLUGIN_CONTEXT", "context");
    expected.put("PLUGIN_TARGET", "target");
    expected.put("PLUGIN_BUILD_ARGS", "arg1=value1");
    expected.put("PLUGIN_CUSTOM_LABELS", "label=label1");
    expected.put("PLUGIN_SNAPSHOT_MODE", "redo");
    expected.put("PLUGIN_ARTIFACT_FILE", "/addon/tmp/.plugin/artifact");
    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
        pluginSettingUtils.getPluginCompatibleEnvVariables(gcrStepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetECRStepInfoStepEnvVariables() {
    ECRStepInfo ecrStepInfo =
        ECRStepInfo.builder()
            .account(ParameterField.createValueField("6874654867"))
            .region(ParameterField.createValueField("eu-central-1"))
            .imageName(ParameterField.createValueField("harness"))
            .tags(ParameterField.createValueField(asList("tag1", "tag2")))
            .dockerfile(ParameterField.createValueField("Dockerfile"))
            .context(ParameterField.createValueField("context"))
            .target(ParameterField.createValueField("target"))
            .buildArgs(ParameterField.createValueField(Collections.singletonMap("arg1", "value1")))
            .labels(ParameterField.createValueField(Collections.singletonMap("label", "label1")))
            .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_REGISTRY", "6874654867.dkr.ecr.eu-central-1.amazonaws.com");
    expected.put("PLUGIN_REGION", "eu-central-1");
    expected.put("PLUGIN_REPO", "harness");
    expected.put("PLUGIN_TAGS", "tag1,tag2");
    expected.put("PLUGIN_DOCKERFILE", "Dockerfile");
    expected.put("PLUGIN_CONTEXT", "context");
    expected.put("PLUGIN_TARGET", "target");
    expected.put("PLUGIN_BUILD_ARGS", "arg1=value1");
    expected.put("PLUGIN_CUSTOM_LABELS", "label=label1");
    expected.put("PLUGIN_SNAPSHOT_MODE", "redo");
    expected.put("PLUGIN_ARTIFACT_FILE", "/addon/tmp/.plugin/artifact");
    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
        pluginSettingUtils.getPluginCompatibleEnvVariables(ecrStepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetDockerStepInfoEnvVariables() {
    DockerStepInfo dockerStepInfo =
        DockerStepInfo.builder()
            .repo(ParameterField.createValueField("harness"))
            .tags(ParameterField.createValueField(asList("tag1", "tag2")))
            .dockerfile(ParameterField.createValueField("Dockerfile"))
            .context(ParameterField.createValueField("context"))
            .target(ParameterField.createValueField("target"))
            .buildArgs(ParameterField.createValueField(Collections.singletonMap("arg1", "value1")))
            .labels(ParameterField.createValueField(Collections.singletonMap("label", "label1")))
            .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_REPO", "harness");
    expected.put("PLUGIN_TAGS", "tag1,tag2");
    expected.put("PLUGIN_DOCKERFILE", "Dockerfile");
    expected.put("PLUGIN_CONTEXT", "context");
    expected.put("PLUGIN_TARGET", "target");
    expected.put("PLUGIN_BUILD_ARGS", "arg1=value1");
    expected.put("PLUGIN_CUSTOM_LABELS", "label=label1");
    expected.put("PLUGIN_SNAPSHOT_MODE", "redo");
    expected.put("PLUGIN_ARTIFACT_FILE", "/addon/tmp/.plugin/artifact");
    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
        pluginSettingUtils.getPluginCompatibleEnvVariables(dockerStepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetRestoreCacheS3StepInfoEnvVariables() {
    RestoreCacheS3StepInfo restoreCacheS3StepInfo = RestoreCacheS3StepInfo.builder()
                                                        .key(ParameterField.createValueField("key"))
                                                        .bucket(ParameterField.createValueField("bucket"))
                                                        .endpoint(ParameterField.createValueField("endpoint"))
                                                        .region(ParameterField.createValueField("region"))
                                                        .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_BACKEND", "s3");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_ENDPOINT", "endpoint");
    expected.put("PLUGIN_CACHE_KEY", "key");
    expected.put("PLUGIN_RESTORE", "true");
    expected.put("PLUGIN_EXIT_CODE", "true");
    expected.put("PLUGIN_REGION", "region");
    expected.put("PLUGIN_ARCHIVE_FORMAT", "tar");
    expected.put("PLUGIN_FAIL_RESTORE_IF_KEY_NOT_PRESENT", "false");
    expected.put("PLUGIN_PATH_STYLE", "false");
    expected.put("PLUGIN_BACKEND_OPERATION_TIMEOUT", "100s");
    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
        pluginSettingUtils.getPluginCompatibleEnvVariables(restoreCacheS3StepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetRestoreCacheS3StepInfoEnvVariablesArchiveSet() {
    RestoreCacheS3StepInfo restoreCacheS3StepInfo =
        RestoreCacheS3StepInfo.builder()
            .key(ParameterField.createValueField("key"))
            .bucket(ParameterField.createValueField("bucket"))
            .endpoint(ParameterField.createValueField("endpoint"))
            .region(ParameterField.createValueField("region"))
            .archiveFormat(ParameterField.createValueField(ArchiveFormat.GZIP))
            .failIfKeyNotFound(ParameterField.createValueField(true))
            .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_BACKEND", "s3");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_ENDPOINT", "endpoint");
    expected.put("PLUGIN_CACHE_KEY", "key");
    expected.put("PLUGIN_RESTORE", "true");
    expected.put("PLUGIN_EXIT_CODE", "true");
    expected.put("PLUGIN_REGION", "region");
    expected.put("PLUGIN_ARCHIVE_FORMAT", "gzip");
    expected.put("PLUGIN_FAIL_RESTORE_IF_KEY_NOT_PRESENT", "true");
    expected.put("PLUGIN_PATH_STYLE", "false");
    expected.put("PLUGIN_BACKEND_OPERATION_TIMEOUT", "100s");
    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
        pluginSettingUtils.getPluginCompatibleEnvVariables(restoreCacheS3StepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetSaveCacheS3StepInfoEnvVariablesBasic() {
    SaveCacheS3StepInfo saveCacheS3StepInfo =
        SaveCacheS3StepInfo.builder()
            .key(ParameterField.createValueField("key"))
            .bucket(ParameterField.createValueField("bucket"))
            .region(ParameterField.createValueField("region"))
            .sourcePaths(ParameterField.createValueField(asList("path1", "path2")))
            .endpoint(ParameterField.createValueField("endpoint"))
            .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_BACKEND", "s3");
    expected.put("PLUGIN_MOUNT", "path1,path2");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_ENDPOINT", "endpoint");
    expected.put("PLUGIN_CACHE_KEY", "key");
    expected.put("PLUGIN_REBUILD", "true");
    expected.put("PLUGIN_EXIT_CODE", "true");
    expected.put("PLUGIN_REGION", "region");
    expected.put("PLUGIN_PATH_STYLE", "false");
    expected.put("PLUGIN_ARCHIVE_FORMAT", "tar");
    expected.put("PLUGIN_BACKEND_OPERATION_TIMEOUT", "100s");
    expected.put("PLUGIN_OVERRIDE", "false");
    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
        pluginSettingUtils.getPluginCompatibleEnvVariables(saveCacheS3StepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetSaveCacheS3StepInfoEnvVariablesArchiveSet() {
    SaveCacheS3StepInfo saveCacheS3StepInfo =
        SaveCacheS3StepInfo.builder()
            .key(ParameterField.createValueField("key"))
            .bucket(ParameterField.createValueField("bucket"))
            .region(ParameterField.createValueField("region"))
            .sourcePaths(ParameterField.createValueField(asList("path1", "path2")))
            .endpoint(ParameterField.createValueField("endpoint"))
            .archiveFormat(ParameterField.createValueField(ArchiveFormat.GZIP))
            .override(ParameterField.createValueField(true))
            .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_BACKEND", "s3");
    expected.put("PLUGIN_MOUNT", "path1,path2");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_ENDPOINT", "endpoint");
    expected.put("PLUGIN_CACHE_KEY", "key");
    expected.put("PLUGIN_REBUILD", "true");
    expected.put("PLUGIN_EXIT_CODE", "true");
    expected.put("PLUGIN_REGION", "region");
    expected.put("PLUGIN_PATH_STYLE", "false");
    expected.put("PLUGIN_ARCHIVE_FORMAT", "gzip");
    expected.put("PLUGIN_BACKEND_OPERATION_TIMEOUT", "100s");
    expected.put("PLUGIN_OVERRIDE", "true");
    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
        pluginSettingUtils.getPluginCompatibleEnvVariables(saveCacheS3StepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetRestoreCacheGCSStepInfoEnvVariables() {
    RestoreCacheGCSStepInfo restoreCacheGCSStepInfo = RestoreCacheGCSStepInfo.builder()
                                                          .key(ParameterField.createValueField("key"))
                                                          .bucket(ParameterField.createValueField("bucket"))
                                                          .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_BACKEND", "gcs");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_CACHE_KEY", "key");
    expected.put("PLUGIN_RESTORE", "true");
    expected.put("PLUGIN_FAIL_RESTORE_IF_KEY_NOT_PRESENT", "false");
    expected.put("PLUGIN_EXIT_CODE", "true");
    expected.put("PLUGIN_ARCHIVE_FORMAT", "tar");
    expected.put("PLUGIN_BACKEND_OPERATION_TIMEOUT", "100s");
    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
        pluginSettingUtils.getPluginCompatibleEnvVariables(restoreCacheGCSStepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetRestoreCacheGCSStepInfoEnvVariablesArchiveSet() {
    RestoreCacheGCSStepInfo restoreCacheGCSStepInfo =
        RestoreCacheGCSStepInfo.builder()
            .key(ParameterField.createValueField("key"))
            .bucket(ParameterField.createValueField("bucket"))
            .archiveFormat(ParameterField.createValueField(ArchiveFormat.GZIP))
            .failIfKeyNotFound(ParameterField.createValueField(true))
            .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_BACKEND", "gcs");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_CACHE_KEY", "key");
    expected.put("PLUGIN_RESTORE", "true");
    expected.put("PLUGIN_FAIL_RESTORE_IF_KEY_NOT_PRESENT", "true");
    expected.put("PLUGIN_EXIT_CODE", "true");
    expected.put("PLUGIN_ARCHIVE_FORMAT", "gzip");
    expected.put("PLUGIN_BACKEND_OPERATION_TIMEOUT", "100s");
    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
        pluginSettingUtils.getPluginCompatibleEnvVariables(restoreCacheGCSStepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetSaveCacheGCSStepInfoEnvVariables() {
    SaveCacheGCSStepInfo saveCacheGCSStepInfo =
        SaveCacheGCSStepInfo.builder()
            .key(ParameterField.createValueField("key"))
            .bucket(ParameterField.createValueField("bucket"))
            .sourcePaths(ParameterField.createValueField(asList("path1", "path2")))
            .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_BACKEND", "gcs");
    expected.put("PLUGIN_MOUNT", "path1,path2");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_CACHE_KEY", "key");
    expected.put("PLUGIN_REBUILD", "true");
    expected.put("PLUGIN_EXIT_CODE", "true");
    expected.put("PLUGIN_ARCHIVE_FORMAT", "tar");
    expected.put("PLUGIN_OVERRIDE", "false");
    expected.put("PLUGIN_BACKEND_OPERATION_TIMEOUT", "100s");
    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
        pluginSettingUtils.getPluginCompatibleEnvVariables(saveCacheGCSStepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetSaveCacheGCSStepInfoEnvVariablesArchiveSet() {
    SaveCacheGCSStepInfo saveCacheGCSStepInfo =
        SaveCacheGCSStepInfo.builder()
            .key(ParameterField.createValueField("key"))
            .bucket(ParameterField.createValueField("bucket"))
            .sourcePaths(ParameterField.createValueField(asList("path1", "path2")))
            .archiveFormat(ParameterField.createValueField(ArchiveFormat.GZIP))
            .override(ParameterField.createValueField(false))
            .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_BACKEND", "gcs");
    expected.put("PLUGIN_MOUNT", "path1,path2");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_CACHE_KEY", "key");
    expected.put("PLUGIN_REBUILD", "true");
    expected.put("PLUGIN_EXIT_CODE", "true");
    expected.put("PLUGIN_ARCHIVE_FORMAT", "gzip");
    expected.put("PLUGIN_OVERRIDE", "false");
    expected.put("PLUGIN_BACKEND_OPERATION_TIMEOUT", "100s");
    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
        pluginSettingUtils.getPluginCompatibleEnvVariables(saveCacheGCSStepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetUploadToS3StepInfoEnvVariables() {
    UploadToS3StepInfo uploadToS3StepInfo = UploadToS3StepInfo.builder()
                                                .endpoint(ParameterField.createValueField("endpoint"))
                                                .region(ParameterField.createValueField("region"))
                                                .bucket(ParameterField.createValueField("bucket"))
                                                .sourcePath(ParameterField.createValueField("sources"))
                                                .target(ParameterField.createValueField("target"))
                                                .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_ENDPOINT", "endpoint");
    expected.put("PLUGIN_REGION", "region");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_SOURCE", "sources");
    expected.put("PLUGIN_TARGET", "target");
    expected.put("PLUGIN_ARTIFACT_FILE", "/addon/tmp/.plugin/artifact");
    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
        pluginSettingUtils.getPluginCompatibleEnvVariables(uploadToS3StepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetUploadToGCSStepInfoEnvVariables() {
    UploadToGCSStepInfo uploadToS3StepInfo = UploadToGCSStepInfo.builder()
                                                 .bucket(ParameterField.createValueField("bucket"))
                                                 .sourcePath(ParameterField.createValueField("pom.xml"))
                                                 .target(ParameterField.createValueField("dir/pom.xml"))
                                                 .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_SOURCE", "pom.xml");
    expected.put("PLUGIN_TARGET", "bucket/dir/pom.xml");
    expected.put("PLUGIN_ARTIFACT_FILE", "/addon/tmp/.plugin/artifact");

    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
        pluginSettingUtils.getPluginCompatibleEnvVariables(uploadToS3StepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = RAGHAV_GUPTA)
  @Category(UnitTests.class)
  public void shouldGetPluginCompatibleStepInfoBaseImageConnectorRefs() {
    PluginCompatibleStep stepInfo = ECRStepInfo.builder()
                                        .account(ParameterField.createValueField("6874654867"))
                                        .region(ParameterField.createValueField("eu-central-1"))
                                        .imageName(ParameterField.createValueField("harness"))
                                        .tags(ParameterField.createValueField(asList("tag1", "tag2")))
                                        .baseImageConnectorRefs(ParameterField.createValueField(asList("docker")))
                                        .build();

    List<String> expected = new ArrayList<>();
    expected.add("docker");
    List<String> actual = pluginSettingUtils.getBaseImageConnectorRefs(stepInfo);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void shouldGetGitClonePluginCompatibleStepInfoBuildTypeEnvVariables() {
    testGitCloneBuildParameters(BuildType.BRANCH, "myBranch");
    testGitCloneBuildParameters(BuildType.TAG, "myTag");
  }

  private void testGitCloneBuildParameters(BuildType buildType, String value) {
    final ParameterField<Build> buildParameter = createBuildParameter(buildType, value);
    final GitCloneStepInfo stepInfo = GitCloneStepInfo.builder()
            //.sslVerify(ParameterField.createValueField(false))
            .connectorRef(ParameterField.createValueField("myConnectorRef"))
            .build(buildParameter)
            .repoName(ParameterField.createValueField("myRepoName"))
            .build();
    Map<String, String> expected = new HashMap<>();

    if(buildType == BuildType.BRANCH) {
      expected.put(DRONE_COMMIT_BRANCH, value );
    } else if (BuildType.TAG == buildType) {
      expected.put(DRONE_TAG, value );
      expected.put(DRONE_BUILD_EVENT, TAG_BUILD_EVENT);
    }
    expected.put(GIT_SSL_NO_VERIFY, String.valueOf(false));

    Ambiance ambiance = Ambiance.newBuilder().build();
    Map<String, String> actual =
            pluginSettingUtils.getPluginCompatibleEnvVariables(stepInfo, "identifier", 100, ambiance, Type.K8);
    assertThat(actual).isEqualTo(expected);
  }

  private static ParameterField<Build> createBuildParameter(BuildType buildType, String value) {
    final ParameterField<String> buildStringParameter = ParameterField.<String>builder().value(value).build();
    BuildSpec buildSpec = null;
    if(BuildType.BRANCH == buildType) {
      buildSpec = BranchBuildSpec.builder().branch(buildStringParameter).build();
    } else if (BuildType.TAG == buildType) {
      buildSpec = TagBuildSpec.builder().tag(buildStringParameter).build();
    } else if (BuildType.PR == buildType) {
      buildSpec = PRBuildSpec.builder().number(buildStringParameter).build();
    }
    final Build build = Build.builder().spec(buildSpec).type(buildType).build();
    return ParameterField.<Build>builder().value(build).build();
  }

}
