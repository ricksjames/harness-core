package io.harness.cvng.core.jobs;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.TestVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.eventsframework.entity_crud.account.AccountEntityChangeDTO;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.Arrays;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AccountChangeEventMessageProcessorTest extends CvNextGenTest {
  @Inject private AccountChangeEventMessageProcessor accountChangeEventMessageProcessor;
  @Inject private CVConfigService cvConfigService;
  @Inject private VerificationJobService verificationJobService;

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessDeleteAction() {
    String accountId = generateUuid();
    CVConfig cvConfig = createCVConfig(accountId);
    cvConfigService.save(cvConfig);
    VerificationJobDTO verificationJobDTO1 = createVerificationJobDTO();
    verificationJobService.upsert(accountId, verificationJobDTO1);

    accountChangeEventMessageProcessor.processDeleteAction(
        AccountEntityChangeDTO.newBuilder().setAccountId(accountId).build());

    assertThat(verificationJobService.getVerificationJobDTO(accountId, verificationJobDTO1.getIdentifier())).isNull();
    assertThat(cvConfigService.get(cvConfig.getUuid())).isNull();

    // For every message processing, idemptotency is assumed - Redelivery of a message produces the same result and
    // there are no side effects
    CVConfig retrievedCVConfig = cvConfigService.get(cvConfig.getUuid());

    accountChangeEventMessageProcessor.processDeleteAction(
        AccountEntityChangeDTO.newBuilder().setAccountId(accountId).build());

    assertThat(cvConfigService.get(cvConfig.getUuid())).isNull();
    assertThat(retrievedCVConfig).isNull();
  }

  private CVConfig createCVConfig(String accountId) {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig, accountId);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier(generateUuid());
    return cvConfig;
  }

  private void fillCommon(CVConfig cvConfig, String accountId) {
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorIdentifier(generateUuid());
    cvConfig.setServiceIdentifier("service");
    cvConfig.setEnvIdentifier("env");
    cvConfig.setOrgIdentifier(generateUuid());
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(generateUuid());
    cvConfig.setIdentifier(generateUuid());
    cvConfig.setMonitoringSourceName(generateUuid());
    cvConfig.setProjectIdentifier(generateUuid());
  }

  private VerificationJobDTO createVerificationJobDTO() {
    TestVerificationJobDTO testVerificationJobDTO = new TestVerificationJobDTO();
    testVerificationJobDTO.setIdentifier(generateUuid());
    testVerificationJobDTO.setJobName(generateUuid());
    testVerificationJobDTO.setDataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS));
    testVerificationJobDTO.setMonitoringSources(Arrays.asList(generateUuid()));
    testVerificationJobDTO.setBaselineVerificationJobInstanceId(null);
    testVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    testVerificationJobDTO.setServiceIdentifier(generateUuid());
    testVerificationJobDTO.setEnvIdentifier(generateUuid());
    testVerificationJobDTO.setBaselineVerificationJobInstanceId(generateUuid());
    testVerificationJobDTO.setDuration("15m");
    testVerificationJobDTO.setOrgIdentifier(generateUuid());
    testVerificationJobDTO.setProjectIdentifier(generateUuid());
    return testVerificationJobDTO;
  }
}