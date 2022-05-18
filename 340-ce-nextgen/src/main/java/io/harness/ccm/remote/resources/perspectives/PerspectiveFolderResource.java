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
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.CEViewFolder;
import io.harness.ccm.views.service.CEViewFolderService;
import io.harness.enforcement.client.annotation.FeatureRestrictionCheck;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;
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
import java.util.List;

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

  @Inject
  public PerspectiveFolderResource(CEViewFolderService ceViewFolderService) {
    this.ceViewFolderService = ceViewFolderService;
  }

  @POST
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
  public ResponseDTO<Boolean>
  create(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @RequestBody(
          required = true, description = "Request body containing Perspective's CEViewFolder object") @Valid CEViewFolder ceViewFolder) {
    ceViewFolder.setAccountId(accountId);
    ceViewFolder.setPinned(false);
    return ResponseDTO.newResponse(ceViewFolderService.save(ceViewFolder));
  }

  @GET
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
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId) {
    return ResponseDTO.newResponse(ceViewFolderService.getFoldersForAccount(accountId));
  }

  @GET
  @Path("{folderId}")
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
  public ResponseDTO<List<CEView>>
  getPerspectives(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
      NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
                  @Parameter(required = true, description = "Unique identifier for folder") @PathParam("folderId") String folderId) {
    return ResponseDTO.newResponse(ceViewFolderService.getPerspectivesForFolder(accountId, folderId));
  }

  @PUT
  @Path("{folderId}/renameFolder")
  @Timed
  @ExceptionMetered
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Rename folder", nickname = "renameFolder")
  @LogAccountIdentifier
  @Operation(operationId = "renameFolder",
      description =
          "Rename a folder",
      summary = "Rename a folder",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "CEViewFolder object",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<CEViewFolder>
  rename(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
         @Parameter(required = true, description = "Unique identifier for folder") @PathParam("folderId") String folderId,
         @QueryParam("newName") @Parameter(required = true, description = "new name for the folder") @NotNull @Valid String newName) {
    return ResponseDTO.newResponse(ceViewFolderService.updateFolderName(accountId, folderId, newName));
  }

  @PUT
  @Path("{folderId}/pinFolder")
  @Timed
  @ExceptionMetered
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Pin/Unpin a folder", nickname = "pinFolder")
  @LogAccountIdentifier
  @Operation(operationId = "pinFolder",
      description =
          "Pin/Unpin a folder",
      summary = "Pin/Unpin a folder",
      responses =
          {
              @io.swagger.v3.oas.annotations.responses.
                  ApiResponse(description = "CEViewFolder object",
                  content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
          })
  public ResponseDTO<CEViewFolder>
  pinFolder(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
      NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
         @Parameter(required = true, description = "Unique identifier for folder") @PathParam("folderId") String folderId,
         @QueryParam("pinStatus") @Parameter(required = true, description = "new pin status for the folder") @NotNull @Valid boolean pinStatus) {
    return ResponseDTO.newResponse(ceViewFolderService.pinFolder(accountId, folderId, pinStatus));
  }

  @POST
  @Path("movePerspective/{newFolderId}/{perspectiveId}")
  @Hidden
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Move perspective", nickname = "movePerspective")
  @FeatureRestrictionCheck(FeatureRestrictionName.PERSPECTIVES)
  @LogAccountIdentifier
  @Operation(operationId = "movePerspective", description = "Move a perspective from a folder to another.",
      summary = "Move a Perspective",
      responses =
          {
              @io.swagger.v3.oas.annotations.responses.
                  ApiResponse(description = "Returns the new CEView object",
                  content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
          })
  public ResponseDTO<CEView>
  movePerspective(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
      NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
         @Parameter(required = true, description = "Unique identifier for the Perspective folder") @PathParam(
             "newFolderId") String newFolderId,
         @Parameter(required = true, description = "Unique identifier for the Perspective") @PathParam(
             "perspectiveId") String perspectiveId) {
    return ResponseDTO.newResponse(ceViewFolderService.moveCEView(accountId, perspectiveId, newFolderId));
  }

  @POST
  @Path("movePerspectives/{newFolderId}")
  @Hidden
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Move perspective", nickname = "movePerspective")
  @FeatureRestrictionCheck(FeatureRestrictionName.PERSPECTIVES)
  @LogAccountIdentifier
  @Operation(operationId = "movePerspective", description = "Move a perspective from a folder to another.",
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
      @QueryParam("folderId") @Parameter(required = true,
          description = "Unique identifier for the Perspective folder") @NotNull @Valid String folderId) {
    return ResponseDTO.newResponse(ceViewFolderService.delete(folderId, accountId));
  }
}
