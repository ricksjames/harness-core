package io.harness.ccm.views.utils;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewPreferences;

import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CE)
public class CEViewPreferenceUtils {
  public ViewPreferences getCEViewPreferences(final CEView ceView) {
    final List<ViewFieldIdentifier> dataSources = ceView.getDataSources();
    return ViewPreferences.builder()
        .includeOthers(true)
        .includeUnallocatedCost(Objects.nonNull(dataSources) && dataSources.contains(ViewFieldIdentifier.CLUSTER)
            && dataSources.size() == 1)
        .build();
  }
}
