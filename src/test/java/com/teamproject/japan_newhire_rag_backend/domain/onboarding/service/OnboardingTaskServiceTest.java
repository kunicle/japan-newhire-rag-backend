package com.teamproject.japan_newhire_rag_backend.domain.onboarding.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskActivationRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingTask;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingTaskRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@ExtendWith(MockitoExtension.class)
class OnboardingTaskServiceTest {

    @Mock
    private OnboardingTaskRepository onboardingTaskRepository;

    @Mock
    private OrganizationQueryService organizationQueryService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private OnboardingTaskService onboardingTaskService;

    @BeforeEach
    void setUp() {
        onboardingTaskService = new OnboardingTaskService(
                onboardingTaskRepository,
                organizationQueryService,
                currentUserProvider);
    }

    @Test
    void hrManagerCreatesActiveTask() {
        stubHrManager();
        when(organizationQueryService
                .isValidDepartment(10L))
                .thenReturn(true);
        when(onboardingTaskRepository.save(any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        OnboardingTaskResponse response =
                onboardingTaskService.createTask(
                        new OnboardingTaskCreateRequest(
                                10L,
                                "Submit documents",
                                "Submit required documents.",
                                7));

        assertThat(response.departmentId()).isEqualTo(10L);
        assertThat(response.taskTitle())
                .isEqualTo("Submit documents");
        assertThat(response.defaultDueDays()).isEqualTo(7);
        assertThat(response.active()).isTrue();
        assertThat(response.createdBy()).isEqualTo(100L);

        verify(onboardingTaskRepository).save(any());
    }

    @Test
    void createTaskRejectsInvalidDepartment() {
        stubHrManager();
        when(organizationQueryService
                .isValidDepartment(999L))
                .thenReturn(false);

        assertThatThrownBy(() ->
                onboardingTaskService.createTask(
                        new OnboardingTaskCreateRequest(
                                999L,
                                "Submit documents",
                                "Submit required documents.",
                                7)))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception)
                                .getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST));

        verify(onboardingTaskRepository, never())
                .save(any());
    }

    @Test
    void nonHrManagerCannotCreateTask() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(
                        100L,
                        200L,
                        Set.of(RoleType.EMPLOYEE),
                        10L,
                        1,
                        EmployeeType.GENERAL));

        assertThatThrownBy(() ->
                onboardingTaskService.createTask(
                        new OnboardingTaskCreateRequest(
                                10L,
                                "Submit documents",
                                "Submit required documents.",
                                7)))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception)
                                .getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));

        verify(onboardingTaskRepository, never())
                .save(any());
    }

    @Test
    void unauthenticatedUserCannotCreateTask() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(null);

        assertThatThrownBy(() ->
                onboardingTaskService.createTask(
                        new OnboardingTaskCreateRequest(
                                10L,
                                "Submit documents",
                                "Submit required documents.",
                                7)))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception)
                                .getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void hrManagerGetsPagedTasksIncludingInactiveTasks() {
        stubHrManager();
        OnboardingTask activeTask = createTask();
        OnboardingTask inactiveTask = createTask();
        inactiveTask.changeActivation(false);
        when(onboardingTaskRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(activeTask, inactiveTask),
                        PageRequest.of(0, 20),
                        2));

        OnboardingTaskPageResponse response =
                onboardingTaskService.getTasks(0, 20);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content())
                .extracting(OnboardingTaskResponse::active)
                .containsExactly(true, false);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(2);
    }

    @Test
    void taskListUsesNewestFirstStableSort() {
        stubHrManager();
        when(onboardingTaskRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        onboardingTaskService.getTasks(2, 10);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(onboardingTaskRepository)
                .findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()
                .getOrderFor("createdAt")).isNotNull();
        assertThat(pageable.getSort()
                .getOrderFor("createdAt").isDescending())
                .isTrue();
        assertThat(pageable.getSort()
                .getOrderFor("onboardingTaskId"))
                .isNotNull();
        assertThat(pageable.getSort()
                .getOrderFor("onboardingTaskId")
                .isDescending()).isTrue();
    }

    @Test
    void invalidTaskListPageIsRejected() {
        stubHrManager();

        assertThatThrownBy(() ->
                onboardingTaskService.getTasks(-1, 20))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception)
                                .getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST));

        verify(onboardingTaskRepository, never())
                .findAll(any(Pageable.class));
    }

    @Test
    void invalidTaskListSizeIsRejected() {
        stubHrManager();

        assertThatThrownBy(() ->
                onboardingTaskService.getTasks(0, 101))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception)
                                .getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void hrManagerGetsTaskDetail() {
        stubHrManager();
        OnboardingTask task = createTask();
        when(onboardingTaskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        OnboardingTaskResponse response =
                onboardingTaskService.getTask(1L);

        assertThat(response.taskTitle())
                .isEqualTo("Original title");
        assertThat(response.active()).isTrue();
    }

    @Test
    void missingTaskDetailReturnsNotFound() {
        stubHrManager();
        when(onboardingTaskRepository.findById(404L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                onboardingTaskService.getTask(404L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception)
                                .getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void unauthenticatedUserCannotGetTaskList() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(null);

        assertThatThrownBy(() ->
                onboardingTaskService.getTasks(0, 20))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception)
                                .getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void nonHrManagerCannotGetTaskDetail() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(
                        100L,
                        200L,
                        Set.of(RoleType.EMPLOYEE),
                        10L,
                        1,
                        EmployeeType.GENERAL));

        assertThatThrownBy(() ->
                onboardingTaskService.getTask(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception)
                                .getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void hrManagerUpdatesTask() {
        stubHrManager();
        OnboardingTask task = createTask();

        when(organizationQueryService
                .isValidDepartment(20L))
                .thenReturn(true);
        when(onboardingTaskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        OnboardingTaskResponse response =
                onboardingTaskService.updateTask(
                        1L,
                        new OnboardingTaskUpdateRequest(
                                20L,
                                "Updated title",
                                "Updated description",
                                14));

        assertThat(response.departmentId()).isEqualTo(20L);
        assertThat(response.taskTitle())
                .isEqualTo("Updated title");
        assertThat(response.taskDescription())
                .isEqualTo("Updated description");
        assertThat(response.defaultDueDays()).isEqualTo(14);
        assertThat(response.createdBy()).isEqualTo(100L);
    }

    @Test
    void updateTaskReturnsNotFoundForMissingTask() {
        stubHrManager();
        when(organizationQueryService
                .isValidDepartment(10L))
                .thenReturn(true);
        when(onboardingTaskRepository.findById(404L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                onboardingTaskService.updateTask(
                        404L,
                        new OnboardingTaskUpdateRequest(
                                10L,
                                "Updated title",
                                "Updated description",
                                7)))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception)
                                .getErrorCode())
                        .isEqualTo(
                                ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void changesActivationIdempotently() {
        stubHrManager();
        OnboardingTask task = createTask();

        when(onboardingTaskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        OnboardingTaskResponse firstResponse =
                onboardingTaskService.changeActivation(
                        1L,
                        new OnboardingTaskActivationRequest(
                                false));

        OnboardingTaskResponse secondResponse =
                onboardingTaskService.changeActivation(
                        1L,
                        new OnboardingTaskActivationRequest(
                                false));

        assertThat(firstResponse.active()).isFalse();
        assertThat(secondResponse.active()).isFalse();
        assertThat(task.isActive()).isFalse();
    }

    private void stubHrManager() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(
                        100L,
                        200L,
                        Set.of(RoleType.HR_MANAGER),
                        10L,
                        1,
                        EmployeeType.GENERAL));
    }

    private OnboardingTask createTask() {
        return OnboardingTask.create(
                10L,
                "Original title",
                "Original description",
                7,
                100L);
    }
}
