/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.common.beans.GitSyncSettings.IS_EXECUTE_ON_DELEGATE;
import static io.harness.gitsync.common.beans.GitSyncSettings.IS_GIT_SIMPLIFICATION_ENABLED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.beans.GitSyncSettings;
import io.harness.gitsync.common.beans.GitSyncSettings.GitSyncSettingsKeys;
import io.harness.gitsync.common.dtos.GitSyncSettingsDTO;
import io.harness.gitsync.common.remote.GitSyncSettingsMapper;
import io.harness.gitsync.common.service.GitSyncSettingsService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.repositories.gitSyncSettings.GitSyncSettingsRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class GitSyncSettingsServiceImpl implements GitSyncSettingsService {
  private final GitSyncSettingsRepository gitSyncSettingsRepository;
  private final YamlGitConfigService yamlGitConfigService;

  @Override
  public Optional<GitSyncSettingsDTO> get(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    final Optional<GitSyncSettings> gitSyncSettings =
        gitSyncSettingsRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier);
    return gitSyncSettings.map(GitSyncSettingsMapper::getDTOFromGitSyncSettings);
  }

  @Override
  public GitSyncSettingsDTO save(GitSyncSettingsDTO request) {
    GitSyncSettings gitSyncSettings = GitSyncSettingsMapper.getGitSyncSettingsFromDTO(request);
    GitSyncSettings savedGitSyncSettings = null;
    try {
      savedGitSyncSettings = gitSyncSettingsRepository.save(gitSyncSettings);
    } catch (DuplicateKeyException ex) {
      throw new io.harness.exception.InvalidRequestException(
          String.format("A git sync settings already exists in the project %s in the org %s",
              request.getProjectIdentifier(), request.getOrgIdentifier()));
    }
    return GitSyncSettingsMapper.getDTOFromGitSyncSettings(savedGitSyncSettings);
  }

  @Override
  public GitSyncSettingsDTO update(GitSyncSettingsDTO request) {
    Criteria criteria = Criteria.where(GitSyncSettingsKeys.accountIdentifier)
                            .is(request.getAccountIdentifier())
                            .and(GitSyncSettingsKeys.orgIdentifier)
                            .is(request.getOrgIdentifier())
                            .and(GitSyncSettingsKeys.projectIdentifier)
                            .is(request.getProjectIdentifier());
    Map<String, String> settings = new HashMap<>();
    settings.put(
        IS_EXECUTE_ON_DELEGATE, (request.isExecuteOnDelegate()) ? String.valueOf(true) : String.valueOf(false));
    Update update = new Update().set(GitSyncSettingsKeys.settings, settings);
    final GitSyncSettings updatedGitSyncSettings = gitSyncSettingsRepository.update(criteria, update);
    return GitSyncSettingsMapper.getDTOFromGitSyncSettings(updatedGitSyncSettings);
  }

  @Override
  public void delete(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    gitSyncSettingsRepository.deleteByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier);
  }

  @Override
  public boolean enableGitSimplification(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (yamlGitConfigService.isGitSyncEnabled(accountIdentifier, orgIdentifier, projectIdentifier)) {
      throwExceptionIfGitSyncAlreadyEnabled();
    }

    if (!isGitSimplificationEnabled(accountIdentifier, orgIdentifier, projectIdentifier)) {
      GitSyncSettings gitSyncSettings =
          getGitSyncSettingsForGitSimplification(accountIdentifier, orgIdentifier, projectIdentifier);
      try {
        gitSyncSettingsRepository.save(gitSyncSettings);
      } catch (DuplicateKeyException ex) {
        return isGitSimplificationEnabled(accountIdentifier, orgIdentifier, projectIdentifier);
      }
    }
    return true;
  }

  @Override
  public boolean getGitSimplificationStatus(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    try {
      return isGitSimplificationEnabled(accountIdentifier, orgIdentifier, projectIdentifier);
    } catch (InvalidRequestException ex) {
      return false;
    }
  }

  private boolean isGitSimplificationEnabled(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<GitSyncSettingsDTO> optionalGitSyncSettingsDTO = get(accountIdentifier, orgIdentifier, projectIdentifier);
    if (optionalGitSyncSettingsDTO.isPresent()) {
      GitSyncSettingsDTO gitSyncSettings = optionalGitSyncSettingsDTO.get();
      if (gitSyncSettings.isGitSimplificationEnabled()) {
        return true;
      }
      throwExceptionIfGitSyncAlreadyEnabled();
    }
    return false;
  }

  private GitSyncSettings getGitSyncSettingsForGitSimplification(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Map<String, String> settings = new HashMap<>();
    settings.put(IS_GIT_SIMPLIFICATION_ENABLED, String.valueOf(true));
    return GitSyncSettings.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .settings(settings)
        .build();
  }

  private void throwExceptionIfGitSyncAlreadyEnabled() {
    throw new InvalidRequestException(
        "Git Management experience is already enabled in this project, cannot enable Git Simplification experience on the same project. Please try with a new project.");
  }
}
