package software.wings.service.intfc.analysis;

import software.wings.api.InstanceElement;
import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.AnalysisServiceImpl.CLUSTER_TYPE;
import software.wings.service.impl.analysis.CVCollaborationProviderParameters;
import software.wings.service.impl.analysis.CVFeedbackRecord;
import software.wings.service.impl.analysis.FeedbackAction;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLClusterSummary;
import software.wings.service.impl.analysis.LogMLExpAnalysisInfo;
import software.wings.service.impl.analysis.LogMLFeedback;
import software.wings.service.impl.analysis.LogMLFeedbackRecord;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.impl.splunk.LogMLClusterScores.LogMLScore;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AnalysisService {
  LogMLAnalysisSummary getAnalysisSummary(String stateExecutionId, String appId, StateType stateType);

  void validateConfig(
      @NotNull SettingAttribute settingAttribute, StateType stateType, List<EncryptedDataDetail> encryptedDataDetails);

  /**
   * Method to return Sample data for given configurations.
   * @param accountId
   * @param analysisServerConfigId
   * @param index
   * @param stateType
   * @param duration
   * @return
   */
  Object getLogSample(String accountId, String analysisServerConfigId, String index, StateType stateType, int duration);

  void createAndSaveSummary(StateType stateType, String appId, String stateExecutionId, String query, String message);

  boolean isStateValid(String appId, String stateExecutionID);

  String getLastSuccessfulWorkflowExecutionIdWithLogs(String stateExecutionId, StateType stateType, String appId,
      String serviceId, String workflowId, String query, String infraMappingId);

  boolean saveFeedback(LogMLFeedback feedback, StateType stateType);

  List<LogMLFeedbackRecord> getMLFeedback(
      String appId, String serviceId, String workflowId, String workflowExecutionId);

  List<LogMLFeedbackRecord> get24x7MLFeedback(String cvConfigId);

  List<LogMLFeedbackRecord> getMLFeedback(String accountId, String workflowId);

  void cleanUpForLogRetry(String stateExecutionId);

  boolean deleteFeedback(String feedbackId) throws IOException;

  LogMLAnalysisSummary getAnalysisSummaryForDemo(String stateExecutionId, String applicationId, StateType stateType);

  Object getHostLogRecords(String accountId, String analysisServerConfigId, String index, ElkQueryType queryType,
      String query, String timeStampField, String timeStampFieldFormat, String messageField, String hostNameField,
      String hostName, StateType stateType);

  Map<String, InstanceElement> getLastExecutionNodes(String appId, String workflowId);
  List<LogMLExpAnalysisInfo> getExpAnalysisInfoList();
  LogMLAnalysisSummary getExperimentalAnalysisSummary(
      String stateExecutionId, String appId, StateType stateType, String expName);
  List<LogMLClusterSummary> computeCluster(Map<String, Map<String, SplunkAnalysisCluster>> cluster,
      Map<String, LogMLScore> clusterScores, CLUSTER_TYPE cluster_type);

  boolean save24x7Feedback(LogMLFeedback feedback, String cvConfigId);
  List<CVFeedbackRecord> getFeedbacks(String cvConfigId, String stateExecutionId);
  boolean addToBaseline(String accountId, String cvConfigId, String stateExecutionId, CVFeedbackRecord feedbackRecord);
  boolean removeFromBaseline(
      String accountId, String cvConfigId, String stateExecutionId, CVFeedbackRecord feedbackRecord);
  boolean updateFeedbackPriority(
      String accountId, String cvConfigId, String stateExecutionId, CVFeedbackRecord feedbackRecord);
  Map<FeedbackAction, List<FeedbackAction>> getNextFeedbackActions();
  String createCollaborationFeedbackTicket(String accountId, String appId, String cvConfigId, String stateExecutionId,
      CVCollaborationProviderParameters cvJiraParameters);
  void updateClustersWithFeedback(Map<CLUSTER_TYPE, Map<Integer, CVFeedbackRecord>> clusterTypeRecordMap,
      CLUSTER_TYPE type, List<LogMLClusterSummary> clusterList);
}
