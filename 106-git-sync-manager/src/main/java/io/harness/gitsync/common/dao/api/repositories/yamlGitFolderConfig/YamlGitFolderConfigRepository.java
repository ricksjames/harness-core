package io.harness.gitsync.common.dao.api.repositories.yamlGitFolderConfig;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.EntityScope.Scope;
import io.harness.gitsync.common.beans.YamlGitFolderConfig;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface YamlGitFolderConfigRepository extends PagingAndSortingRepository<YamlGitFolderConfig, String> {
  List<YamlGitFolderConfig> findByAccountIdAndOrganizationIdAndProjectIdAndScopeOrderByCreatedAtAsc(
      String accountId, String organizationIdentifier, String projectIdentifier, Scope scope);

  List<YamlGitFolderConfig> findByAccountIdAndOrganizationIdAndProjectIdAndScopeAndYamlGitConfigIdOrderByCreatedAtAsc(
      String accountId, String organizationId, String projectId, Scope scope, String identifier);

  Optional<YamlGitFolderConfig> findByAccountIdAndOrganizationIdAndProjectIdAndScopeAndIsDefault(
      String accountId, String organizationId, String projectId, Scope scope, boolean isDefault);

  Optional<YamlGitFolderConfig> findByUuidAndAccountIdAndEnabled(String identifier, String accountId, boolean enabled);
}
