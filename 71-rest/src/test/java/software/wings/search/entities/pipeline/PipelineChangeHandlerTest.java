package software.wings.search.entities.pipeline;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.audit.AuditHeader;
import software.wings.beans.AccountType;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.search.entities.application.ApplicationEntityTestUtils;
import software.wings.search.entities.environment.EnvironmentEntityTestUtils;
import software.wings.search.entities.pipeline.PipelineView.PipelineViewKeys;
import software.wings.search.entities.related.RelatedAuditEntityTestUtils;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.service.ServiceEntityTestUtils;
import software.wings.search.entities.workflow.WorkflowEntityTestUtils;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.io.IOException;

public class PipelineChangeHandlerTest extends WingsBaseTest {
  @Mock private SearchDao searchDao;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Inject @InjectMocks private PipelineChangeHandler pipelineChangeHandler;

  private static final String APP_NAME = "PipelineHandlerTestForApplication" + System.currentTimeMillis();
  private static final String SERVICE_NAME = "PipelineHandlerTestForService" + System.currentTimeMillis();
  private static final String ENVIRONMENT_NAME = "PipelineHandlerTestForEnvironment" + System.currentTimeMillis();
  private static final String WORKFLOW_NAME = "PipelineHandlerTestForWorkflow" + System.currentTimeMillis();
  private static final String PIPELINE_NAME = "PipelineHandlerTestForPipeline" + System.currentTimeMillis();

  private Application application;
  private Service service;
  private Environment environment;
  private Workflow workflow;
  private Pipeline pipeline;
  private AuditHeader deleteAuditHeader;
  private AuditHeader nonDeleteAuditHeader;
  private ChangeEvent deleteAuditHeaderChangeEvent;
  private ChangeEvent nonDeleteAuditHeaderChangeEvent;
  private String accountId = getAccount(AccountType.PAID).getUuid();
  private String nonDeleteAuditHeaderId = generateUuid();
  private String deleteAuditHeaderId = generateUuid();
  private String appId = generateUuid();
  private String serviceId = generateUuid();
  private String environmentId = generateUuid();
  private String workflowId = generateUuid();
  private String pipelineId = generateUuid();

  @Before
  public void setup() throws IOException {
    application = ApplicationEntityTestUtils.createApplication(accountId, appId, APP_NAME);
    wingsPersistence.save(application);
    assertThat(application).isNotNull();

    service = ServiceEntityTestUtils.createService(accountId, appId, serviceId, SERVICE_NAME);
    assertThat(service).isNotNull();

    environment = EnvironmentEntityTestUtils.createEnvironment(accountId, appId, environmentId, ENVIRONMENT_NAME);
    assertThat(environment).isNotNull();

    workflow = WorkflowEntityTestUtils.createWorkflow(accountId, appId, workflowId, WORKFLOW_NAME);
    assertThat(environment).isNotNull();

    pipeline = PipelineEntityTestUtils.createPipeline(accountId, appId, pipelineId, PIPELINE_NAME);
    assertThat(pipeline).isNotNull();

    deleteAuditHeader = RelatedAuditEntityTestUtils.createAuditHeader(
        deleteAuditHeaderId, accountId, appId, pipelineId, EntityType.PIPELINE.name(), Type.DELETE.name());
    wingsPersistence.save(deleteAuditHeader);
    assertThat(deleteAuditHeader).isNotNull();

    nonDeleteAuditHeader = RelatedAuditEntityTestUtils.createAuditHeader(
        nonDeleteAuditHeaderId, accountId, appId, pipelineId, EntityType.PIPELINE.name(), Type.UPDATE.name());
    wingsPersistence.save(nonDeleteAuditHeader);
    assertThat(nonDeleteAuditHeader).isNotNull();

    deleteAuditHeaderChangeEvent = RelatedAuditEntityTestUtils.createAuditHeaderChangeEvent(
        deleteAuditHeader, appId, ChangeType.UPDATE, pipelineId, Type.DELETE.name());
    assertThat(deleteAuditHeaderChangeEvent).isNotNull();

    nonDeleteAuditHeaderChangeEvent = RelatedAuditEntityTestUtils.createAuditHeaderChangeEvent(
        nonDeleteAuditHeader, appId, ChangeType.UPDATE, pipelineId, Type.UPDATE.name());
    assertThat(nonDeleteAuditHeaderChangeEvent).isNotNull();
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(UnitTests.class)
  public void testAuditRelatedChange() {
    boolean isSuccessful = pipelineChangeHandler.handleChange(deleteAuditHeaderChangeEvent);
    assertThat(isSuccessful).isNotNull();
    assertThat(isSuccessful).isTrue();

    when(searchDao.addTimestamp(eq(PipelineSearchEntity.TYPE), eq(PipelineViewKeys.auditTimestamps), anyString(),
             eq(nonDeleteAuditHeader.getCreatedAt()), eq(7)))
        .thenReturn(true);
    when(searchDao.appendToListInSingleDocument(
             eq(PipelineSearchEntity.TYPE), eq(PipelineViewKeys.audits), anyString(), anyMap(), eq(3)))
        .thenReturn(true);
    boolean result = pipelineChangeHandler.handleChange(nonDeleteAuditHeaderChangeEvent);
    assertThat(result).isNotNull();
    assertThat(result).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testPipelineInsertChange() {
    Pipeline pipeline = PipelineEntityTestUtils.createPipeline(accountId, appId, pipelineId, PIPELINE_NAME);
    assertThat(pipeline).isNotNull();
    ChangeEvent pipelineInsertChangeEvent =
        PipelineEntityTestUtils.createPipelineChangeEvent(pipeline, ChangeType.INSERT);
    when(searchDao.upsertDocument(eq(PipelineSearchEntity.TYPE), eq(pipelineId), any())).thenReturn(true);
    boolean isInsertSuccessful = pipelineChangeHandler.handleChange(pipelineInsertChangeEvent);
    assertThat(isInsertSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testPipelineDeleteChange() {
    Pipeline pipeline = PipelineEntityTestUtils.createPipeline(accountId, appId, pipelineId, PIPELINE_NAME);
    assertThat(pipeline).isNotNull();
    ChangeEvent pipelineDeleteChangeEvent =
        PipelineEntityTestUtils.createPipelineChangeEvent(pipeline, ChangeType.DELETE);
    when(searchDao.deleteDocument(PipelineSearchEntity.TYPE, pipelineId)).thenReturn(true);
    boolean isDeleteSuccessful = pipelineChangeHandler.handleChange(pipelineDeleteChangeEvent);
    assertThat(isDeleteSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testPipelineUpdateChange() {
    Pipeline pipeline = PipelineEntityTestUtils.createPipeline(accountId, appId, pipelineId, PIPELINE_NAME);
    assertThat(pipeline).isNotNull();
    ChangeEvent pipelineUpdateChangeEvent =
        PipelineEntityTestUtils.createPipelineChangeEvent(pipeline, ChangeType.UPDATE);
    when(searchDao.upsertDocument(eq(PipelineSearchEntity.TYPE), eq(pipelineId), any())).thenReturn(true);
    boolean isUpdateSuccessful = pipelineChangeHandler.handleChange(pipelineUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testServiceUpdateChange() {
    ChangeEvent serviceUpdateChangeEvent = ServiceEntityTestUtils.createServiceChangeEvent(service, ChangeType.UPDATE);
    when(searchDao.updateListInMultipleDocuments(
             eq(PipelineSearchEntity.TYPE), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);
    boolean isUpdateSuccessful = pipelineChangeHandler.handleChange(serviceUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testEnvironmentUpdateChange() {
    ChangeEvent environmentUpdateChangeEvent =
        EnvironmentEntityTestUtils.createEnvironmentChangeEvent(environment, ChangeType.UPDATE);
    when(searchDao.updateKeyInMultipleDocuments(
             eq(PipelineSearchEntity.TYPE), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);

    boolean isUpdateSuccessful = pipelineChangeHandler.handleChange(environmentUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testApplicationUpdateChange() {
    ChangeEvent applicationUpdateChangeEvent =
        ApplicationEntityTestUtils.createApplicationChangeEvent(application, ChangeType.UPDATE);

    when(searchDao.updateKeyInMultipleDocuments(
             eq(PipelineSearchEntity.TYPE), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);

    boolean isUpdateSuccessful = pipelineChangeHandler.handleChange(applicationUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testWorkflowUpdateChange() {
    ChangeEvent workflowUpdateChangeEvent =
        WorkflowEntityTestUtils.createWorkflowChangeEvent(workflow, ChangeType.UPDATE);

    when(searchDao.updateListInMultipleDocuments(
             eq(PipelineSearchEntity.TYPE), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);

    boolean isUpdateSuccessful = pipelineChangeHandler.handleChange(workflowUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }
}
