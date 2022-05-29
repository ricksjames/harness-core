/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.overview.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.dashboards.DeploymentStatsSummary;
import io.harness.dashboards.EnvCount;
import io.harness.dashboards.PipelinesExecutionDashboardInfo;
import io.harness.dashboards.ProjectsDashboardInfo;
import io.harness.dashboards.ServicesCount;
import io.harness.dashboards.ServicesDashboardInfo;
import io.harness.dashboards.SortBy;
import io.harness.ng.core.OrgProjectIdentifier;
import io.harness.pms.dashboards.GroupBy;

import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(PIPELINE)
public interface CDLandingDashboardService {
  ServicesDashboardInfo getActiveServices(@NotNull String accountIdentifier,
      @NotNull List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval,
      @NotNull SortBy sortBy);

  ProjectsDashboardInfo getTopProjects(
      String accountIdentifier, List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval);

  ServicesCount getServicesCount(
      String accountIdentifier, List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval);

  EnvCount getEnvCount(
      String accountIdentifier, List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval);

  PipelinesExecutionDashboardInfo getActiveDeploymentStats(
      String accountIdentifier, List<OrgProjectIdentifier> orgProjectIdentifiers);

  DeploymentStatsSummary getDeploymentStatsSummary(String accountIdentifier,
      List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval, GroupBy groupBy);
}
