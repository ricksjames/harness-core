package io.harness;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.AbstractModule;

import io.harness.annotations.dev.OwnedBy;
import io.harness.service.GraphGenerationService;
import io.harness.service.impl.GraphGenerationServiceImpl;

@OwnedBy(CDC)
public class OrchestrationVisualizationModule extends AbstractModule {
  private static OrchestrationVisualizationModule instance;

  public static OrchestrationVisualizationModule getInstance() {
    if (instance == null) {
      instance = new OrchestrationVisualizationModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(GraphGenerationService.class).to(GraphGenerationServiceImpl.class);
  }
}
