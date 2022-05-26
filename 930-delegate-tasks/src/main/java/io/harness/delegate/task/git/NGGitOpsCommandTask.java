package io.harness.delegate.task.git;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogHelper.color;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitPRCreateRequest;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.connector.service.git.NGGitService;
import io.harness.connector.task.git.GitDecryptionHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitFile;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.product.ci.scm.proto.CreatePRResponse;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.SshSessionConfig;

import software.wings.beans.LogColor;
import software.wings.beans.LogWeight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.jooq.tools.json.JSONObject;
import org.jooq.tools.json.JSONParser;
import org.jooq.tools.json.ParseException;
import org.jose4j.lang.JoseException;

@Slf4j
@OwnedBy(CDP)
public class NGGitOpsCommandTask extends AbstractDelegateRunnableTask {
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private ScmFetchFilesHelperNG scmFetchFilesHelper;
  @Inject private GitFetchFilesTaskHelper gitFetchFilesTaskHelper;
  @Inject private GitDecryptionHelper gitDecryptionHelper;
  @Inject private NGGitService ngGitService;

  public static final String FetchFiles = "Fetch Files";
  public static final String UpdateFiles = "Update fetched files";
  public static final String CommitAndPush = "Commit and Push";
  public static final String CreatePR = "Create PR";

  public NGGitOpsCommandTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    return null;
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) throws IOException, JoseException {
    /*
        TODO:
        II. Merge PR
        DONE (?):
        I. Create PR
            1. Fetch Files from Git
            2. Modify/Update them
            3. Commit and Push
            4. Create a PR
     */

    CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

    try {
      NGGitOpsTaskParams gitOpsTaskParams = (NGGitOpsTaskParams) parameters;

      log.info("Running Create PR Task for activityId {}", gitOpsTaskParams.getActivityId());

      LogCallback logCallback =
          new NGDelegateLogCallback(getLogStreamingTaskClient(), FetchFiles, true, commandUnitsProgress);

      logCallback.saveExecutionLog(color(format("%nStarting Git Fetch Files"), LogColor.White, LogWeight.Bold));

      FetchFilesResult fetchFilesResult =
          fetchFilesFromRepo(gitOpsTaskParams.getGitFetchFilesConfig(), logCallback, gitOpsTaskParams.getAccountId());

      logCallback.saveExecutionLog(
          color(format("%nGit Fetch Files completed successfully."), LogColor.White, LogWeight.Bold), INFO);

      logCallback = markDoneAndStartNew(logCallback, UpdateFiles, commandUnitsProgress);

      updateFiles(gitOpsTaskParams, fetchFilesResult);

      logCallback = markDoneAndStartNew(logCallback, CommitAndPush, commandUnitsProgress);

      ScmConnector scmConnector =
          gitOpsTaskParams.getGitFetchFilesConfig().getGitStoreDelegateConfig().getGitConfigDTO();

      commitAndPush(fetchFilesResult, gitOpsTaskParams.getTargetBranch(), scmConnector);

      logCallback = markDoneAndStartNew(logCallback, CreatePR, commandUnitsProgress);

      String sourceBranch = gitOpsTaskParams.getGitFetchFilesConfig().getGitStoreDelegateConfig().getBranch();

      CreatePRResponse createPRResponse = createPullRequest(scmConnector, sourceBranch,
          gitOpsTaskParams.getTargetBranch(), gitOpsTaskParams.getPrTitle(), gitOpsTaskParams.getAccountId());

      logCallback.saveExecutionLog("Done.", INFO, CommandExecutionStatus.SUCCESS);

      return NGGitOpsResponse.builder()
          .prNumber(createPRResponse.getNumber())
          .taskStatus(TaskStatus.SUCCESS)
          .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
          .build();

    } catch (Exception e) {
      // TODO: Handle aptly
    }

    return null;
  }

  private LogCallback markDoneAndStartNew(
      LogCallback logCallback, String newName, CommandUnitsProgress commandUnitsProgress) {
    logCallback.saveExecutionLog("\nDone", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
    logCallback = new NGDelegateLogCallback(getLogStreamingTaskClient(), newName, true, commandUnitsProgress);
    return logCallback;
  }

  public CreatePRResponse createPullRequest(
      ScmConnector scmConnector, String sourceBranch, String targetBranch, String title, String accountId) {
    return scmFetchFilesHelper.createPR(scmConnector,
        GitPRCreateRequest.builder()
            .title(title)
            .sourceBranch(sourceBranch)
            .targetBranch(targetBranch)
            .accountIdentifier(accountId)
            .build());
  }

  public void commitAndPush(FetchFilesResult fetchFilesResult, String branch, ScmConnector scmConnector) {
    List<GitFileDetails> gitFileDetailsList = new ArrayList<>();

    for (GitFile gitFile : fetchFilesResult.getFiles()) {
      gitFileDetailsList.add(GitFileDetails.builder()
                                 .fileContent(gitFile.getFileContent())
                                 .filePath(gitFile.getFilePath())
                                 .commitId(fetchFilesResult.getCommitResult().getCommitId())
                                 .commitMessage(fetchFilesResult.getCommitResult().getCommitMessage())
                                 .branch(branch)
                                 /*
                                   TODO: change this -- AbstractScmClientFacilitatorServiceImpl.java line #154
                                   String scmUserName = getScmUserName(accountId, scmType);
                                  */
                                 .userName("usrName")
                                 .userEmail("xyz@abc.com")
                                 .build());
    }

    for (GitFileDetails gitFileDetails : gitFileDetailsList) {
      scmFetchFilesHelper.commitAndPush(false, gitFileDetails, branch, scmConnector);
    }
  }

  public void updateFiles(NGGitOpsTaskParams gitOpsTaskParams, FetchFilesResult fetchFilesResult)
      throws ParseException, IOException {
    Map<String, String> stringMap = gitOpsTaskParams.getStringMap();
    List<String> fetchedFilesContents = new ArrayList<>();

    for (GitFile gitFile : fetchFilesResult.getFiles()) {
      if (gitFile.getFilePath().contains(".yaml")) {
        fetchedFilesContents.add(convertYamlToJson(gitFile.getFileContent()));
      } else {
        fetchedFilesContents.add(gitFile.getFileContent());
      }
    }

    List<String> updatedFiles = replaceFields(fetchedFilesContents, stringMap);
    List<GitFile> updatedGitFiles = new ArrayList<>();

    for (int i = 0; i < updatedFiles.size(); i++) {
      GitFile gitFile = fetchFilesResult.getFiles().get(i);
      if (gitFile.getFilePath().contains(".yaml")) {
        gitFile.setFileContent(convertJsonToYaml(updatedFiles.get(i)));
      } else {
        gitFile.setFileContent(updatedFiles.get(i));
      }
      updatedGitFiles.add(gitFile);
    }

    fetchFilesResult.setFiles(updatedGitFiles);
  }

  public List<String> replaceFields(List<String> stringList, Map<String, String> stringMap) throws ParseException {
    List<String> result = new ArrayList<>();
    for (String str : stringList) {
      for (String key : stringMap.keySet()) {
        if (contains(str, key)) {
          str = replace(str, key, stringMap);
        }
      }
      result.add(str);
    }
    return result;
  }

  private boolean contains(String str, String key) {
    if (key.contains(".")) { // complex object
      String[] keys = key.split("\\.");
      boolean isSubstring = true;
      for (String s : keys) {
        isSubstring = isSubstring && str.contains(s);
      }
      return isSubstring;
    }

    // simple object
    return str.contains(key);
  }

  private String replace(String str, String key, Map<String, String> stringMap) throws ParseException {
    JSONParser parser = new JSONParser();
    JSONObject json = (JSONObject) parser.parse(str);

    if (key.contains(".")) {
      String[] keys = key.split("\\.");
      int len = keys.length - 1;
      return str.replaceAll("\"" + keys[len] + "\""
              + "\\s*:\\s*\"*" + recGet(json, keys) + "\"*",
          "\"" + keys[len] + "\": \"" + stringMap.get(key) + "\"");
    }

    return str.replaceAll("\"" + key + "\""
            + "\\s*:\\s*\"*" + json.get(key) + "\"*",
        "\"" + key + "\": \"" + stringMap.get(key) + "\"");
  }

  private String recGet(JSONObject jsonObject, String[] keys) {
    JSONObject json = jsonObject;
    int i = 0;
    while (i < keys.length - 1) {
      json = (JSONObject) json.get(keys[i]);
      i++;
    }
    return json.get(keys[i]).toString();
  }

  public String convertYamlToJson(String yaml) throws IOException {
    ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
    Object obj = yamlReader.readValue(yaml, Object.class);

    ObjectMapper jsonWriter = new ObjectMapper();
    return jsonWriter.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
  }

  public String convertJsonToYaml(String jsonString) throws IOException {
    JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
    return new YAMLMapper().writeValueAsString(jsonNodeTree);
  }

  public FetchFilesResult fetchFilesFromRepo(
      GitFetchFilesConfig gitFetchFilesConfig, LogCallback executionLogCallback, String accountId) throws IOException {
    GitStoreDelegateConfig gitStoreDelegateConfig = gitFetchFilesConfig.getGitStoreDelegateConfig();
    executionLogCallback.saveExecutionLog("Git connector Url: " + gitStoreDelegateConfig.getGitConfigDTO().getUrl());
    String fetchTypeInfo = gitStoreDelegateConfig.getFetchType() == FetchType.BRANCH
        ? "Branch: " + gitStoreDelegateConfig.getBranch()
        : "CommitId: " + gitStoreDelegateConfig.getCommitId();

    executionLogCallback.saveExecutionLog(fetchTypeInfo);

    List<String> filePathsToFetch = null;
    if (EmptyPredicate.isNotEmpty(gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths())) {
      filePathsToFetch = gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths();
      executionLogCallback.saveExecutionLog("\nFetching following Files :");
      gitFetchFilesTaskHelper.printFileNamesInExecutionLogs(filePathsToFetch, executionLogCallback);
    }

    FetchFilesResult gitFetchFilesResult;
    if (gitStoreDelegateConfig.isOptimizedFilesFetch()) {
      executionLogCallback.saveExecutionLog("Using optimized file fetch");
      secretDecryptionService.decrypt(
          GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(gitStoreDelegateConfig.getGitConfigDTO()),
          gitStoreDelegateConfig.getApiAuthEncryptedDataDetails());
      ExceptionMessageSanitizer.storeAllSecretsForSanitizing(
          GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(gitStoreDelegateConfig.getGitConfigDTO()),
          gitStoreDelegateConfig.getApiAuthEncryptedDataDetails());
      gitFetchFilesResult = scmFetchFilesHelper.fetchFilesFromRepoWithScm(gitStoreDelegateConfig, filePathsToFetch);
    } else {
      GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO(gitStoreDelegateConfig.getGitConfigDTO());
      gitDecryptionHelper.decryptGitConfig(gitConfigDTO, gitStoreDelegateConfig.getEncryptedDataDetails());
      SshSessionConfig sshSessionConfig = gitDecryptionHelper.getSSHSessionConfig(
          gitStoreDelegateConfig.getSshKeySpecDTO(), gitStoreDelegateConfig.getEncryptedDataDetails());
      gitFetchFilesResult =
          ngGitService.fetchFilesByPath(gitStoreDelegateConfig, accountId, sshSessionConfig, gitConfigDTO);
    }

    gitFetchFilesTaskHelper.printFileNamesInExecutionLogs(executionLogCallback, gitFetchFilesResult.getFiles());

    return gitFetchFilesResult;
  }
}
