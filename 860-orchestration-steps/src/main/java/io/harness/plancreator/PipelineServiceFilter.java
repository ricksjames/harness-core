package io.harness.plancreator;

import io.harness.pms.pipeline.filter.PipelineFilter;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class PipelineServiceFilter implements PipelineFilter {
  @Singular List<String> moduleNames;
}
