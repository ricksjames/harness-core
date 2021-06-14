package io.harness.resourcegroup.framework.service;

import io.harness.resourcegroup.model.ResourceGroup;

public interface ResourceGroupValidatorService {
  boolean validateAndFilterInvalidResourceSelectors(ResourceGroup resourceGroup);
}
