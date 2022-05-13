/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.beans.FeatureName.USE_IMMUTABLE_DELEGATE;
import static io.harness.persistence.HQuery.excludeAuthority;

import static software.wings.beans.Account.AccountKeys;
import static software.wings.beans.LicenseInfo.LicenseInfoKeys;

import io.harness.ff.FeatureFlagService;
import io.harness.migrations.SeedDataMigration;
import io.harness.persistence.HPersistence;

import software.wings.beans.Account;
import software.wings.beans.AccountType;

import com.google.inject.Inject;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ImmutableDelegateForCommunityUsersMigration implements SeedDataMigration {
  private final HPersistence persistence;
  private final FeatureFlagService featureFlagService;

  @Override
  public void migrate() {
    final List<Account> accountIds =
        persistence.createQuery(Account.class, excludeAuthority)
            .filter(AccountKeys.licenseInfo + "." + LicenseInfoKeys.accountType, AccountType.COMMUNITY)
            .project(AccountKeys.uuid, true)
            .asList();
    accountIds.stream()
        .map(Account::getUuid)
        .forEach(accountId -> featureFlagService.enableAccount(USE_IMMUTABLE_DELEGATE, accountId));
  }
}
