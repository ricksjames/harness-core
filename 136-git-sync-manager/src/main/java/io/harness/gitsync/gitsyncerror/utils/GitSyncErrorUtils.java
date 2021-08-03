package io.harness.gitsync.gitsyncerror.utils;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType;
import io.harness.gitsync.gitsyncerror.beans.GitToHarnessErrorDetails;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class GitSyncErrorUtils {
  public static final String EMPTY_STR = "";
  public static final Long DEFAULT_COMMIT_TIME = 0L;

  public static boolean isGitToHarnessSyncError(GitSyncError gitSyncError) {
    return GitSyncErrorType.GIT_TO_HARNESS == gitSyncError.getErrorType();
  }

  public static String getCommitIdOfError(GitSyncError error) {
    if (isGitToHarnessSyncError(error)) {
      GitToHarnessErrorDetails gitToHarnessErrorDetails = (GitToHarnessErrorDetails) error.getAdditionalErrorDetails();
      return gitToHarnessErrorDetails.getGitCommitId();
    }
    log.warn("The commitId is specific to the git to harness error, it should not be called for harness to git");
    return EMPTY_STR;
  }

  public static String getYamlContentOfError(GitSyncError error) {
    if (isGitToHarnessSyncError(error)) {
      GitToHarnessErrorDetails gitToHarnessErrorDetails = (GitToHarnessErrorDetails) error.getAdditionalErrorDetails();
      return gitToHarnessErrorDetails.getYamlContent();
    }
    log.warn("The yaml content is specific to the git to harness error, it should not be called for harness to git");
    return EMPTY_STR;
  }

  public static Long getCommitTimeOfError(GitSyncError error) {
    if (isGitToHarnessSyncError(error)) {
      GitToHarnessErrorDetails gitToHarnessErrorDetails = (GitToHarnessErrorDetails) error.getAdditionalErrorDetails();
      return gitToHarnessErrorDetails.getCommitTime();
    }

    log.warn("The commitTime is specific to the git to harness error, it should not be called for harness to git");
    return DEFAULT_COMMIT_TIME;
  }

  public static String getCommitMessageOfError(GitSyncError error) {
    if (isGitToHarnessSyncError(error)) {
      GitToHarnessErrorDetails gitToHarnessErrorDetails = (GitToHarnessErrorDetails) error.getAdditionalErrorDetails();
      return gitToHarnessErrorDetails.getCommitMessage();
    }

    log.warn("The commitMessage is specific to the git to harness error, it should not be called for harness to git");
    return EMPTY_STR;
  }

  public static void setYamlContent(GitSyncError error) {
    if (isGitToHarnessSyncError(error)) {
      GitToHarnessErrorDetails gitToHarnessErrorDetails = (GitToHarnessErrorDetails) error.getAdditionalErrorDetails();
      gitToHarnessErrorDetails.setYamlContent(null);
    }
  }
}
