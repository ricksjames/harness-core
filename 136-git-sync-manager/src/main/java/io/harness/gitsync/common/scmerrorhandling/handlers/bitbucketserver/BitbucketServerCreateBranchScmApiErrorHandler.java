/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.scmerrorhandling.handlers.bitbucketserver;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.ScmBadRequestException;
import io.harness.exception.ScmUnauthorizedException;
import io.harness.exception.ScmUnexpectedException;
import io.harness.exception.WingsException;
import io.harness.gitsync.common.scmerrorhandling.handlers.ScmApiErrorHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PL)
public class BitbucketServerCreateBranchScmApiErrorHandler implements ScmApiErrorHandler {
  public static final String CREATE_BRANCH_FAILED_MESSAGE =
      "The requested branch could not be created on Bitbucket Server. ";
  public static final String CREATE_BRANCH_UNPROCESSABLE_ENTITY_ERROR_HINT =
      "Please check that the requested Bitbucket branch name is valid and does not already exist.";
  public static final String CREATE_BRANCH_UNPROCESSABLE_ENTITY_ERROR_EXPLANATION = "Possible reasons can be:\n"
      + "1. A branch with given name already exists in the remote Bitbucket repository.\n"
      + "2. The given branch name is invalid.";
  public static final String CREATE_BRANCH_NOT_FOUND_ERROR_HINT =
      "Please check if the requested Bitbucket repository or base branch exists.";
  public static final String CREATE_BRANCH_NOT_FOUND_ERROR_EXPLANATION = "Possible reasons can be:\n"
      + "1. The given bitbucket repository is invalid.\n"
      + "2. The given base branch does not exists.";
  @Override
  public void handleError(int statusCode, String errorMessage) throws WingsException {
    switch (statusCode) {
      case 401:
      case 403:
        throw NestedExceptionUtils.hintWithExplanationException(ScmErrorHints.INVALID_CREDENTIALS,
            CREATE_BRANCH_FAILED_MESSAGE + ScmErrorExplanations.INVALID_CONNECTOR_CREDS,
            new ScmUnauthorizedException(errorMessage));
      case 400:
        throw NestedExceptionUtils.hintWithExplanationException(CREATE_BRANCH_UNPROCESSABLE_ENTITY_ERROR_HINT,
            CREATE_BRANCH_FAILED_MESSAGE + CREATE_BRANCH_UNPROCESSABLE_ENTITY_ERROR_EXPLANATION,
            new ScmBadRequestException(errorMessage));
      case 404:
        throw NestedExceptionUtils.hintWithExplanationException(CREATE_BRANCH_NOT_FOUND_ERROR_HINT,
            CREATE_BRANCH_FAILED_MESSAGE + CREATE_BRANCH_NOT_FOUND_ERROR_EXPLANATION,
            new ScmBadRequestException(errorMessage));
      case 409:
        throw NestedExceptionUtils.hintWithExplanationException(ScmErrorHints.BRANCH_ALREADY_EXISTS,
            CREATE_BRANCH_FAILED_MESSAGE + ScmErrorExplanations.BRANCH_ALREADY_EXISTS,
            new ScmBadRequestException(errorMessage));
      default:
        log.error(String.format("Error while creating bitbucket(server) branch: [%s: %s] ", statusCode, errorMessage));
        throw new ScmUnexpectedException(errorMessage);
    }
  }
}
