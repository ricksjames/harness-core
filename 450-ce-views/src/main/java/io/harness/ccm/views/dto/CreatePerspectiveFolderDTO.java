package io.harness.ccm.views.dto;

import io.harness.ccm.views.entities.CEViewFolder;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import javax.validation.Valid;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePerspectiveFolderDTO {
  @Valid CEViewFolder ceViewFolder;
  List<String> perspectiveIds;
}
