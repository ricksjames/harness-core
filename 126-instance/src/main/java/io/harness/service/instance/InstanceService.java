/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.service.instance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.InstanceDTO;
import io.harness.models.CountByServiceIdAndEnvType;
import io.harness.models.EnvBuildInstanceCount;
import io.harness.models.InstancesByBuildId;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.DX)
public interface InstanceService {
  InstanceDTO save(InstanceDTO instanceDTO);

  List<InstanceDTO> saveAll(List<InstanceDTO> instanceDTOList);

  Optional<InstanceDTO> saveOrReturnEmptyIfAlreadyExists(InstanceDTO instanceDTO);

  void deleteById(String id);

  void deleteAll(List<InstanceDTO> instanceDTOList);

  Optional<InstanceDTO> softDelete(String instanceKey);

  Optional<InstanceDTO> findAndReplace(InstanceDTO instanceDTO);

  List<InstanceDTO> getActiveInstancesByAccount(String accountIdentifier, long timestamp);

  List<InstanceDTO> getInstancesDeployedInInterval(String accountIdentifier, long startTimestamp, long endTimeStamp);

  List<InstanceDTO> getInstancesDeployedInInterval(
      String accountIdentifier, String organizationId, String projectId, long startTimestamp, long endTimeStamp);

  List<InstanceDTO> getInstances(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String infrastructureMappingId);

  List<InstanceDTO> getActiveInstances(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, long timestampInMs);

  List<InstanceDTO> getActiveInstancesByServiceId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId, long timestampInMs);

  List<InstanceDTO> getActiveInstancesByInfrastructureMappingId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String infrastructureMappingId);

  List<InstanceDTO> getActiveInstancesByInstanceInfo(
      String accountIdentifier, String instanceInfoNamespace, String instanceInfoPodName);

  AggregationResults<EnvBuildInstanceCount> getEnvBuildInstanceCountByServiceId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId, long timestampInMs);

  AggregationResults<InstancesByBuildId> getActiveInstancesByServiceIdEnvIdAndBuildIds(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String serviceId, String envId, List<String> buildIds,
      long timestampInMs, int limit);

  AggregationResults<CountByServiceIdAndEnvType> getActiveServiceInstanceCountBreakdown(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, List<String> serviceId, long timestampInMs);

  InstanceDTO findFirstInstance(Criteria criteria);
}
