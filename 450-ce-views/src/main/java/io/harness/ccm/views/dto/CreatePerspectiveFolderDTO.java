package io.harness.ccm.views.dto;

import io.harness.ccm.views.entities.CEViewFolder;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePerspectiveFolderDTO {
  CEViewFolder ceViewFolder;
  List<String> perspectiveIds;
}
