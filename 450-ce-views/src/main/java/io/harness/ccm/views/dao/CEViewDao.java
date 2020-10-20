package io.harness.ccm.views.dao;

import static io.harness.persistence.HQuery.excludeValidate;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.CEView.CEViewKeys;
import io.harness.ccm.views.entities.ViewState;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.List;

@Slf4j
@Singleton
public class CEViewDao {
  @Inject private HPersistence hPersistence;

  public boolean save(CEView ceView) {
    return hPersistence.save(ceView) != null;
  }

  public CEView update(CEView ceView) {
    Query query = hPersistence.createQuery(CEView.class)
                      .field(CEViewKeys.accountId)
                      .equal(ceView.getAccountId())
                      .field(CEViewKeys.uuid)
                      .equal(ceView.getUuid());
    UpdateOperations<CEView> updateOperations = hPersistence.createUpdateOperations(CEView.class)
                                                    .set(CEViewKeys.viewVersion, ceView.getViewVersion())
                                                    .set(CEViewKeys.name, ceView.getName())
                                                    .set(CEViewKeys.viewTimeRange, ceView.getViewTimeRange())
                                                    .set(CEViewKeys.viewRules, ceView.getViewRules())
                                                    .set(CEViewKeys.viewVisualization, ceView.getViewVisualization())
                                                    .set(CEViewKeys.viewType, ceView.getViewType())
                                                    .set(CEViewKeys.viewState, ViewState.COMPLETED);
    hPersistence.update(query, updateOperations);
    logger.info(query.toString());
    return (CEView) query.asList().get(0);
  }

  public boolean delete(String uuid, String accountId) {
    Query query = hPersistence.createQuery(CEView.class)
                      .field(CEViewKeys.accountId)
                      .equal(accountId)
                      .field(CEViewKeys.uuid)
                      .equal(uuid);
    return hPersistence.delete(query);
  }

  public CEView get(String uuid) {
    return hPersistence.createQuery(CEView.class, excludeValidate).filter(CEViewKeys.uuid, uuid).get();
  }

  public CEView findByName(String accountId, String name) {
    return hPersistence.createQuery(CEView.class)
        .filter(CEViewKeys.accountId, accountId)
        .filter(CEViewKeys.name, name)
        .get();
  }

  public List<CEView> findByAccountId(String accountId) {
    return hPersistence.createQuery(CEView.class).filter(CEViewKeys.accountId, accountId).asList();
  }
}
