package io.harness.waiter;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SAHIL;
import static io.harness.waiter.TestNotifyEventListener.TEST_PUBLISHER;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.WaitEngineTestBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.testlib.RealMongo;
import io.harness.threading.Morpheus;
import io.harness.waiter.persistence.PersistenceWrapper;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Test;
import org.junit.experimental.categories.Category;
public class ProgressUpdateServiceTest extends WaitEngineTestBase {
  String waitInstanceId = generateUuid();
  String correlationId = generateUuid();
  @Inject private PersistenceWrapper persistenceWrapper;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private WaitInstanceService waitInstanceService;

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  @RealMongo
  public void testModifyAndFetchWaitInstanceForNoExistingResponse() throws InterruptedException {
    final WaitInstance waitInstance = WaitInstance.builder()
                                          .uuid(waitInstanceId)
                                          .callback(new TestNotifyCallback())
                                          .progressCallback(new TestProgressCallback())
                                          .publisher(TEST_PUBLISHER)
                                          .correlationIds(Collections.singletonList(correlationId))
                                          .waitingOnCorrelationIds(Collections.singletonList(correlationId))
                                          .build();
    persistenceWrapper.save(waitInstance);

    persistenceWrapper.save(ProgressUpdate.builder()
                                .uuid(generateUuid())
                                .correlationId(correlationId)
                                .createdAt(currentTimeMillis() - 1000)
                                .expireProcessing(currentTimeMillis() + 60000)
                                .progressData(kryoSerializer.asDeflatedBytes(
                                    StringNotifyProgressData.builder().data("progress1-" + generateUuid()).build()))
                                .build());

    persistenceWrapper.save(ProgressUpdate.builder()
                                .uuid(generateUuid())
                                .correlationId(correlationId)
                                .createdAt(currentTimeMillis())
                                .expireProcessing(currentTimeMillis())
                                .progressData(kryoSerializer.asDeflatedBytes(
                                    StringNotifyProgressData.builder().data("progress1-" + generateUuid()).build()))
                                .build());

    Morpheus.sleep(Duration.ofMinutes(2));
    assertThat(waitInstanceService.fetchForProcessingProgressUpdate(new HashSet<>(), currentTimeMillis())).isNull();
  }
}