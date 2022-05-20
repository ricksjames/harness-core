/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.remote.resources.perspectives;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.utils.LogAccountIdentifier;
import io.harness.ccm.views.dto.ViewFolderQueryDTO;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.CEViewFolder;
import io.harness.ccm.views.entities.ViewType;
import io.harness.ccm.views.graphql.QLCEView;
import io.harness.ccm.views.service.CEViewFolderService;
import io.harness.ccm.views.service.CEViewService;
import io.harness.enforcement.client.annotation.FeatureRestrictionCheck;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;
import io.jsonwebtoken.lang.Collections;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.CE;

@Api("perspectiveFolders")
@Path("perspectiveFolders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@NextGenManagerAuth
@Slf4j
@Service
@OwnedBy(CE)
@Tag(name = "Cloud Cost Perspectives Folders",
    description = "Group your Perspectives using Folders in ways that are more meaningful to your business needs.")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content = { @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FailureDTO.class)) })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content = { @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDTO.class)) })
public class PerspectiveFolderResource {
  private final CEViewFolderService ceViewFolderService;
  private final CEViewService ceViewService;

  @Inject
  public PerspectiveFolderResource(CEViewFolderService ceViewFolderService, CEViewService ceViewService) {
    this.ceViewFolderService = ceViewFolderService;
    this.ceViewService = ceViewService;
  }

  @POST
  @Path("create")
  @Timed
  @ExceptionMetered
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create perspective folder", nickname = "createPerspectiveFolder")
  @FeatureRestrictionCheck(FeatureRestrictionName.PERSPECTIVES)
  @LogAccountIdentifier
  @Operation(operationId = "createPerspectiveFolder",
      description = "Create a Perspective Folder.",
      summary = "Create a Perspective folder",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Returns a created CEViewFolder object with all its details",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<CEViewFolder>
  create(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @RequestBody(
          required = true, description = "Request body containing Perspective's CEViewFolder object") @Valid CEViewFolder ceViewFolder) {
    ceViewFolder.setAccountId(accountId);
    ceViewFolder.setPinned(false);
    ceViewFolder.setViewType(ViewType.CUSTOMER);
    return ResponseDTO.newResponse(ceViewFolderService.save(ceViewFolder));
  }

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get folders for account", nickname = "getFolders")
  @LogAccountIdentifier
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(operationId = "getFolders",
      description = "Fetch folders given an accountId",
      summary = "Fetch folders for an account",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            description =
                "Returns List of CEViewFolders",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<List<CEViewFolder>>
  getFolders(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
      NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
             @RequestBody(required = true, description = "Request body containing accountId and folderIds") @Valid ViewFolderQueryDTO query) {
    long totalFolders;
    List<CEViewFolder> ceViewFolders;
    if (Collections.isEmpty(query.getFolderIds())) {
      totalFolders = ceViewFolderService.numberOfFolders(accountId);
      ceViewFolders = ceViewFolderService.getFolders(accountId, query.getPageNo());
    } else {
      totalFolders = ceViewFolderService.numberOfFolders(accountId, query.getFolderIds());
      ceViewFolders = ceViewFolderService.getFolders(accountId, query.getFolderIds(), query.getPageNo());
    }
    Map<String, Long> metadata = new HashMap<>();
    metadata.put("totalFolders", totalFolders);
    metadata.put("pageNo", query.getPageNo());
    ResponseDTO<List<CEViewFolder>> response = ResponseDTO.newResponse(ceViewFolders);
    response.setMetaData(metadata);
    return response;
  }

  @GET
  @Path("{folderId}/perspectives")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get All perspectives in a folder", nickname = "getAllPerspectives")
  @LogAccountIdentifier
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(operationId = "getAllPerspectives",
      description = "Return details of all the Perspectives for the given account ID and folder",
      summary = "Return details of all the Perspectives",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "Returns a List of Perspectives",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<List<QLCEView>>
  getPerspectives(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
      NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
                  @Parameter(required = true, description = "Unique identifier for folder") @PathParam("folderId") String folderId) {
    return ResponseDTO.newResponse(ceViewService.getAllViews(accountId, folderId, true));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update a folder", nickname = "updateFolder")
  @LogAccountIdentifier
  @Operation(operationId = "updateFolder",
      description =
          "Update a folder",
      summary = "Update a folder",
      responses =
          {
              @io.swagger.v3.oas.annotations.responses.
                  ApiResponse(description = "CEViewFolder object",
                  content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
          })
  public ResponseDTO<CEViewFolder>
  rename(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
      NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
         @RequestBody(required = true, description = "Request body containing ceViewFolder object") @Valid CEViewFolder ceViewFolder) {
    return ResponseDTO.newResponse(ceViewFolderService.updateFolder(accountId, ceViewFolder));
  }

  @POST
  @Path("movePerspectives/{newFolderId}")
  @Hidden
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Move perspectives", nickname = "movePerspectives")
  @FeatureRestrictionCheck(FeatureRestrictionName.PERSPECTIVES)
  @LogAccountIdentifier
  @Operation(operationId = "movePerspectives", description = "Move a perspective from a folder to another.",
      summary = "Move a Perspective",
      responses =
          {
              @io.swagger.v3.oas.annotations.responses.
                  ApiResponse(description = "Returns the new CEView object",
                  content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
          })
  public ResponseDTO<List<CEView>>
  movePerspectives(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
      NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
         @Parameter(required = true, description = "Unique identifier for the Perspective folder") @PathParam(
             "newFolderId") String newFolderId,
         @RequestBody(required = true, description = "Request body containing perspectiveIds to be moved") @Valid List<String> perspectiveIds) {
    return ResponseDTO.newResponse(ceViewFolderService.moveMultipleCEViews(accountId, perspectiveIds, newFolderId));
  }

  @DELETE
  @Path("{folderId}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Delete folder", nickname = "deleteFolder")
  @LogAccountIdentifier
  @Operation(operationId = "deleteFolder", description = "Delete a Folder for the given Folder ID.",
      summary = "Delete a folder",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "A boolean whether the delete was successful or not",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<Boolean>
  delete(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @PathParam("folderId") @Parameter(required = true,
          description = "Unique identifier for the Perspective folder") @NotNull @Valid String folderId) {
    return ResponseDTO.newResponse(ceViewFolderService.delete(accountId, folderId));
  }
}
