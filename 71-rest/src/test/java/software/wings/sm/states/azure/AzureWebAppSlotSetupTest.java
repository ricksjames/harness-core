package software.wings.sm.states.azure;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ANIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotSetupResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.ResponseData;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureWebAppInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnit;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupContextElement;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupExecutionData;
import software.wings.sm.states.azure.appservices.AzureAppServiceStateData;
import software.wings.sm.states.azure.appservices.AzureWebAppSlotSetup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AzureWebAppSlotSetupTest extends WingsBaseTest {
  @Mock private DelegateService delegateService;
  @Mock private AzureVMSSStateHelper azureVMSSStateHelper;
  @Spy @InjectMocks AzureWebAppSlotSetup state = new AzureWebAppSlotSetup("Slot Setup state");

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotSetupExecuteSuccess() {
    String appId = "appId";
    String serviceId = "serviceId";
    String envId = "envId";
    String activityId = "activityId";
    String delegateResult = "Done";

    Application app = Application.Builder.anApplication().uuid(appId).build();
    Environment env = Environment.Builder.anEnvironment().uuid(envId).build();
    Service service = Service.builder().uuid(serviceId).build();
    Activity activity = Activity.builder().uuid(activityId).build();

    AzureWebAppInfrastructureMapping azureWebAppInfrastructureMapping = AzureWebAppInfrastructureMapping.builder()
                                                                            .resourceGroup("rg")
                                                                            .subscriptionId("subId")
                                                                            .webApp("app-service")
                                                                            .deploymentSlot("stage")
                                                                            .build();

    AzureConfig azureConfig = AzureConfig.builder().build();
    Artifact artifact = Artifact.Builder.anArtifact().build();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();

    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    ManagerExecutionLogCallback managerExecutionLogCallback = mock(ManagerExecutionLogCallback.class);

    AzureAppServiceStateData appServiceStateData = AzureAppServiceStateData.builder()
                                                       .application(app)
                                                       .environment(env)
                                                       .service(service)
                                                       .infrastructureMapping(azureWebAppInfrastructureMapping)
                                                       .deploymentSlot("stage")
                                                       .resourceGroup("rg")
                                                       .subscriptionId("subId")
                                                       .azureConfig(azureConfig)
                                                       .artifact(artifact)
                                                       .azureEncryptedDataDetails(encryptedDataDetails)
                                                       .appService("app-service")
                                                       .build();

    doReturn(appServiceStateData).when(azureVMSSStateHelper).populateAzureAppServiceData(context);

    doReturn(activity)
        .when(azureVMSSStateHelper)
        .createAndSaveActivity(any(), any(), anyString(), anyString(), any(), anyListOf(CommandUnit.class));
    doReturn(managerExecutionLogCallback).when(azureVMSSStateHelper).getExecutionLogCallback(activity);

    doReturn(delegateResult).when(delegateService).queueTask(any());
    ExecutionResponse result = state.execute(context);

    assertThat(result).isNotNull();
    assertThat(result.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(result.getErrorMessage()).isNull();
    assertThat(result.getStateExecutionData()).isNotNull();
    assertThat(result.getStateExecutionData()).isInstanceOf(AzureAppServiceSlotSetupExecutionData.class);
    assertThat(((AzureAppServiceSlotSetupExecutionData) result.getStateExecutionData()).getActivityId())
        .isEqualTo(activityId);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotSetupExecuteFailure() {
    Activity activity = Activity.builder().uuid("activityId").build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    ManagerExecutionLogCallback managerExecutionLogCallback = mock(ManagerExecutionLogCallback.class);

    doReturn(activity)
        .when(azureVMSSStateHelper)
        .createAndSaveActivity(any(), any(), anyString(), anyString(), any(), anyListOf(CommandUnit.class));
    doReturn(managerExecutionLogCallback).when(azureVMSSStateHelper).getExecutionLogCallback(activity);
    doThrow(Exception.class).when(azureVMSSStateHelper).populateAzureAppServiceData(eq(context));

    ExecutionResponse response = state.execute(context);
    assertThat(response.getExecutionStatus()).isEqualTo(FAILED);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotSetupHandleAsyncResponse() {
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    doNothing().when(azureVMSSStateHelper).updateActivityStatus(anyString(), anyString(), any());
    doReturn(SUCCESS).when(azureVMSSStateHelper).getAppServieExecutionStatus(any());
    Map<String, ResponseData> responseMap = ImmutableMap.of(ACTIVITY_ID,
        AzureTaskExecutionResponse.builder()
            .azureTaskResponse(AzureWebAppSlotSetupResponse.builder()
                                   .preDeploymentData(AzureAppServicePreDeploymentData.builder()
                                                          .appName("webApp-for-deployment")
                                                          .appSettings(Collections.emptyMap())
                                                          .connSettings(Collections.emptyMap())
                                                          .slotName("stage")
                                                          .build())
                                   .build())
            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
            .build());
    AzureAppServiceSlotSetupExecutionData data = AzureAppServiceSlotSetupExecutionData.builder().build();
    doReturn(data).when(context).getStateExecutionData();

    ExecutionResponse executionResponse = state.handleAsyncResponse(context, responseMap);

    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SUCCESS);
    List<ContextElement> notifyElements = executionResponse.getNotifyElements();
    assertThat(notifyElements).isNotNull();
    assertThat(notifyElements.size()).isEqualTo(1);
    ContextElement contextElement = notifyElements.get(0);
    assertThat(contextElement).isNotNull();
    assertThat(contextElement instanceof AzureAppServiceSlotSetupContextElement).isTrue();
    AzureAppServiceSlotSetupContextElement slotSetupContextElement =
        (AzureAppServiceSlotSetupContextElement) contextElement;
    assertThat(slotSetupContextElement.getWebApp()).isEqualTo("webApp-for-deployment");
    assertThat(slotSetupContextElement.getDeploymentSlot()).isEqualTo("stage");
  }
}
