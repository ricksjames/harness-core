package io.harness.accesscontrol.roleassignments.migration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.harness.HarnessScopeLevel;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureFlag;
import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagClientHttpFactory;
import io.harness.ff.FeatureFlagDTO;
//import io.harness.ff.FeatureFlagService;
import io.harness.ff.FeatureFlagsClient;
import io.harness.migration.NGMigration;
import io.harness.utils.CryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;

import java.util.List;
import java.util.Optional;

import static io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupConstants.DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.remote.client.RestClientUtils.getResponse;
import static org.springframework.data.mongodb.core.query.Update.update;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PL)
public class AccountBasicRoleAssignmentAdditionMigration implements NGMigration {
    private final RoleAssignmentRepository roleAssignmentRepository;
    //private FeatureFlagService featureFlagService;
    private final  FeatureFlagsClient featureFlagsClient;

    @Inject
    public AccountBasicRoleAssignmentAdditionMigration(RoleAssignmentRepository roleAssignmentRepository, FeatureFlagsClient featureFlagsClient) {//, FeatureFlagService featureFlagService
        this.roleAssignmentRepository = roleAssignmentRepository;
        //this.featureFlagService = featureFlagService;
        this.featureFlagsClient = featureFlagsClient;
    }

    @Override
    public void migrate() {
        log.info("AccountBasicRoleAssignmentAdditionMigration starts ...");
        FeatureFlagDTO featureFlag = getResponse(featureFlagsClient.getFeatureFlagName(FeatureName.ACCOUNT_BASIC_ROLE.name())); //featureFlagService.getFeatureFlag(FeatureName.ACCOUNT_BASIC_ROLE);
        //if(optionalFeatureFlag.isPresent()){
            //FeatureFlag featureFlag = optionalFeatureFlag.get();
            for (String accountId: featureFlag.getAccountIds()) {
                Scope accountScope = Scope.builder().instanceId(accountId).level(HarnessScopeLevel.ACCOUNT).build();
                int pageSize = 1000;
                int pageIndex = 0;
                Pageable pageable = PageRequest.of(pageIndex, pageSize);
                Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.roleIdentifier).is("_account_viewer")
                        .and(RoleAssignmentDBOKeys.resourceGroupIdentifier).is(DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER)
                        .and(RoleAssignmentDBOKeys.scopeLevel).is(accountScope.getLevel().getResourceType())
                        .and(RoleAssignmentDBOKeys.scopeIdentifier).is(accountScope.toString())
                        .and(RoleAssignmentDBOKeys.managed).is(true);

                do {
                    List<RoleAssignmentDBO> roleAssignmentList = roleAssignmentRepository.findAll(criteria, pageable).getContent();
                    if (isEmpty(roleAssignmentList)) {
                        log.info("AccountBasicRoleAssignmentAdditionMigration completed.");
                        return;
                    }
                    for (RoleAssignmentDBO roleAssignment : roleAssignmentList) {
                        RoleAssignmentDBO newRoleAssignmentDBO = buildRoleAssignmentDBO(roleAssignment);
                        try {
                            roleAssignmentRepository.save(newRoleAssignmentDBO);
                            roleAssignmentRepository.updateById(
                                    roleAssignment.getId(), update(RoleAssignmentDBOKeys.managed, false));
                        } catch (Exception exception) {
                            log.error("[AccountBasicRoleAssignmentAdditionMigration] Unexpected error occurred.", exception);
                        }
                    }

                    pageIndex++;
                }
                while (true);
            }
        //}
    }

    private RoleAssignmentDBO buildRoleAssignmentDBO(RoleAssignmentDBO roleAssignmentDBO) {
        return   RoleAssignmentDBO.builder()
                .identifier("role_assignment_".concat(CryptoUtils.secureRandAlphaNumString(20)))
                .scopeIdentifier(roleAssignmentDBO.getScopeIdentifier())
                .scopeLevel(roleAssignmentDBO.getScopeLevel())
                .disabled(roleAssignmentDBO.isDisabled())
                .managed(true)
                .roleIdentifier("_account_basic")
                .resourceGroupIdentifier(roleAssignmentDBO.getResourceGroupIdentifier())
                .principalScopeLevel(roleAssignmentDBO.getPrincipalScopeLevel())
                .principalIdentifier(roleAssignmentDBO.getPrincipalIdentifier())
                .principalType(roleAssignmentDBO.getPrincipalType())
                .build();
    }
}
