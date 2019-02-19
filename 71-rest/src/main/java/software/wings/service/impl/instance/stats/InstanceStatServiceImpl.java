package software.wings.service.impl.instance.stats;

import static software.wings.resources.stats.model.InstanceTimeline.top;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.persistence.HQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.User;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.dl.WingsPersistence;
import software.wings.resources.stats.model.InstanceTimeline;
import software.wings.resources.stats.rbac.TimelineRbacFilters;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.instance.stats.collector.SimplePercentile;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.instance.DashboardStatisticsService;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Mongo backed implementation for instant stat service.
 * This service is used to render the timelines and provide aggregates on user dashboard.
 */
@Singleton
@ParametersAreNonnullByDefault
public class InstanceStatServiceImpl implements InstanceStatService {
  private static final Logger log = LoggerFactory.getLogger(InstanceStatServiceImpl.class);

  @Inject private WingsPersistence persistence;
  @Inject private AppService appService;
  @Inject private UsageRestrictionsService usageRestrictionsService;
  @Inject private DashboardStatisticsService dashboardStatsService;

  @Override
  public boolean save(InstanceStatsSnapshot stats) {
    String id = persistence.save(stats);

    if (null == id) {
      log.error("Could not save instance stats. Stats: {}", stats);
      return false;
    }

    log.info("Saved stats. Time: {}, Account: {}, ID: {} ", stats.getTimestamp(), stats.getAccountId(), id);
    return true;
  }

  @Override
  public InstanceTimeline aggregate(String accountId, long fromTsMillis, long toTsMillis) {
    Instant from = Instant.ofEpochMilli(fromTsMillis);
    Instant to = Instant.ofEpochMilli(toTsMillis);

    Stopwatch stopwatch = Stopwatch.createStarted();

    List<InstanceStatsSnapshot> stats = aggregate(accountId, from, to);
    log.info("Aggregate Time: {} ms, accountId={}, from={} to={}", stopwatch.elapsed(TimeUnit.MILLISECONDS), accountId,
        from, to);
    Set<String> deletedAppIds = dashboardStatsService.getDeletedAppIds(accountId, fromTsMillis, toTsMillis);
    log.info("Get Deleted App Time: {} ms, accountId={}", stopwatch.elapsed(TimeUnit.MILLISECONDS), accountId);

    User user = UserThreadLocal.get();
    if (null != user) {
      TimelineRbacFilters rbacFilters = new TimelineRbacFilters(user, accountId, appService, usageRestrictionsService);
      List<InstanceStatsSnapshot> filteredStats = rbacFilters.filter(stats, deletedAppIds);
      log.info("Stats before and after filtering. Before: {}, After: {}", stats.size(), filteredStats.size());
      log.info("Time till RBAC filters: {} ms, accountId={}", stopwatch.elapsed(TimeUnit.MILLISECONDS), accountId);
      InstanceTimeline timeline = new InstanceTimeline(filteredStats, deletedAppIds);
      InstanceTimeline top = top(timeline, 5);
      log.info("Total time taken: {} ms, accountId={}", stopwatch.elapsed(TimeUnit.MILLISECONDS), accountId);
      return top;
    } else {
      throw new WingsException(ErrorCode.USER_DOES_NOT_EXIST);
    }
  }

  @Override
  public List<InstanceStatsSnapshot> aggregate(String accountId, Instant from, Instant to) {
    Preconditions.checkArgument(to.isAfter(from), "'to' timestamp should be after 'from'");

    Query<InstanceStatsSnapshot> query = persistence.createQuery(InstanceStatsSnapshot.class)
                                             .filter("accountId", accountId)
                                             .field("timestamp")
                                             .greaterThanOrEq(from)
                                             .field("timestamp")
                                             .lessThan(to)
                                             .project("accountId", true)
                                             .project("timestamp", true)
                                             .project("aggregateCounts", true)
                                             .project("total", true)
                                             .order(Sort.ascending("timestamp"));

    List<InstanceStatsSnapshot> timeline = new LinkedList<>();

    try (HIterator<InstanceStatsSnapshot> iterator = new HIterator<>(query.fetch())) {
      while (iterator.hasNext()) {
        timeline.add(iterator.next());
      }
    }

    return timeline;
  }

  @Override
  public @Nullable Instant getLastSnapshotTime(String accountId) {
    FindOptions options = new FindOptions();
    options.limit(1);

    List<InstanceStatsSnapshot> snapshots = persistence.createQuery(InstanceStatsSnapshot.class)
                                                .filter("accountId", accountId)
                                                .order(Sort.descending("timestamp"))
                                                .asList(options);

    if (CollectionUtils.isEmpty(snapshots)) {
      return null;
    }

    return snapshots.get(0).getTimestamp();
  }

  @Nullable
  @Override
  public Instant getFirstSnapshotTime(String accountId) {
    FindOptions options = new FindOptions();
    options.limit(1);

    List<InstanceStatsSnapshot> snapshots = persistence.createQuery(InstanceStatsSnapshot.class)
                                                .filter("accountId", accountId)
                                                .order(Sort.ascending("timestamp"))
                                                .asList(options);

    if (CollectionUtils.isEmpty(snapshots)) {
      return null;
    }

    return snapshots.get(0).getTimestamp();
  }

  @Override
  public double percentile(String accountId, Instant from, Instant to, double p) {
    Preconditions.checkArgument(to.isAfter(from), "'to' timestamp should be after 'from'");

    List<InstanceStatsSnapshot> dataPoints = persistence.createQuery(InstanceStatsSnapshot.class, HQuery.excludeCount)
                                                 .filter("accountId", accountId)
                                                 .field("timestamp")
                                                 .greaterThanOrEq(from)
                                                 .field("timestamp")
                                                 .lessThan(to)
                                                 .project("total", true)
                                                 .order(Sort.ascending("total"))
                                                 .asList();

    List<Integer> counts = dataPoints.stream().map(InstanceStatsSnapshot::getTotal).collect(Collectors.toList());
    return new SimplePercentile(counts).evaluate(p);
  }
}
