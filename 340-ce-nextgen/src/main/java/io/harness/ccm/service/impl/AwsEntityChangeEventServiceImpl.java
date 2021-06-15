package io.harness.ccm.service.impl;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;

import static java.lang.String.format;

import io.harness.ccm.CENextGenConfiguration;
import io.harness.ccm.commons.beans.config.AwsConfig;
import io.harness.ccm.commons.entities.billing.CECloudAccount;
import io.harness.ccm.service.intf.AWSOrganizationHelperService;
import io.harness.ccm.service.intf.AwsEntityChangeEventService;
import io.harness.ccm.setup.CECloudAccountDao;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.remote.client.NGRestUtils;

import com.amazonaws.services.eks.model.NotFoundException;
import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AwsEntityChangeEventServiceImpl implements AwsEntityChangeEventService {
  @Inject ConnectorResourceClient connectorResourceClient;
  @Inject AWSOrganizationHelperService awsOrganizationHelperService;
  @Inject CENextGenConfiguration configuration;
  @Inject CECloudAccountDao cloudAccountDao;

  @Override
  public boolean processAWSEntityChangeEvent(EntityChangeDTO entityChangeDTO, String action) {
    String identifier = entityChangeDTO.getIdentifier().getValue();
    String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();
    AwsConfig awsConfig = configuration.getAwsConfig();

    switch (action) {
      case CREATE_ACTION:
        CEAwsConnectorDTO ceAwsConnectorDTO =
            (CEAwsConnectorDTO) getConnectorConfigDTO(accountIdentifier, identifier).getConnectorConfig();
        List<CECloudAccount> awsAccounts = awsOrganizationHelperService.getAWSAccounts(
            accountIdentifier, identifier, ceAwsConnectorDTO, awsConfig.getAwsAccessKey(), awsConfig.getAwsSecretKey());
        for (CECloudAccount account : awsAccounts) {
          cloudAccountDao.create(account);
        }
        break;
      default:
        log.info("Not processing AWS Event, action: {}, entityChangeDTO: {}", action, entityChangeDTO);
    }
    return false;
  }

  public ConnectorInfoDTO getConnectorConfigDTO(String accountIdentifier, String connectorIdentifierRef) {
    try {
      Optional<ConnectorDTO> connectorDTO =
          NGRestUtils.getResponse(connectorResourceClient.get(connectorIdentifierRef, accountIdentifier, null, null));

      if (!connectorDTO.isPresent()) {
        throw new InvalidRequestException(format("Connector not found for identifier : [%s]", connectorIdentifierRef));
      }

      return connectorDTO.get().getConnectorInfo();
    } catch (Exception e) {
      throw new NotFoundException(format("Error while getting connector information : [%s]", connectorIdentifierRef));
    }
  }
}
