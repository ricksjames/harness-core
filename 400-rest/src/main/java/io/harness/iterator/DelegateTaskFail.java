package io.harness.iterator;

import com.google.inject.Inject;
import io.harness.beans.DelegateTask;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import software.wings.beans.Account;

public class DelegateTaskFail implements MongoPersistenceIterator.Handler<DelegateTask>{
    @Inject
    private PersistenceIteratorFactory persistenceIteratorFactory;
    @Inject private MorphiaPersistenceProvider<DelegateTask> persistenceProvider;
    @Override
    public void handle(DelegateTask entity) {

    }
}
