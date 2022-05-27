/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.scmerrorhandling.handlers.github;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.SCMExceptionErrorMessages;
import io.harness.exception.ScmBadRequestException;
import io.harness.exception.ScmUnauthorizedException;
import io.harness.exception.ScmUnexpectedException;
import io.harness.exception.WingsException;
import io.harness.gitsync.common.scmerrorhandling.handlers.ScmApiErrorHandler;

@OwnedBy(PL)
public class GithubCreatePullRequestScmApiErrorHandler implements ScmApiErrorHandler {
  public static final String CREATE_PULL_REQUEST_FAILURE = "The pull request could not be created in Github. ";
  public static final String CREATE_PULL_REQUEST_VALIDATION_FAILED_EXPLANATION = "Please check the following:\n"
      + "1. If already a pull request exists for request source branch to target branch.\n"
      + "2. If source branch and target branch both exists in Github repository.\n"
      + "3. If title of the pull request is empty.";
  public static final String CREATE_PULL_REQUEST_VALIDATION_FAILED_HINT =
      "There was issue while creating pull request. Possible reasons can be:\n"
      + "1. There is already an open pull request from source branch to target branch for given Github repository.\n"
      + "2. The source branch or target branch doesn't exist for given Github repository.\n"
      + "3. The title of the pull request is empty";

  @Override
  public void handleError(int statusCode, String errorMessage) throws WingsException {
    switch (statusCode) {
      case 401:
      case 403:
        throw NestedExceptionUtils.hintWithExplanationException(ScmErrorHints.INVALID_CREDENTIALS,
            CREATE_PULL_REQUEST_FAILURE + ScmErrorExplanations.INVALID_CONNECTOR_CREDS,
            new ScmUnauthorizedException(errorMessage));
      case 404:
        throw NestedExceptionUtils.hintWithExplanationException(ScmErrorHints.REPO_NOT_FOUND,
            CREATE_PULL_REQUEST_FAILURE + ScmErrorExplanations.REPO_NOT_FOUND,
            new ScmBadRequestException(SCMExceptionErrorMessages.REPOSITORY_NOT_FOUND_ERROR));
      case 422:
        throw NestedExceptionUtils.hintWithExplanationException(CREATE_PULL_REQUEST_VALIDATION_FAILED_HINT,
            CREATE_PULL_REQUEST_VALIDATION_FAILED_EXPLANATION,
            new ScmBadRequestException(SCMExceptionErrorMessages.CREATE_PULL_REQUEST_VALIDATION_FAILED));
      default:
        throw new ScmUnexpectedException(errorMessage);
    }
  }
}
