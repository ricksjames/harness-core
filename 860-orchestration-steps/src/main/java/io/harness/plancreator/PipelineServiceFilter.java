package io.harness.plancreator;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.pipeline.filter.PipelineFilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class PipelineServiceFilter implements PipelineFilter {
  @Singular Set<String> moduleNames;

  public void addModuleNames(PipelineServiceFilter currentFilter) {
    if (this.moduleNames == null) {
      this.moduleNames = new HashSet<>();
    } else if (!(this.moduleNames instanceof HashSet)) {
      this.moduleNames = new HashSet<>(this.moduleNames);
    }

    if (EmptyPredicate.isNotEmpty(currentFilter.getModuleNames())) {
      moduleNames.addAll(currentFilter.getModuleNames());
    }
  }
}
