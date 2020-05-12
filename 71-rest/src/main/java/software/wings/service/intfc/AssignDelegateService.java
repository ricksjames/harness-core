package software.wings.service.intfc;

import io.harness.beans.DelegateTask;
import software.wings.beans.BatchDelegateSelectionLog;
import software.wings.beans.TaskGroup;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.List;

/**
 * Created by brett on 7/20/17
 */
public interface AssignDelegateService {
  boolean canAssign(BatchDelegateSelectionLog batch, String delegateId, DelegateTask task);

  boolean canAssign(BatchDelegateSelectionLog batch, String delegateId, String accountId, String appId, String envId,
      String infraMappingId, TaskGroup taskGroup, List<String> tags);

  boolean isWhitelisted(DelegateTask task, String delegateId);

  boolean shouldValidate(DelegateTask task, String delegateId);

  List<String> connectedWhitelistedDelegates(DelegateTask task);

  String pickFirstAttemptDelegate(DelegateTask task);

  void refreshWhitelist(DelegateTask task, String delegateId);

  void saveConnectionResults(List<DelegateConnectionResult> results);

  void clearConnectionResults(String accountId, String delegateId);

  void clearConnectionResults(String accountId);

  String getActiveDelegateAssignmentErrorMessage(DelegateTask delegateTask);

  List<String> retrieveActiveDelegates(String accountId, BatchDelegateSelectionLog batch);
}
