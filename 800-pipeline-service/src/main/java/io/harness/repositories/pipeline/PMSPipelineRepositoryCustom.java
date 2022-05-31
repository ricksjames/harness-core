/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.pms.pipeline.PipelineEntity;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PIPELINE)
public interface PMSPipelineRepositoryCustom {
  Page<PipelineEntity> findAll(Criteria criteria, Pageable pageable, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, boolean getDistinctFromBranches);

  Long countAllPipelines(Criteria criteria);

  PipelineEntity saveForOldGitSync(PipelineEntity pipelineToSave);

  /**
   * this method is to be used for new git experience, and for all pipelines that are not git synced in both old and new
   * flows
   */
  PipelineEntity save(PipelineEntity pipelineToSave);

  Optional<PipelineEntity> findForOldGitSync(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, boolean notDeleted);

  /**
   * this method is to be used for new git experience, and for all pipelines that are not git synced in both old and new
   * flows
   */
  Optional<PipelineEntity> find(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, boolean notDeleted, boolean getMetadataOnly);

  PipelineEntity updatePipelineYamlForOldGitSync(
      PipelineEntity pipelineToUpdate, PipelineEntity oldPipelineEntity, ChangeType changeType);

  /**
   * this method is to be used for new git experience, and for all pipelines that are not git synced in both old and new
   * flows
   */
  PipelineEntity updatePipelineYaml(PipelineEntity pipelineToUpdate);

  PipelineEntity updatePipelineMetadata(
      String accountId, String orgIdentifier, String projectIdentifier, Criteria criteria, Update update);

  PipelineEntity deleteForOldGitSync(PipelineEntity pipelineToUpdate);

  /**
   * this method is to be used for new git experience, and for all pipelines that are not git synced in both old and new
   * flows
   */
  PipelineEntity delete(String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier);

  String importPipelineFromRemote(String accountId, String orgIdentifier, String projectIdentifier);
}
