package io.harness.connector.impl;

import static io.harness.connector.entities.ConnectivityStatus.FAILURE;
import static io.harness.connector.entities.ConnectivityStatus.SUCCESS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.connector.ConnectorFilterHelper;
import io.harness.connector.ConnectorScopeHelper;
import io.harness.connector.FullyQualitifedIdentifierHelper;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorFilter;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.ConnectorConnectivityDetails;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.connector.mappers.ConnectorSummaryMapper;
import io.harness.connector.repositories.base.ConnectorRepository;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.connector.validator.KubernetesConnectionValidator;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ConnectorServiceImpl implements ConnectorService {
  private final ConnectorMapper connectorMapper;
  private final ConnectorRepository connectorRepository;
  private final ConnectorFilterHelper connectorFilterHelper;
  private final ConnectorScopeHelper connectorScopeHelper;
  private final ConnectorSummaryMapper connectorSummaryMapper;
  @Inject private Map<String, ConnectionValidator> connectionValidatorMap;
  private final KubernetesConnectionValidator kubernetesConnectionValidator;

  @Override
  public Optional<ConnectorDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    Optional<Connector> connector = connectorRepository.findByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    if (connector.isPresent()) {
      return Optional.of(connectorMapper.writeDTO(connector.get()));
    }
    throw new InvalidRequestException(
        createConnectorNotFoundMessage(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  private String createConnectorNotFoundMessage(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    StringBuilder stringBuilder = new StringBuilder(256);
    stringBuilder.append("No connector exists with the identifier ")
        .append(connectorIdentifier)
        .append(" in account ")
        .append(accountIdentifier);
    if (isNotBlank(orgIdentifier)) {
      stringBuilder.append(", organisation ").append(orgIdentifier);
    }
    if (isNotBlank(projectIdentifier)) {
      stringBuilder.append(", project ").append(projectIdentifier);
    }
    return stringBuilder.toString();
  }

  @Override
  public Page<ConnectorSummaryDTO> list(ConnectorFilter connectorFilter, int page, int size, String accountIdentifier) {
    Criteria criteria = connectorFilterHelper.createCriteriaFromConnectorFilter(connectorFilter, accountIdentifier);
    Pageable pageable = getPageRequest(page, size);
    Page<Connector> connectors = connectorRepository.findAll(criteria, pageable);
    return connectorScopeHelper.createConnectorSummaryListForConnectors(connectors);
  }

  private Pageable getPageRequest(int page, int size) {
    return PageRequest.of(page, size);
  }

  @Override
  public ConnectorDTO create(ConnectorRequestDTO connectorRequestDTO, String accountIdentifier) {
    Connector connectorEntity = connectorMapper.toConnector(connectorRequestDTO, accountIdentifier);
    Connector savedConnectorEntity = null;
    try {
      savedConnectorEntity = connectorRepository.save(connectorEntity);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("Connector [%s] already exists", connectorEntity.getIdentifier()));
    }
    return connectorMapper.writeDTO(savedConnectorEntity);
  }

  @Override
  public ConnectorDTO update(ConnectorRequestDTO connectorRequestDTO, String accountIdentifier) {
    Objects.requireNonNull(connectorRequestDTO.getIdentifier());
    String fullyQualifiedIdentifier = FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(accountIdentifier,
        connectorRequestDTO.getOrgIdentifier(), connectorRequestDTO.getProjectIdentifier(),
        connectorRequestDTO.getIdentifier());
    Optional<Connector> existingConnector =
        connectorRepository.findByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    if (!existingConnector.isPresent()) {
      throw new InvalidRequestException(
          String.format("No connector exists with the  Identitier %s", connectorRequestDTO.getIdentifier()));
    }
    Connector newConnector = connectorMapper.toConnector(connectorRequestDTO, accountIdentifier);
    newConnector.setId(existingConnector.get().getId());
    newConnector.setVersion(existingConnector.get().getVersion());
    Connector updatedConnector = connectorRepository.save(newConnector);
    return connectorMapper.writeDTO(updatedConnector);
  }

  @Override
  public boolean delete(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    Long connectorsDeleted = connectorRepository.deleteByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    return connectorsDeleted == 1;
  }

  public ConnectorValidationResult validate(ConnectorRequestDTO connectorDTO, String accountIdentifier) {
    ConnectionValidator connectionValidator = connectionValidatorMap.get(connectorDTO.getConnectorType().toString());
    return connectionValidator.validate(connectorDTO.getConnectorConfig(), accountIdentifier,
        connectorDTO.getOrgIdentifier(), connectorDTO.getProjectIdentifier());
  }

  public boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    return !connectorRepository.existsByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
  }

  public ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    Optional<Connector> connectorOptional =
        connectorRepository.findByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    if (connectorOptional.isPresent()) {
      ConnectorDTO connectorDTO = connectorMapper.writeDTO(connectorOptional.get());
      ConnectionValidator connectionValidator = connectionValidatorMap.get(connectorDTO.getConnectorType().toString());
      ConnectorValidationResult validationResult = connectionValidator.validate(
          connectorDTO.getConnectorConfig(), accountIdentifier, orgIdentifier, projectIdentifier);
      updateConnectivityStatusOfConnector(connectorOptional.get(), validationResult);
      return validationResult;
    } else {
      throw new InvalidRequestException(
          createConnectorNotFoundMessage(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
    }
  }

  private void updateConnectivityStatusOfConnector(
      Connector connector, ConnectorValidationResult connectorValidationResult) {
    if (connectorValidationResult != null) {
      connector.setStatus(ConnectorConnectivityDetails.builder()
                              .status(connectorValidationResult.isValid() ? SUCCESS : FAILURE)
                              .errorMessage(connectorValidationResult.getErrorMessage())
                              .build());
      connectorRepository.save(connector);
    }
  }
}
