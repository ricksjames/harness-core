/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.scm.errorhandling;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.ScmException;
import io.harness.exception.WingsException;
import io.harness.gitsync.ErrorDetails;
import io.harness.gitsync.scm.beans.ScmErrorDetails;

@OwnedBy(HarnessTeam.PL)
public abstract class ScmErrorHandler {
  abstract void handleError(int statusCode, ScmErrorDetails errorDetails);

  public final void handleIfError(int statusCode, ScmErrorDetails errorDetails) {
    if (statusCode < 400) {
      return;
    }

    handleError(statusCode, errorDetails);
  }

  public final ScmErrorDetails getScmErrorDetails(ErrorDetails errorDetails) {
    return ScmErrorDetails.builder().errorMessage(errorDetails.getErrorMessage()).build();
  }

  WingsException prepareException(Class<? extends ScmException> clazz, ScmErrorDetails scmErrorDetails)
      throws InstantiationException, IllegalAccessException {
    WingsException finalException = clazz.newInstance();
    if (isNotEmpty(scmErrorDetails.getExplanationMessage())) {
      finalException = new ExplanationException(scmErrorDetails.getExplanationMessage(), finalException);
    }
    if (isNotEmpty(scmErrorDetails.getHintMessage())) {
      finalException = new HintException(scmErrorDetails.getHintMessage(), finalException);
    }
    return finalException;
  }
}
