/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.FileBucket.FILE_STORE;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DuplicateEntityException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.file.beans.NGBaseFile;
import io.harness.ng.core.api.FileStoreService;
import io.harness.ng.core.dto.filestore.FileDTO;
import io.harness.ng.core.dto.filestore.node.FileStoreNodeDTO;
import io.harness.ng.core.dto.filestore.node.FolderNodeDTO;
import io.harness.ng.core.entities.NGFile;
import io.harness.ng.core.mapper.FileDTOMapper;
import io.harness.ng.core.mapper.FileStoreNodeDTOMapper;
import io.harness.repositories.filestore.spring.FileStoreRepository;
import io.harness.stream.BoundedInputStream;

import software.wings.app.MainConfiguration;
import software.wings.service.intfc.FileService;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

@Singleton
@OwnedBy(CDP)
@Slf4j
public class FileStoreServiceImpl implements FileStoreService {
  private final FileService fileService;
  private final FileStoreRepository fileStoreRepository;
  private final MainConfiguration configuration;

  @Inject
  public FileStoreServiceImpl(
      FileService fileService, FileStoreRepository fileStoreRepository, MainConfiguration configuration) {
    this.fileService = fileService;
    this.fileStoreRepository = fileStoreRepository;
    this.configuration = configuration;
  }

  @Override
  public FileDTO create(@NotNull FileDTO fileDto, InputStream content) {
    log.info("Creating {}: {}", fileDto.getType().name().toLowerCase(), fileDto);

    if (existInDatabase(fileDto)) {
      throw new DuplicateEntityException(getDuplicateEntityMessage(fileDto));
    }

    NGFile ngFile = FileDTOMapper.getNGFileFromDTO(fileDto);

    if (fileDto.isFile()) {
      if (content == null) {
        throw new InvalidArgumentsException(format("File content is empty. Identifier: %s", fileDto.getIdentifier()));
      }
      saveFile(fileDto, ngFile, content);
    }

    try {
      ngFile = fileStoreRepository.save(ngFile);
      return FileDTOMapper.getFileDTOFromNGFile(ngFile);
    } catch (DuplicateKeyException e) {
      throw new DuplicateEntityException(getDuplicateEntityMessage(fileDto));
    }
  }

  @Override
  public File downloadFile(@NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier,
      @NotNull String fileIdentifier) {
    if (isEmpty(fileIdentifier)) {
      throw new InvalidArgumentsException("File identifier cannot be empty");
    }

    Optional<NGFile> ngFileOpt =
        fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, fileIdentifier);
    if (!ngFileOpt.isPresent()) {
      throw new InvalidArgumentsException(format("Unable to find file, fileIdentifier: %s", fileIdentifier));
    }
    NGFile ngFile = ngFileOpt.get();
    if (ngFile.isFolder()) {
      throw new InvalidArgumentsException(
          format("Downloading folder not supported, fileIdentifier: %s", fileIdentifier));
    }

    File file = new File(Files.createTempDir(), ngFile.getFileName());
    log.info("Start downloading file, fileIdentifier: {}, filePath: {}", fileIdentifier, file.getPath());
    return fileService.download(ngFile.getFileUuid(), file, FILE_STORE);
  }

  @Override
  public FileDTO update(@NotNull FileDTO fileDto, InputStream content, String identifier) {
    NGFile existingFile =
        fileStoreRepository
            .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
                fileDto.getAccountIdentifier(), fileDto.getOrgIdentifier(), fileDto.getProjectIdentifier(), identifier)
            .orElseThrow(() -> new IllegalArgumentException(format("File with identifier: %s not found.", identifier)));

    FileDTOMapper.updateNGFile(fileDto, existingFile);
    if (content != null && fileDto.isFile()) {
      log.info("Start updating file in file system, identifier: {}", identifier);
      saveFile(fileDto, existingFile, content);
    }
    fileStoreRepository.save(existingFile);
    return FileDTOMapper.getFileDTOFromNGFile(existingFile);
  }

  @Override
  public boolean delete(
      @NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier, @NotNull String identifier) {
    // delete entities in configs.files and configs.files by using fileService
    // delete entity in DB by using fileStoreRepository or fileStoreRepositoryCustom
    return false;
  }

  @Override
  public FolderNodeDTO listFolderNodes(@NotNull String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotNull FolderNodeDTO folderNodeDTO) {
    return populateFolderNode(folderNodeDTO, accountIdentifier, orgIdentifier, projectIdentifier);
  }

  private boolean existInDatabase(FileDTO fileDto) {
    return fileStoreRepository
        .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(fileDto.getAccountIdentifier(),
            fileDto.getOrgIdentifier(), fileDto.getProjectIdentifier(), fileDto.getIdentifier())
        .isPresent();
  }

  private String getDuplicateEntityMessage(@NotNull FileDTO fileDto) {
    return String.format("Try creating another %s, %s with identifier [%s] already exists in the parent folder",
        fileDto.getType().name().toLowerCase(), fileDto.getType().name().toLowerCase(), fileDto.getIdentifier());
  }

  // in the case when we need to return the whole folder structure, create recursion on this method
  private FolderNodeDTO populateFolderNode(
      FolderNodeDTO folderNode, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<FileStoreNodeDTO> fileStoreNodes =
        listFolderChildren(accountIdentifier, orgIdentifier, projectIdentifier, folderNode.getFolderIdentifier());
    for (FileStoreNodeDTO node : fileStoreNodes) {
      folderNode.addChild(node);
    }
    return folderNode;
  }

  private List<FileStoreNodeDTO> listFolderChildren(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String folderIdentifier) {
    return listFilesByParentIdentifier(accountIdentifier, orgIdentifier, projectIdentifier, folderIdentifier)
        .stream()
        .filter(Objects::nonNull)
        .map(ngFile
            -> ngFile.isFolder() ? FileStoreNodeDTOMapper.getFolderNodeDTO(ngFile)
                                 : FileStoreNodeDTOMapper.getFileNodeDTO(ngFile))
        .collect(Collectors.toList());
  }

  private List<NGFile> listFilesByParentIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String parentIdentifier) {
    return fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, parentIdentifier);
  }

  private void saveFile(FileDTO fileDto, NGFile ngFile, @NotNull InputStream content) {
    BoundedInputStream fileContent =
        new BoundedInputStream(content, configuration.getFileUploadLimits().getFileStoreFileLimit());
    fileService.saveFile(getNgBaseFile(fileDto), fileContent, FILE_STORE);
    ngFile.setSize(fileContent.getTotalBytesRead());
  }

  private NGBaseFile getNgBaseFile(FileDTO fileDto) {
    NGBaseFile baseFile = new NGBaseFile();
    baseFile.setFileName(fileDto.getName());
    baseFile.setMimeType(fileDto.getMimeType());
    baseFile.setAccountId(fileDto.getAccountIdentifier());
    return baseFile;
  }
}
