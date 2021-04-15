package io.harness.audit.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.mapper.YamlDiffRecordMapper.toDTO;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.api.AuditService;
import io.harness.audit.api.AuditYamlService;
import io.harness.audit.api.impl.AuditPermissionValidator;
import io.harness.audit.beans.YamlDiffRecordDTO;
import io.harness.audit.entities.AuditEvent;
import io.harness.audit.mapper.ResourceScopeMapper;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@OwnedBy(PL)
@Api("auditYaml")
@Path("auditYaml")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class AuditYamlResource {
  @Inject private final AuditService auditService;
  @Inject private final AuditYamlService auditYamlService;
  @Inject private final AuditPermissionValidator auditPermissionValidator;

  @GET
  @ApiOperation(value = "Get Yaml Diff for an audit", nickname = "getYamlDiff")
  public ResponseDTO<YamlDiffRecordDTO> get(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY)
                                            String accountIdentifier, @NotNull @QueryParam("auditId") String auditId) {
    AuditEvent auditEvent =
        auditService.get(accountIdentifier, auditId)
            .orElseThrow(() -> new InvalidRequestException(String.format("No audit found with auditId %s", auditId)));
    auditPermissionValidator.validate(accountIdentifier, ResourceScopeMapper.toDTO(auditEvent.getResourceScope()));
    return ResponseDTO.newResponse(toDTO(auditYamlService.get(auditId)));
  }
}
