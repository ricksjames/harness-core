/**
 *
 */

package software.wings.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.common.collect.Lists;
import com.google.inject.Injector;

import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.InstanceElement;
import software.wings.api.ServiceInstanceIdsParam;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.SearchFilter;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceInstance.Builder;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.List;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * The Class InstanceExpressionProcessorTest.
 *
 * @author Rishi
 */
public class InstanceExpressionProcessorTest extends WingsBaseTest {
  @Inject Injector injector;
  /**
   * The App service.
   */
  @Inject AppService appService;
  /**
   * The Environment service.
   */
  @Inject EnvironmentService environmentService;

  @Mock @Inject ServiceInstanceService serviceInstanceService;

  private String appId = UUIDGenerator.getUuid();

  /**
   * Should return instances.
   */
  @Test
  public void shouldReturnInstances() {
    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance().withAppId(appId).build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);

    PageResponse<ServiceInstance> res = new PageResponse<>();
    ServiceInstance instance1 =
        Builder.aServiceInstance()
            .withUuid(UUIDGenerator.getUuid())
            .withHost(Host.HostBuilder.aHost().withHostName("host1").build())
            .withServiceTemplate(ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate()
                                     .withName("template")
                                     .withService(Service.Builder.aService().withUuid("uuid1").withName("svc1").build())
                                     .build())
            .build();
    ServiceInstance instance2 =
        Builder.aServiceInstance()
            .withUuid(UUIDGenerator.getUuid())
            .withHost(Host.HostBuilder.aHost().withHostName("host2").build())
            .withServiceTemplate(ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate()
                                     .withName("template")
                                     .withService(Service.Builder.aService().withUuid("uuid1").withName("svc1").build())
                                     .build())
            .build();
    ServiceInstance instance3 =
        Builder.aServiceInstance()
            .withUuid(UUIDGenerator.getUuid())
            .withHost(Host.HostBuilder.aHost().withHostName("host3").build())
            .withServiceTemplate(ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate()
                                     .withName("template")
                                     .withService(Service.Builder.aService().withUuid("uuid1").withName("svc1").build())
                                     .build())
            .build();
    List<ServiceInstance> instances = Lists.newArrayList(instance1, instance2, instance3);
    res.setResponse(instances);

    when(serviceInstanceService.list(any(PageRequest.class))).thenReturn(res);

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);
    processor.setServiceInstanceService(serviceInstanceService);

    List<InstanceElement> elements = processor.list();

    assertThat(elements).isNotNull();
    assertThat(elements.size()).isEqualTo(3);
    assertThat(elements.get(0)).isNotNull();
    assertThat(elements.get(0).getUuid()).isEqualTo(instance1.getUuid());
    assertThat(elements.get(0).getHostElement()).isNotNull();
    assertThat(elements.get(0).getServiceTemplateElement()).isNotNull();
    assertThat(elements.get(1)).isNotNull();
    assertThat(elements.get(1).getUuid()).isEqualTo(instance2.getUuid());
    assertThat(elements.get(1).getHostElement()).isNotNull();
    assertThat(elements.get(1).getServiceTemplateElement()).isNotNull();
    assertThat(elements.get(2)).isNotNull();
    assertThat(elements.get(2).getUuid()).isEqualTo(instance3.getUuid());
    assertThat(elements.get(2).getHostElement()).isNotNull();
    assertThat(elements.get(2).getServiceTemplateElement()).isNotNull();
  }

  /**
   * Should return instances from param.
   */
  @Test
  public void shouldReturnInstancesFromParam() {
    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance().withAppId(appId).build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);

    String instance1 = UUIDGenerator.getUuid();
    String instance2 = UUIDGenerator.getUuid();

    ServiceInstanceIdsParam element = new ServiceInstanceIdsParam();
    element.setInstanceIds(Lists.newArrayList(instance1, instance2));

    List<ContextElement> paramList = Lists.newArrayList(element);
    when(context.getContextElementList(ContextElementType.PARAM)).thenReturn(paramList);

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);

    PageRequest<ServiceInstance> pageRequest = processor.buildPageRequest();

    assertThat(pageRequest).isNotNull();
    assertThat(pageRequest.getFilters()).isNotNull();

    boolean appIdFound = false;
    boolean instanceIdsMatched = false;
    for (SearchFilter filter : pageRequest.getFilters()) {
      assertThat(filter.getFieldValues()).isNotEmpty();
      if (filter.getFieldName().equals("appId")) {
        assertThat(filter.getFieldValues()[0]).isEqualTo(appId);
        appIdFound = true;
      } else if (filter.getFieldName().equals(ID_KEY)) {
        assertThat(filter.getFieldValues().length).isEqualTo(2);
        assertThat(filter.getFieldValues()[0]).isEqualTo(instance1);
        assertThat(filter.getFieldValues()[1]).isEqualTo(instance2);
        instanceIdsMatched = true;
      }
    }
    assertThat(appIdFound).isTrue();
    assertThat(instanceIdsMatched).isTrue();
  }

  /**
   * Should return common instances from param.
   */
  @Test
  public void shouldReturnCommonInstancesFromParam() {
    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance().withAppId(appId).build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);

    String instance1 = UUIDGenerator.getUuid();
    String instance2 = UUIDGenerator.getUuid();
    String instance3 = UUIDGenerator.getUuid();

    ServiceInstanceIdsParam element = new ServiceInstanceIdsParam();
    element.setInstanceIds(Lists.newArrayList(instance1, instance2));

    List<ContextElement> paramList = Lists.newArrayList(element);
    when(context.getContextElementList(ContextElementType.PARAM)).thenReturn(paramList);

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);

    processor.withInstanceIds(instance1, instance2, instance3);
    PageRequest<ServiceInstance> pageRequest = processor.buildPageRequest();

    assertThat(pageRequest).isNotNull();
    assertThat(pageRequest.getFilters()).isNotNull();

    boolean appIdFound = false;
    boolean instanceIdsMatched = false;
    for (SearchFilter filter : pageRequest.getFilters()) {
      assertThat(filter.getFieldValues()).isNotEmpty();
      if (filter.getFieldName().equals("appId")) {
        assertThat(filter.getFieldValues()[0]).isEqualTo(appId);
        appIdFound = true;
      } else if (filter.getFieldName().equals(ID_KEY)) {
        assertThat(filter.getFieldValues().length).isEqualTo(2);
        assertThat(filter.getFieldValues()[0]).isIn(instance1, instance2);
        assertThat(filter.getFieldValues()[1]).isIn(instance1, instance2);
        instanceIdsMatched = true;
      }
    }
    assertThat(appIdFound).isTrue();
    assertThat(instanceIdsMatched).isTrue();
  }

  /**
   * Should return common instances from param 2.
   */
  @Test
  public void shouldReturnCommonInstancesFromParam2() {
    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance().withAppId(appId).build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);

    String instance1 = UUIDGenerator.getUuid();
    String instance2 = UUIDGenerator.getUuid();

    ServiceInstanceIdsParam element = new ServiceInstanceIdsParam();
    element.setInstanceIds(Lists.newArrayList(instance1, instance2));

    List<ContextElement> paramList = Lists.newArrayList(element);
    when(context.getContextElementList(ContextElementType.PARAM)).thenReturn(paramList);

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);

    processor.withInstanceIds(instance1);
    PageRequest<ServiceInstance> pageRequest = processor.buildPageRequest();

    assertThat(pageRequest).isNotNull();
    assertThat(pageRequest.getFilters()).isNotNull();

    boolean appIdFound = false;
    boolean instanceIdsMatched = false;
    for (SearchFilter filter : pageRequest.getFilters()) {
      assertThat(filter.getFieldValues()).isNotEmpty();
      if (filter.getFieldName().equals("appId")) {
        assertThat(filter.getFieldValues()[0]).isEqualTo(appId);
        appIdFound = true;
      } else if (filter.getFieldName().equals(ID_KEY)) {
        assertThat(filter.getFieldValues().length).isEqualTo(1);
        assertThat(filter.getFieldValues()[0]).isIn(instance1);
        instanceIdsMatched = true;
      }
    }
    assertThat(appIdFound).isTrue();
    assertThat(instanceIdsMatched).isTrue();
  }

  /**
   * Should fetch context element.
   */
  @Test
  public void shouldRenderExpressionFromInstanceElement() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setStateName("abc");
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    injector.injectMembers(context);

    Application app = Application.Builder.anApplication().withName("AppA").build();
    app = appService.save(app);

    Environment env = Environment.EnvironmentBuilder.anEnvironment().withName("DEV").build();
    env = environmentService.save(env);

    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());

    String timeStampId = std.getTimestampId();

    injector.injectMembers(std);
    context.pushContextElement(std);

    PageResponse<ServiceInstance> res = new PageResponse<>();
    ServiceInstance instance1 =
        Builder.aServiceInstance()
            .withUuid(UUIDGenerator.getUuid())
            .withHost(Host.HostBuilder.aHost().withHostName("host1").build())
            .withServiceTemplate(ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate()
                                     .withName("template")
                                     .withService(Service.Builder.aService().withUuid("uuid1").withName("svc1").build())
                                     .build())
            .build();
    List<ServiceInstance> instances = Lists.newArrayList(instance1);
    res.setResponse(instances);

    when(serviceInstanceService.list(any(PageRequest.class))).thenReturn(res);

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);
    processor.setServiceInstanceService(serviceInstanceService);

    List<InstanceElement> elements = processor.list();
    assertThat(elements).isNotNull();
    assertThat(elements.size()).isEqualTo(1);

    context.pushContextElement(elements.get(0));

    String expr =
        "$HOME/${env.name}/${app.name}/${service.name}/${serviceTemplate.name}/${host.name}/${timestampId}/runtime";
    String path = context.renderExpression(expr);
    assertThat(path).isEqualTo("$HOME/DEV/AppA/svc1/template/host1/" + timeStampId + "/runtime");
  }
}
