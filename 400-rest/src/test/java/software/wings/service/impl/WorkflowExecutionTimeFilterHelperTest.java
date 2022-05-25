package software.wings.service.impl;

import static io.harness.rule.OwnerRule.ABHINAV;

import static software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class WorkflowExecutionTimeFilterHelperTest {
  private final Long THREE_MONTHS_MILLIS = 7889400000L;
  private final Long FOUR_MONTHS_MILLIS = 10519200000L;
  private final Long TWO_MONTHS_MILLIS = 5259600000L;
  private final Long SIX_MONTHS_MILLIS = 15778800000L;

  @InjectMocks WorkflowExecutionTimeFilterHelper workflowExecutionTimeFilterHelper;

  @Before
  public void runBeforeTest() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testWorkflowExecutionFilter() {
    final PageRequest pageRequest =
        PageRequest.PageRequestBuilder.aPageRequest()
            .addFilter(SearchFilter.builder()
                           .op(SearchFilter.Operator.GT)
                           .fieldValues(new Object[] {System.currentTimeMillis() - FOUR_MONTHS_MILLIS})
                           .fieldName(WorkflowExecutionKeys.createdAt)
                           .build())
            .build();
    assertThatThrownBy(() -> workflowExecutionTimeFilterHelper.updatePageRequestForTimeFilter(pageRequest));

    final PageRequest pageRequest1 =
        PageRequest.PageRequestBuilder.aPageRequest()
            .addFilter(SearchFilter.builder()
                           .op(SearchFilter.Operator.GT)
                           .fieldValues(new Object[] {System.currentTimeMillis() - THREE_MONTHS_MILLIS})
                           .fieldName(WorkflowExecutionKeys.createdAt)
                           .build())
            .build();
    workflowExecutionTimeFilterHelper.updatePageRequestForTimeFilter(pageRequest1);

    final PageRequest pageRequest2 =
        PageRequest.PageRequestBuilder.aPageRequest()
            .addFilter(SearchFilter.builder()
                           .op(SearchFilter.Operator.GT)
                           .fieldValues(new Object[] {System.currentTimeMillis() - SIX_MONTHS_MILLIS})
                           .fieldName(WorkflowExecutionKeys.createdAt)
                           .build())
            .build();
    assertThatThrownBy(() -> workflowExecutionTimeFilterHelper.updatePageRequestForTimeFilter(pageRequest2));

    final PageRequest pageRequest3 =
        PageRequest.PageRequestBuilder.aPageRequest()
            .addFilter(SearchFilter.builder()
                           .op(SearchFilter.Operator.GT)
                           .fieldValues(new Object[] {System.currentTimeMillis() - SIX_MONTHS_MILLIS})
                           .fieldName(WorkflowExecutionKeys.createdAt)
                           .build())
            .addFilter(SearchFilter.builder()
                           .op(SearchFilter.Operator.LT)
                           .fieldValues(new Object[] {System.currentTimeMillis() - FOUR_MONTHS_MILLIS})
                           .fieldName(WorkflowExecutionKeys.createdAt)
                           .build())
            .build();
    workflowExecutionTimeFilterHelper.updatePageRequestForTimeFilter(pageRequest3);

    final PageRequest pageRequest4 =
        PageRequest.PageRequestBuilder.aPageRequest()
            .addFilter(SearchFilter.builder()
                           .fieldName(WorkflowExecutionKeys.createdAt)
                           .op(SearchFilter.Operator.GT)
                           .fieldValues(new Object[] {System.currentTimeMillis() - SIX_MONTHS_MILLIS})
                           .build())
            .addFilter(SearchFilter.builder()
                           .fieldName(WorkflowExecutionKeys.createdAt)
                           .op(SearchFilter.Operator.LT)
                           .fieldValues(new Object[] {System.currentTimeMillis()})
                           .build())
            .build();
    assertThatThrownBy(() -> workflowExecutionTimeFilterHelper.updatePageRequestForTimeFilter(pageRequest4));
  }
}