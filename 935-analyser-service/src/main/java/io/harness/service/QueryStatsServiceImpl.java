package io.harness.service;

import static io.harness.mongo.tracing.TracerConstants.ANALYZER_CACHE_NAME;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.analyserservice.AnalyserServiceConfiguration;
import io.harness.beans.alerts.AlertMetadata;
import io.harness.beans.alerts.CollScanAlertInfo;
import io.harness.beans.alerts.ManyEntriesExaminedAlertInfo;
import io.harness.beans.alerts.SlowQueryAlertInfo;
import io.harness.beans.alerts.SortStageAlertInfo;
import io.harness.event.ExecutionStats;
import io.harness.event.QueryAlertCategory;
import io.harness.event.QueryExplainResult;
import io.harness.event.QueryRecordEntity;
import io.harness.event.QueryStats;
import io.harness.event.QueryStats.QueryStatsKeys;
import io.harness.eventsframework.impl.redis.DistributedCache;
import io.harness.repositories.QueryStatsRepository;
import io.harness.service.beans.QueryRecordKey;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class QueryStatsServiceImpl implements QueryStatsService {
  private static final int size = 100;

  @Inject @Named(ANALYZER_CACHE_NAME) DistributedCache queryRecordCache;
  @Inject QueryStatsRepository queryStatsRepository;
  @Inject private MongoTemplate mongoTemplate;
  @Inject AnalyserServiceConfiguration analyserServiceConfiguration;

  public void storeHashesInsideCache() {
    int page = 1;
    while (queryStatsRepository.findAllHashes(page, size, queryRecordCache)) {
      page++;
    }
  }

  public void updateQueryStatsByAggregation(Map<QueryRecordKey, List<QueryRecordEntity>> queryRecordKeyListMap) {
    for (QueryRecordKey queryRecordKey : queryRecordKeyListMap.keySet()) {
      List<QueryRecordEntity> queryRecordsPerUniqueEntry = queryRecordKeyListMap.get(queryRecordKey);
      QueryStats averageAggregatedStats = getAverageAggregatedStats(queryRecordsPerUniqueEntry);
      if (averageAggregatedStats == null) {
        continue;
      }
      Query query =
          query(Criteria.where(QueryStatsKeys.hash).is(averageAggregatedStats.getHash()))
              .addCriteria(Criteria.where(QueryStatsKeys.serviceName).is(averageAggregatedStats.getServiceName()))
              .addCriteria(Criteria.where(QueryStatsKeys.version).is(averageAggregatedStats.getVersion()));
      mongoTemplate.remove(query, QueryStats.class);
      queryStatsRepository.save(averageAggregatedStats);
    }
  }

  private QueryStats getAverageAggregatedStats(List<QueryRecordEntity> queryRecordEntityList) {
    if (queryRecordEntityList.isEmpty()) {
      return null;
    }
    QueryRecordEntity latestQueryRecord = queryRecordEntityList.get(0);
    QueryExplainResult averageExplainResult = getAverageExplainResult(queryRecordEntityList);
    return QueryStats.builder()
        .hash(latestQueryRecord.getHash())
        .serviceName(latestQueryRecord.getServiceName())
        .version(latestQueryRecord.getVersion())
        .data(ByteString.copyFrom(latestQueryRecord.getData()).toStringUtf8())
        .parsedQuery(latestQueryRecord.getParsedQuery())
        .collectionName(latestQueryRecord.getCollectionName())
        .count((long) queryRecordEntityList.size())
        .alerts(getAlertsDataFromLatestRecord(latestQueryRecord))
        .explainResult(averageExplainResult)
        .executionTimeMillis(averageExplainResult.getExecutionStats().getExecutionTimeMillis())
        .indexUsed(isIndexUsed(averageExplainResult))
        .alerts(getAlertsDataFromLatestRecord(latestQueryRecord))
        .build();
  }

  private QueryExplainResult getAverageExplainResult(List<QueryRecordEntity> queryRecordEntityList) {
    QueryRecordEntity latestQueryRecord = queryRecordEntityList.get(0);

    long nReturned = 0;
    long executionTimeMillis = 0;
    long totalDocsExamined = 0;

    for (QueryRecordEntity queryRecordEntity : queryRecordEntityList) {
      nReturned += queryRecordEntity.getExplainResult().getExecutionStats().getNReturned();
      executionTimeMillis += queryRecordEntity.getExplainResult().getExecutionStats().getExecutionTimeMillis();
      totalDocsExamined += queryRecordEntity.getExplainResult().getExecutionStats().getTotalDocsExamined();
    }
    nReturned /= queryRecordEntityList.size();
    executionTimeMillis /= queryRecordEntityList.size();
    totalDocsExamined /= queryRecordEntityList.size();

    return QueryExplainResult.builder()
        .queryPlanner(latestQueryRecord.getExplainResult().getQueryPlanner())
        .executionStats(
            ExecutionStats.builder()
                .nReturned(nReturned)
                .executionTimeMillis(executionTimeMillis)
                .totalDocsExamined(totalDocsExamined)
                .executionStages(latestQueryRecord.getExplainResult().getExecutionStats().getExecutionStages())
                .build())
        .build();
  }

  private List<AlertMetadata> getAlertsDataFromLatestRecord(QueryRecordEntity queryRecordEntity) {
    List<AlertMetadata> alerts = new LinkedList<>();
    QueryExplainResult explainResult = queryRecordEntity.getExplainResult();
    if (explainResult == null) {
      return Collections.emptyList();
    }
    boolean indexUsed = isIndexUsed(explainResult);
    if (!indexUsed) {
      alerts.add(AlertMetadata.builder()
                     .alertCategory(QueryAlertCategory.COLLSCAN)
                     .alertInfo(CollScanAlertInfo.builder().build())
                     .build());
    }
    if (explainResult.getExecutionStats() != null
        && explainResult.getExecutionStats().getExecutionTimeMillis()
            > analyserServiceConfiguration.getExecutionTimeLimitMillis()) {
      alerts.add(AlertMetadata.builder()
                     .alertCategory(QueryAlertCategory.SLOW_QUERY)
                     .alertInfo(SlowQueryAlertInfo.builder().executionStats(explainResult.getExecutionStats()).build())
                     .build());
    }
    if (explainResult.getQueryPlanner().getWinningPlan().getStage().equals("SORT")) {
      alerts.add(AlertMetadata.builder()
                     .alertCategory(QueryAlertCategory.SORT_STAGE)
                     .alertInfo(SortStageAlertInfo.builder().queryPlanner(explainResult.getQueryPlanner()).build())
                     .build());
    }
    if (explainResult.getExecutionStats().getTotalDocsExamined()
        > analyserServiceConfiguration.getManyEntriesAlertFactor() * explainResult.getExecutionStats().getNReturned()) {
      alerts.add(
          AlertMetadata.builder()
              .alertCategory(QueryAlertCategory.MANY_ENTRIES_EXAMINED)
              .alertInfo(
                  ManyEntriesExaminedAlertInfo.builder().executionStats(explainResult.getExecutionStats()).build())
              .build());
    }
    return alerts;
  }

  private boolean isIndexUsed(QueryExplainResult queryExplainResult) {
    return queryExplainResult != null
        && queryExplainResult.getQueryPlanner().getWinningPlan().getStage().equals("IXSCAN");
  }
}
