package io.harness.ngtriggers.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.beans.source.WebhookTriggerType.AWS_CODECOMMIT;
import static io.harness.ngtriggers.beans.source.WebhookTriggerType.BITBUCKET;
import static io.harness.ngtriggers.beans.source.WebhookTriggerType.GITHUB;
import static io.harness.ngtriggers.beans.source.WebhookTriggerType.GITLAB;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.WebhookTriggerType;
import io.harness.ngtriggers.beans.source.webhook.WebhookAction;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;
import io.harness.ngtriggers.beans.source.webhook.v2.WebhookTriggerConfigV2;
import io.harness.ngtriggers.beans.source.webhook.v2.awscodecommit.AwsCodeCommitSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.awscodecommit.event.AwsCodeCommitEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.awscodecommit.event.AwsCodeCommitTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.BitbucketSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.action.BitbucketPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.event.BitbucketEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.event.BitbucketTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAction;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAware;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.git.PayloadAware;
import io.harness.ngtriggers.beans.source.webhook.v2.github.GithubSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.github.action.GithubIssueCommentAction;
import io.harness.ngtriggers.beans.source.webhook.v2.github.action.GithubPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.github.event.GithubEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.github.event.GithubTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.GitlabSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.action.GitlabPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.event.GitlabEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.event.GitlabTriggerEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class WebhookConfigHelper {
  public Map<WebhookSourceRepo, List<WebhookEvent>> getSourceRepoToEvent() {
    Map<WebhookSourceRepo, List<WebhookEvent>> map = new HashMap<>();
    map.put(WebhookSourceRepo.GITHUB, new ArrayList<>(WebhookEvent.githubEvents));
    map.put(WebhookSourceRepo.GITLAB, new ArrayList<>(WebhookEvent.gitlabEvents));
    map.put(WebhookSourceRepo.BITBUCKET, new ArrayList<>(WebhookEvent.bitbucketEvents));
    map.put(WebhookSourceRepo.AWS_CODECOMMIT, new ArrayList<>(WebhookEvent.awsCodeCommitEvents));

    return map;
  }

  public List<GithubTriggerEvent> getGithubTriggerEvents() {
    return Arrays.asList(GithubTriggerEvent.PUSH, GithubTriggerEvent.PULL_REQUEST, GithubTriggerEvent.ISSUE_COMMENT);
  }

  public List<GitlabTriggerEvent> getGitlabTriggerEvents() {
    return Arrays.asList(GitlabTriggerEvent.PUSH, GitlabTriggerEvent.MERGE_REQUEST);
  }

  public List<BitbucketTriggerEvent> getBitbucketTriggerEvents() {
    return Arrays.asList(BitbucketTriggerEvent.PUSH, BitbucketTriggerEvent.PULL_REQUEST);
  }

  public List<WebhookAction> getActionsList(WebhookSourceRepo sourceRepo, WebhookEvent event) {
    if (sourceRepo == WebhookSourceRepo.GITHUB) {
      return new ArrayList<>(WebhookAction.getGithubActionForEvent(event));
    } else if (sourceRepo == WebhookSourceRepo.BITBUCKET) {
      return new ArrayList<>(WebhookAction.getBitbucketActionForEvent(event));
    } else if (sourceRepo == WebhookSourceRepo.GITLAB) {
      return new ArrayList<>(WebhookAction.getGitLabActionForEvent(event));
    } else if (sourceRepo == WebhookSourceRepo.AWS_CODECOMMIT) {
      return new ArrayList<>(WebhookAction.getAwsCodeCommitActionForEvent(event));
    } else {
      return emptyList();
    }
  }

  public GitAware retrieveGitAware(WebhookTriggerConfigV2 webhookTriggerConfig) {
    if (!isGitSpec(webhookTriggerConfig)) {
      return null;
    }

    GitAware gitAware = null;
    if (webhookTriggerConfig.getType() == GITHUB) {
      GithubSpec githubSpec = (GithubSpec) webhookTriggerConfig.getSpec();
      GithubEventSpec gitEventSpec = githubSpec.getSpec();
      if (GitAware.class.isAssignableFrom(gitEventSpec.getClass())) {
        gitAware = (GitAware) gitEventSpec;
      }
    } else if (webhookTriggerConfig.getType() == GITLAB) {
      GitlabSpec gitlabSpec = (GitlabSpec) webhookTriggerConfig.getSpec();
      GitlabEventSpec gitlabEventSpec = gitlabSpec.getSpec();
      if (GitAware.class.isAssignableFrom(gitlabEventSpec.getClass())) {
        gitAware = (GitAware) gitlabEventSpec;
      }
    } else if (webhookTriggerConfig.getType() == BITBUCKET) {
      BitbucketSpec bitbucketSpec = (BitbucketSpec) webhookTriggerConfig.getSpec();
      BitbucketEventSpec bitbucketEventSpec = bitbucketSpec.getSpec();
      if (GitAware.class.isAssignableFrom(bitbucketEventSpec.getClass())) {
        gitAware = (GitAware) bitbucketEventSpec;
      }
    } else if (webhookTriggerConfig.getType() == AWS_CODECOMMIT) {
      AwsCodeCommitSpec awsCodeCommitSpec = (AwsCodeCommitSpec) webhookTriggerConfig.getSpec();
      AwsCodeCommitEventSpec awsCodeCommitEventSpec = awsCodeCommitSpec.getSpec();
      if (GitAware.class.isAssignableFrom(awsCodeCommitEventSpec.getClass())) {
        gitAware = (GitAware) awsCodeCommitEventSpec;
      }
    }

    return gitAware;
  }

  public PayloadAware retrievePayloadAware(WebhookTriggerConfigV2 webhookTriggerConfig) {
    if (!isGitSpec(webhookTriggerConfig)) {
      return null;
    }

    PayloadAware payloadAware = null;
    if (webhookTriggerConfig.getType() == GITHUB) {
      GithubSpec githubSpec = (GithubSpec) webhookTriggerConfig.getSpec();
      GithubEventSpec gitEventSpec = githubSpec.getSpec();
      if (PayloadAware.class.isAssignableFrom(gitEventSpec.getClass())) {
        payloadAware = (PayloadAware) gitEventSpec;
      }
    } else if (webhookTriggerConfig.getType() == GITLAB) {
      GitlabSpec gitlabSpec = (GitlabSpec) webhookTriggerConfig.getSpec();
      GitlabEventSpec gitlabEventSpec = gitlabSpec.getSpec();
      if (PayloadAware.class.isAssignableFrom(gitlabEventSpec.getClass())) {
        payloadAware = (PayloadAware) gitlabEventSpec;
      }
    } else if (webhookTriggerConfig.getType() == BITBUCKET) {
      BitbucketSpec bitbucketSpec = (BitbucketSpec) webhookTriggerConfig.getSpec();
      BitbucketEventSpec bitbucketEventSpec = bitbucketSpec.getSpec();
      if (PayloadAware.class.isAssignableFrom(bitbucketEventSpec.getClass())) {
        payloadAware = (PayloadAware) bitbucketEventSpec;
      }
    } else if (webhookTriggerConfig.getType() == AWS_CODECOMMIT) {
      AwsCodeCommitSpec awsCodeCommitSpec = (AwsCodeCommitSpec) webhookTriggerConfig.getSpec();
      AwsCodeCommitEventSpec awsCodeCommitEventSpec = awsCodeCommitSpec.getSpec();
      if (PayloadAware.class.isAssignableFrom(awsCodeCommitEventSpec.getClass())) {
        payloadAware = (PayloadAware) awsCodeCommitEventSpec;
      }
    } else if (webhookTriggerConfig.getType() == WebhookTriggerType.CUSTOM) {
      payloadAware = (PayloadAware) webhookTriggerConfig.getSpec();
    }

    return payloadAware;
  }

  public List<TriggerEventDataCondition> retrievePayloadConditions(WebhookTriggerConfigV2 webhookTriggerConfig) {
    PayloadAware payloadAware = null;
    payloadAware = retrievePayloadAware(webhookTriggerConfig);
    if (payloadAware != null) {
      return payloadAware.fetchPayloadConditions();
    }

    return emptyList();
  }

  public List<TriggerEventDataCondition> retrieveHeaderConditions(WebhookTriggerConfigV2 webhookTriggerConfig) {
    PayloadAware payloadAware = null;
    payloadAware = retrievePayloadAware(webhookTriggerConfig);
    if (payloadAware != null) {
      return payloadAware.fetchHeaderConditions();
    }

    return emptyList();
  }

  public String retrieveJexlExpression(WebhookTriggerConfigV2 webhookTriggerConfig) {
    PayloadAware payloadAware = null;
    payloadAware = retrievePayloadAware(webhookTriggerConfig);
    if (payloadAware != null) {
      return payloadAware.fetchJexlCondition();
    }

    return null;
  }

  public boolean isGitSpec(WebhookTriggerConfigV2 webhookTriggerConfig) {
    return webhookTriggerConfig.getType() == GITHUB || webhookTriggerConfig.getType() == GITLAB
        || webhookTriggerConfig.getType() == BITBUCKET || webhookTriggerConfig.getType() == AWS_CODECOMMIT;
  }

  public static List<GithubPRAction> getGithubPRAction() {
    return Arrays.asList(GithubPRAction.values());
  }

  public static List<GithubIssueCommentAction> getGithubIssueCommentAction() {
    return Arrays.asList(GithubIssueCommentAction.values());
  }

  public static List<GitlabPRAction> getGitlabPRAction() {
    return Arrays.asList(GitlabPRAction.values());
  }

  public static List<BitbucketPRAction> getBitbucketPRAction() {
    return Arrays.asList(BitbucketPRAction.values());
  }

  public static List<WebhookTriggerType> getWebhookTriggerType() {
    return Arrays.asList(WebhookTriggerType.values());
  }

  public static Map<String, Map<String, List<String>>> getGitTriggerEventDetails() {
    Map<String, Map<String, List<String>>> resposeMap = new HashMap<>();

    Map githubMap = new HashMap<GitEvent, List<GitAction>>();
    resposeMap.put(GITHUB.getValue(), githubMap);
    githubMap.put(GithubTriggerEvent.PUSH.getValue(), emptyList());
    githubMap.put(GithubTriggerEvent.PULL_REQUEST.getValue(),
        getGithubPRAction().stream().map(githubPRAction -> githubPRAction.getValue()).collect(toList()));
    githubMap.put(GithubTriggerEvent.ISSUE_COMMENT.getValue(),
        getGithubIssueCommentAction()
            .stream()
            .map(githubIssueCommentAction -> githubIssueCommentAction.getValue())
            .collect(toList()));

    Map gitlabMap = new HashMap<GitEvent, List<GitAction>>();
    resposeMap.put(GITLAB.getValue(), gitlabMap);
    gitlabMap.put(GitlabTriggerEvent.PUSH.getValue(), emptyList());
    gitlabMap.put(GitlabTriggerEvent.MERGE_REQUEST.getValue(),
        getGitlabPRAction().stream().map(gitlabPRAction -> gitlabPRAction.getValue()).collect(toList()));

    Map bitbucketMap = new HashMap<GitEvent, List<GitAction>>();
    resposeMap.put(BITBUCKET.getValue(), bitbucketMap);
    bitbucketMap.put(BitbucketTriggerEvent.PUSH.getValue(), emptyList());
    bitbucketMap.put(BitbucketTriggerEvent.PULL_REQUEST.getValue(),
        getBitbucketPRAction().stream().map(bitbucketPRAction -> bitbucketPRAction.getValue()).collect(toList()));

    Map awsCodeCommitMap = new HashMap<GitEvent, List<GitAction>>();
    resposeMap.put(AWS_CODECOMMIT.getValue(), awsCodeCommitMap);
    awsCodeCommitMap.put(AwsCodeCommitTriggerEvent.PUSH.getValue(), emptyList());

    return resposeMap;
  }
}
