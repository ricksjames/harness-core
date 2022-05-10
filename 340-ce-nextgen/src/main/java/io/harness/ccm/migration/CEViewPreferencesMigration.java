package io.harness.ccm.migration;

import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.dao.CEViewDao;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewPreferences;
import io.harness.migration.NGMigration;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CE)
public class CEViewPreferencesMigration implements NGMigration {
  @Inject CEViewDao ceViewDao;
  @Inject private HPersistence hPersistence;

  @Override
  public void migrate() {
    try {
      log.info("Starting migration (updates) of all CE Views Preferences");
      List<CEView> ceViewList = hPersistence.createQuery(CEView.class, excludeValidate).asList();
      for (CEView ceView : ceViewList) {
        try {
          migrateCEViewPreferences(ceView);
        } catch (Exception e) {
          log.info("Migration Failed for Account {}, ViewId {}", ceView.getAccountId(), ceView.getUuid());
        }
      }
    } catch (Exception e) {
      log.error("Failure occurred in CEViewsPreferencesMigration", e);
    }
    log.info("CEViewsPreferencesMigration has completed");
  }

  private void migrateCEViewPreferences(CEView ceView) {
    ceView.setViewPreferences(ViewPreferences.builder().showOthers(false).showUnallocated(false).build());
    ceViewDao.update(ceView);
  }
}
