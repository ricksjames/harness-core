package software.wings.service.intfc;

import software.wings.beans.JenkinsConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by peeyushaggarwal on 1/12/17.
 */
public interface JenkinsBuildService extends BuildService<JenkinsConfig> {
  @DelegateTaskType(TaskType.JENKINS_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, JenkinsConfig config,
      List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.JENKINS_GET_JOBS)
  List<JobDetails> getJobs(
      JenkinsConfig jenkinsConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName);

  @DelegateTaskType(TaskType.JENKINS_GET_ARTIFACT_PATHS)
  List<String> getArtifactPaths(
      String jobName, String groupId, JenkinsConfig config, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.JENKINS_LAST_SUCCESSFUL_BUILD)
  BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      JenkinsConfig config, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.JENKINS_GET_PLANS)
  Map<String, String> getPlans(JenkinsConfig config, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.JENKINS_VALIDATE_ARTIFACT_SERVER)
  boolean validateArtifactServer(JenkinsConfig jenkinsConfig);

  @DelegateTaskType(TaskType.JENKINS_GET_JOB)
  JobDetails getJob(String jobName, JenkinsConfig jenkinsConfig, List<EncryptedDataDetail> encryptionDetails);
}
