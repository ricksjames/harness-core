/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.helper;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.ScmBadRequestException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ScmRequestValidationHelper {
  public void isEmptyParam(String value, String paramName) {
    if (isEmpty(value)) {
      throw new ScmBadRequestException(paramName + " is empty");
    }
  }
}
