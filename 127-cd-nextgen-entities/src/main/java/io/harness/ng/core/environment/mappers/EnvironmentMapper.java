/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.environment.mappers;

import static io.harness.NGConstants.HARNESS_BLUE;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ng.core.mapper.TagMapper.convertToMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.EnvironmentBasicInfo;
import io.harness.ng.core.environment.dto.EnvironmentRequestDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponse;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.environment.yaml.NGEnvironmentConfig;
import io.harness.ng.core.environment.yaml.NGEnvironmentInfoConfig;
import io.harness.pms.yaml.YamlUtils;
import io.harness.utils.YamlPipelineUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class EnvironmentMapper {
  public Environment toEnvironmentEntity(String accountId, EnvironmentRequestDTO environmentRequestDTO) {
    final Environment environment;
    if (isNotEmpty(environmentRequestDTO.getYaml())) {
      NGEnvironmentConfig ngEnvironmentConfig = toNGEnvironmentConfig(environmentRequestDTO);
      environment = toNGEnvironmentEntity(accountId, ngEnvironmentConfig, environmentRequestDTO.getColor());
      environment.setYaml(environmentRequestDTO.getYaml());
      return environment;
    }
    environment = toNGEnvironmentEntity(accountId, environmentRequestDTO);
    NGEnvironmentConfig ngEnvironmentConfig = toNGEnvironmentConfig(environment);
    environment.setYaml(toYaml(ngEnvironmentConfig));
    return environment;
  }

  public Environment toNGEnvironmentEntity(String accountId, EnvironmentRequestDTO dto) {
    return Environment.builder()
        .identifier(dto.getIdentifier())
        .accountId(accountId)
        .orgIdentifier(dto.getOrgIdentifier())
        .projectIdentifier(dto.getProjectIdentifier())
        .name(dto.getName())
        .color(Optional.ofNullable(dto.getColor()).orElse(HARNESS_BLUE))
        .description(dto.getDescription())
        .type(dto.getType())
        .tags(convertToList(dto.getTags()))
        .build();
  }

  public Environment toNGEnvironmentEntity(String accountId, NGEnvironmentConfig envConfig, String color) {
    NGEnvironmentInfoConfig ngEnvironmentInfoConfig = envConfig.getNgEnvironmentInfoConfig();
    return Environment.builder()
        .identifier(ngEnvironmentInfoConfig.getIdentifier())
        .accountId(accountId)
        .orgIdentifier(ngEnvironmentInfoConfig.getOrgIdentifier())
        .projectIdentifier(ngEnvironmentInfoConfig.getProjectIdentifier())
        .name(ngEnvironmentInfoConfig.getName())
        .color(Optional.ofNullable(color).orElse(HARNESS_BLUE))
        .description(ngEnvironmentInfoConfig.getDescription())
        .type(ngEnvironmentInfoConfig.getType())
        .tags(convertToList(ngEnvironmentInfoConfig.getTags()))
        .build();
  }

  public EnvironmentResponseDTO writeDTO(Environment environment) {
    return EnvironmentResponseDTO.builder()
        .accountId(environment.getAccountId())
        .orgIdentifier(environment.getOrgIdentifier())
        .projectIdentifier(environment.getProjectIdentifier())
        .identifier(environment.getIdentifier())
        .name(environment.getName())
        .color(Optional.ofNullable(environment.getColor()).orElse(HARNESS_BLUE))
        .description(environment.getDescription())
        .type(environment.getType())
        .deleted(environment.getDeleted())
        .tags(convertToMap(environment.getTags()))
        .version(environment.getVersion())
        .yaml(environment.getYaml())
        .build();
  }

  public EnvironmentResponse toResponseWrapper(Environment environment) {
    return EnvironmentResponse.builder()
        .environment(writeDTO(environment))
        .createdAt(environment.getCreatedAt())
        .lastModifiedAt(environment.getLastModifiedAt())
        .build();
  }

  public EnvironmentBasicInfo toBasicInfo(Environment environment) {
    return EnvironmentBasicInfo.builder()
        .identifier(environment.getIdentifier())
        .name(environment.getName())
        .description(environment.getDescription())
        .type(environment.getType())
        .accountIdentifier(environment.getAccountId())
        .orgIdentifier(environment.getOrgIdentifier())
        .projectIdentifier(environment.getProjectIdentifier())
        .tags(convertToMap(environment.getTags()))
        .color(environment.getColor())
        .build();
  }

  public static List<EnvironmentResponse> toResponseWrapper(List<Environment> envList) {
    return envList.stream().map(env -> toResponseWrapper(env)).collect(Collectors.toList());
  }

  public static NGEnvironmentConfig toNGEnvironmentConfig(Environment environmentEntity) {
    if (isNotEmpty(environmentEntity.getYaml())) {
      try {
        return YamlUtils.read(environmentEntity.getYaml(), NGEnvironmentConfig.class);
      } catch (IOException e) {
        throw new InvalidRequestException("Cannot create environment config due to " + e.getMessage());
      }
    }
    return NGEnvironmentConfig.builder()
        .ngEnvironmentInfoConfig(NGEnvironmentInfoConfig.builder()
                                     .name(environmentEntity.getName())
                                     .identifier(environmentEntity.getIdentifier())
                                     .orgIdentifier(environmentEntity.getOrgIdentifier())
                                     .projectIdentifier(environmentEntity.getProjectIdentifier())
                                     .description(environmentEntity.getDescription())
                                     .tags(convertToMap(environmentEntity.getTags()))
                                     .type(environmentEntity.getType())
                                     .build())
        .build();
  }

  public static NGEnvironmentConfig toNGEnvironmentConfig(EnvironmentRequestDTO dto) {
    if (isNotEmpty(dto.getYaml())) {
      try {
        return YamlUtils.read(dto.getYaml(), NGEnvironmentConfig.class);
      } catch (IOException e) {
        throw new InvalidRequestException("Cannot create environment config due to " + e.getMessage());
      }
    }
    return NGEnvironmentConfig.builder()
        .ngEnvironmentInfoConfig(NGEnvironmentInfoConfig.builder()
                                     .name(dto.getName())
                                     .identifier(dto.getIdentifier())
                                     .orgIdentifier(dto.getOrgIdentifier())
                                     .projectIdentifier(dto.getProjectIdentifier())
                                     .description(dto.getDescription())
                                     .tags(dto.getTags())
                                     .type(dto.getType())
                                     .build())
        .build();
  }

  public static String toYaml(@Valid NGEnvironmentConfig ngEnvironmentConfig) {
    try {
      return YamlPipelineUtils.getYamlString(ngEnvironmentConfig);
    } catch (IOException e) {
      throw new InvalidRequestException("Cannot create environment entity due to " + e.getMessage());
    }
  }
}
