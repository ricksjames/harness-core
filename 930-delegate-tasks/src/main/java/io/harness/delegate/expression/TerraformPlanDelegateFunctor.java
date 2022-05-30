/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.expression;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateFileManagerBase;
import io.harness.delegate.beans.FileBucket;
import io.harness.exception.FunctorException;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import io.harness.expression.ExpressionFunctor;
import io.harness.terraform.expression.TerraformPlanExpressionInterface;

import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import lombok.Builder;
import org.apache.commons.io.FileUtils;

@Builder
@OwnedBy(CDP)
public class TerraformPlanDelegateFunctor implements ExpressionFunctor {
  private static final String JSON_PLAN_PREFIX = "tfPlan";
  private static final String JSON_PLAN_SUFFIX = ".json";

  @Builder
  static class TerraformPlan implements TerraformPlanExpressionInterface {
    private String jsonPlanFilePath;

    @Override
    public String jsonFilePath() {
      return jsonPlanFilePath;
    }

    public void cleanup() {
      FileUtils.deleteQuietly(new File(jsonPlanFilePath));
    }
  }

  private final Map<String, TerraformPlan> cache = new HashMap<>();

  @Inject private DelegateFileManagerBase delegateFileManager;

  private int expressionFunctorToken;
  private String accountId;

  public TerraformPlan obtainPlan(String fileId, int token) {
    if (expressionFunctorToken != token) {
      throw new FunctorException("Inappropriate usage of internal functor");
    }

    if (cache.containsKey(fileId)) {
      return cache.get(fileId);
    }

    TerraformPlan tfPlan = downloadJsonPlan(fileId);
    cache.put(fileId, tfPlan);
    return tfPlan;
  }

  public void cleanup() {
    if (isNotEmpty(cache)) {
      cache.values().forEach(TerraformPlan::cleanup);
    }
  }

  private TerraformPlan downloadJsonPlan(String fileId) {
    try (InputStream inputStream =
             delegateFileManager.downloadByFileId(FileBucket.TERRAFORM_PLAN_JSON, fileId, accountId);
         InputStream decompressInputStream = new GZIPInputStream(inputStream)) {
      Path outputFilePath = Files.createTempFile(JSON_PLAN_PREFIX, JSON_PLAN_SUFFIX);
      Files.copy(decompressInputStream, outputFilePath, StandardCopyOption.REPLACE_EXISTING);

      return TerraformPlan.builder().jsonPlanFilePath(outputFilePath.toString()).build();
    } catch (IOException e) {
      throw new FunctorException(
          format("Failed to download file '%s'", fileId), ExceptionMessageSanitizer.sanitizeException(e));
    }
  }
}
