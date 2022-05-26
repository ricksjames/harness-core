package io.harness.ccm.views.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovePerspectiveDTO {
  String newFolderId;
  List<String> perspectiveIds;
}
