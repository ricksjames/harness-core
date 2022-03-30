/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.principals.usergroups;

import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.accesscontrol.scopes.core.ScopeHelper.toParentScope;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.common.validation.ValidationResult;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.principals.PrincipalValidator;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;

@OwnedBy(PL)
@Singleton
@ValidateOnExecution
public class UserGroupValidator implements PrincipalValidator {
  private final UserGroupService userGroupService;
  private final ScopeService scopeService;

  @Inject
  public UserGroupValidator(UserGroupService userGroupService, ScopeService scopeService) {
    this.userGroupService = userGroupService;
    this.scopeService = scopeService;
  }

  @Override
  public PrincipalType getPrincipalType() {
    return USER_GROUP;
  }

  @Override
  public ValidationResult validatePrincipal(Principal principal, String scopeIdentifier) {
    Scope scope =
        toParentScope(scopeService.buildScopeFromScopeIdentifier(scopeIdentifier), principal.getPrincipalScopeLevel());
    String principalScopeIdentifier = scope == null ? scopeIdentifier : scope.toString();
    String identifier = principal.getPrincipalIdentifier();
    Optional<UserGroup> userGroupOptional = userGroupService.get(identifier, principalScopeIdentifier);
    if (userGroupOptional.isPresent()) {
      return ValidationResult.builder().valid(true).build();
    }
    return ValidationResult.builder()
        .valid(false)
        .errorMessage(String.format(
            "user group not found with the given identifier %s in the scope %s", identifier, scopeIdentifier))
        .build();
  }
}
