package io.harness.ngtriggers.eventmapper.filters.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ngtriggers.Constants.DOT_GIT;
import static io.harness.ngtriggers.beans.response.TriggerEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_REPO;
import static io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo.AWS_CODECOMMIT;
import static io.harness.utils.IdentifierRefHelper.getFullyQualifiedIdentifierRefString;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Repository;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnector;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitUrlType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse.WebhookEventMappingResponseBuilder;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.metadata.WebhookMetadata;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.eventmapper.TriggerGitConnectorWrapper;
import io.harness.ngtriggers.eventmapper.filters.TriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.helpers.TriggerEventResponseHelper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.utils.GitProviderDataObtainmentManager;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(PIPELINE)
public class GitWebhookTriggerRepoFilter implements TriggerFilter {
  // TODO: This should come from scm parsing service
  private static final String AWS_CODECOMMIT_URL_PATTERN = "https://git-codecommit.%s.amazonaws.com/v1/repos/%s";
  private final NGTriggerService ngTriggerService;
  private final GitProviderDataObtainmentManager additionalDataObtainmentManager;

  @Override
  public WebhookEventMappingResponse applyFilter(FilterRequestData filterRequestData) {
    WebhookEventMappingResponseBuilder mappingResponseBuilder = initWebhookEventMappingResponse(filterRequestData);

    WebhookPayloadData webhookPayloadData = filterRequestData.getWebhookPayloadData();
    TriggerWebhookEvent originalEvent = webhookPayloadData.getOriginalEvent();
    Repository repository = webhookPayloadData.getRepository();
    Set<String> urls = getUrls(repository, originalEvent.getSourceRepoType());

    // {connectorFQN, connectorConfig, List<Trigger>}
    List<TriggerGitConnectorWrapper> triggerGitConnectorWrappers =
        prepareTriggerConnectorWrapperList(originalEvent.getAccountId(), filterRequestData.getDetails());

    List<TriggerDetails> eligibleTriggers = new ArrayList<>();
    for (TriggerGitConnectorWrapper wrapper : triggerGitConnectorWrappers) {
      // update GitConnectionType and repoUrl values in wrapper.
      updateConnectionTypeAndUrlInWrapper(wrapper);

      if (wrapper.getGitConnectionType() == GitConnectionType.REPO) {
        evaluateWrapperForRepoLevelGitConnector(urls, eligibleTriggers, wrapper);
      } else {
        evaluateWrapperForAccountLevelGitConnector(urls, eligibleTriggers, wrapper);
      }
    }

    if (isEmpty(eligibleTriggers)) {
      String msg = format("No trigger found for repoUrl: %s for Account %s",
          webhookPayloadData.getRepository().getLink(), filterRequestData.getAccountId());
      log.info(msg);
      mappingResponseBuilder.failedToFindTrigger(true)
          .webhookEventResponse(
              TriggerEventResponseHelper.toResponse(NO_MATCHING_TRIGGER_FOR_REPO, originalEvent, null, null, msg, null))
          .build();
    } else {
      // fetches additional information
      additionalDataObtainmentManager.acquireProviderData(filterRequestData, eligibleTriggers);
      addDetails(mappingResponseBuilder, filterRequestData, eligibleTriggers);
    }

    return mappingResponseBuilder.build();
  }

  @VisibleForTesting
  HashSet<String> getUrls(Repository repository, String sourceRepoType) {
    if (AWS_CODECOMMIT.name().equals(sourceRepoType)) {
      String[] arnTokens = repository.getId().split(":");
      String awsRepoUrl = format(AWS_CODECOMMIT_URL_PATTERN, arnTokens[3], arnTokens[5]);
      return new HashSet<>(Collections.singletonList(awsRepoUrl));
    }

    HashSet<String> urls = new HashSet<>();

    String httpUrl = repository.getHttpURL().toLowerCase();
    urls.add(httpUrl);
    // Add url without .git, to handle case, where user entered url without .git on connector
    if (httpUrl.endsWith(DOT_GIT)) {
      urls.add(httpUrl.substring(0, httpUrl.length() - 4));
    }
    // Add url without .git, to handle case, where user entered url without .git on connector
    String sshUrl = repository.getSshURL().toLowerCase();
    if (sshUrl.endsWith(DOT_GIT)) {
      urls.add(sshUrl.substring(0, sshUrl.length() - 4));
    }
    urls.add(sshUrl);
    urls.add(repository.getLink().toLowerCase());

    return urls;
  }

  private void evaluateWrapperForAccountLevelGitConnector(
      Set<String> urls, List<TriggerDetails> eligibleTriggers, TriggerGitConnectorWrapper wrapper) {
    String accUrl = wrapper.getUrl();

    for (TriggerDetails details : wrapper.getTriggers()) {
      try {
        final String repoUrl =
            new StringBuilder(128)
                .append(accUrl)
                .append(accUrl.endsWith("/") ? EMPTY : '/')
                .append(details.getNgTriggerEntity().getMetadata().getWebhook().getGit().getRepoName())
                .toString();

        String finalUrl = urls.stream().filter(u -> u.equalsIgnoreCase(repoUrl)).findAny().orElse(null);

        if (!isBlank(finalUrl)) {
          eligibleTriggers.add(details);
        }
      } catch (Exception e) {
        log.error(getTriggerSkipMessage(details.getNgTriggerEntity()));
      }
    }
  }

  @VisibleForTesting
  void evaluateWrapperForRepoLevelGitConnector(
      Set<String> urls, List<TriggerDetails> eligibleTriggers, TriggerGitConnectorWrapper wrapper) {
    String url = wrapper.getUrl();
    // accomadate the '/' at the end of the provided repo URL
    final String modifiedUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

    String finalUrl = urls.stream().filter(u -> u.equalsIgnoreCase(modifiedUrl)).findAny().orElse(null);

    if (!isBlank(finalUrl)) {
      eligibleTriggers.addAll(wrapper.getTriggers());
    }
  }

  @VisibleForTesting
  void updateConnectionTypeAndUrlInWrapper(TriggerGitConnectorWrapper wrapper) {
    ConnectorConfigDTO connectorConfigDTO = wrapper.getConnectorConfigDTO();

    if (connectorConfigDTO.getClass().isAssignableFrom(GithubConnector.class)) {
      GithubConnector githubConnector = (GithubConnector) connectorConfigDTO;
      wrapper.setConnectorType(ConnectorType.GITHUB);
      wrapper.setUrl(githubConnector.getUrl());
      wrapper.setGitConnectionType(githubConnector.getConnectionType());
    } else if (connectorConfigDTO.getClass().isAssignableFrom(GitlabConnector.class)) {
      GitlabConnector gitlabConnector = (GitlabConnector) connectorConfigDTO;
      wrapper.setConnectorType(ConnectorType.GITLAB);
      wrapper.setUrl(gitlabConnector.getUrl());
      wrapper.setGitConnectionType(gitlabConnector.getConnectionType());
    } else if (connectorConfigDTO.getClass().isAssignableFrom(BitbucketConnector.class)) {
      BitbucketConnector bitbucketConnector = (BitbucketConnector) connectorConfigDTO;
      wrapper.setConnectorType(ConnectorType.BITBUCKET);
      wrapper.setUrl(bitbucketConnector.getUrl());
      wrapper.setGitConnectionType(bitbucketConnector.getConnectionType());
    } else if (connectorConfigDTO.getClass().isAssignableFrom(AwsCodeCommitConnector.class)) {
      AwsCodeCommitConnector awsCodeCommitConnector = (AwsCodeCommitConnector) connectorConfigDTO;
      wrapper.setConnectorType(ConnectorType.CODECOMMIT);
      wrapper.setUrl(awsCodeCommitConnector.getUrl());
      AwsCodeCommitUrlType urlType = awsCodeCommitConnector.getUrlType();
      if (urlType == AwsCodeCommitUrlType.REGION) {
        wrapper.setGitConnectionType(GitConnectionType.ACCOUNT);
      } else if (urlType == AwsCodeCommitUrlType.REPO) {
        wrapper.setGitConnectionType(GitConnectionType.REPO);
      }
    }
  }

  @VisibleForTesting
  List<TriggerGitConnectorWrapper> prepareTriggerConnectorWrapperList(
      String accountId, List<TriggerDetails> triggerDetails) {
    // Map 1
    Map<String, List<TriggerDetails>> triggerToConnectorMap = new HashMap<>();
    triggerDetails.forEach(
        triggerDetail -> generateConnectorFQNFromTriggerConfig(triggerDetail, triggerToConnectorMap));

    // Map 2
    Map<String, ConnectorConfigDTO> connectorMap = new HashMap<>();
    List<ConnectorResponseDTO> connectors =
        ngTriggerService.fetchConnectorsByFQN(accountId, new ArrayList<>(triggerToConnectorMap.keySet()));
    connectors.forEach(connector
        -> connectorMap.put(
            FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(accountId,
                connector.getConnector().getOrgIdentifier(), connector.getConnector().getProjectIdentifier(),
                connector.getConnector().getIdentifier()),
            connector.getConnector().getConnectorConfig()));

    return connectorMap.keySet()
        .stream()
        .map(fqn
            -> TriggerGitConnectorWrapper.builder()
                   .connectorFQN(fqn)
                   .connectorConfigDTO(connectorMap.get(fqn))
                   .triggers(triggerToConnectorMap.get(fqn))
                   .build())
        .collect(toList());
  }

  @VisibleForTesting
  void generateConnectorFQNFromTriggerConfig(
      TriggerDetails triggerDetail, Map<String, List<TriggerDetails>> triggerToConnectorMap) {
    NGTriggerEntity ngTriggerEntity = triggerDetail.getNgTriggerEntity();
    WebhookMetadata webhook = ngTriggerEntity.getMetadata().getWebhook();
    if (webhook == null || webhook.getGit() == null) {
      return;
    }

    String fullyQualifiedIdentifier = getFullyQualifiedIdentifierRefString(
        IdentifierRefHelper.getIdentifierRef(webhook.getGit().getConnectorIdentifier(), ngTriggerEntity.getAccountId(),
            ngTriggerEntity.getOrgIdentifier(), ngTriggerEntity.getProjectIdentifier()));

    List<TriggerDetails> triggerDetailList =
        triggerToConnectorMap.computeIfAbsent(fullyQualifiedIdentifier, k -> new ArrayList<>());

    triggerDetailList.add(triggerDetail);
  }
}
