/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.commons.entities.anomaly;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnomalyData {
  String id;
  Long time;
  String anomalyRelativeTime;
  Double actualAmount;
  Double expectedAmount;
  Double trend;
  String resourceName;
  String resourceInfo;
  EntityInfo entity;
  String details;
  String status;
  String statusRelativeTime;
  String comment;
  Double anomalyScore;
  AnomalyFeedback userFeedback;
}
