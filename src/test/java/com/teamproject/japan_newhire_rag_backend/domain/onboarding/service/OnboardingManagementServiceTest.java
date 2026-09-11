package com.teamproject.japan_newhire_rag_backend.domain.onboarding.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingCompletionRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingManagementItemResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingManagementPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingProgress;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingTask;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingAssignmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.EmployeeSummary;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;

@ExtendWith(MockitoExtension.class)
class OnboardingManagementServiceTest {

    private static final Long MANAGER_EMPLOYEE_ID = 1L;
    private static final Long TARGET_EMPLOYEE_ID = 10L;

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-10T00:00:00Z"),
            ZoneId.of("Asia/Tokyo"));

    @Mock
    private OnboardingProgressRepository progressRepository;

    @Mock
    private OrganizationQueryService organizationQueryService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private OnboardingManagementService service;

    @BeforeEach
    void setUp() {
        service = new OnboardingManagementService(
                progressRepository,
                organizationQueryService,
                currentUserProvider,
                FIXED_CLOCK);
    }

    @Test
    void managerGetsOnlyDirectNewHiresForAssignment() {
        stubCurrentUser(
                MANAGER_EMPLOYEE_ID,
                Set.of(RoleType.MANAGER));
        when(organizationQueryService.findManagedEmployeeIds(
                MANAGER_EMPLOYEE_ID))
                .thenReturn(List.of(TARGET_EMPLOYEE_ID, 20L));
        when(organizationQueryService
                .findValidNewHireEmployeeIds())
                .thenReturn(List.of(TARGET_EMPLOYEE_ID, 30L));
        when(organizationQueryService.findEmployeeSummaries(
                Set.of(TARGET_EMPLOYEE_ID)))
                .thenReturn(List.of(new EmployeeSummary(
                        TARGET_EMPLOYEE_ID,
                        "Employee A",
                        20L,
                        "Development",
                        30L,
                        "Junior")));

        var response = service.getAssignableEmployees();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).employeeId())
                .isEqualTo(TARGET_EMPLOYEE_ID);
        assertThat(response.get(0).employeeName())
                .isEqualTo("Employee A");
    }

    @Test
    void hrManagerCannotUseManagerAssignableEmployeeList() {
        stubCurrentUser(200L, Set.of(RoleType.HR_MANAGER));

        assertThatThrownBy(service::getAssignableEmployees)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(ErrorCode.FORBIDDEN));

        verifyNoInteractions(organizationQueryService);
    }

    @Test
    void hrManagerGetsAllEmployeesProgress() {
        stubCurrentUser(200L, Set.of(RoleType.HR_MANAGER));
        OnboardingProgress progress =
                progressForEmployee(TARGET_EMPLOYEE_ID);

        when(progressRepository.findAllWithAssignment(
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(progress),
                        PageRequest.of(0, 20),
                        1));
        stubEmployeeSummary();

        OnboardingManagementPageResponse response =
                service.getProgress(null, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).employeeId())
                .isEqualTo(TARGET_EMPLOYEE_ID);
        verify(organizationQueryService, never())
                .findManagedEmployeeIds(any());
        verify(organizationQueryService, never())
                .isManagedEmployee(any(), any());
    }

    @Test
    void managerGetsOnlyManagedEmployeesProgress() {
        stubCurrentUser(
                MANAGER_EMPLOYEE_ID,
                Set.of(RoleType.MANAGER));

        when(organizationQueryService.findManagedEmployeeIds(
                MANAGER_EMPLOYEE_ID))
                .thenReturn(List.of(TARGET_EMPLOYEE_ID));

        OnboardingProgress progress =
                progressForEmployee(TARGET_EMPLOYEE_ID);
        when(progressRepository
                .findAllByOnboardingAssignment_EmployeeIdIn(
                        eq(List.of(TARGET_EMPLOYEE_ID)),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(progress),
                        PageRequest.of(0, 20),
                        1));
        stubEmployeeSummary();

        OnboardingManagementPageResponse response =
                service.getProgress(null, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).employeeName())
                .isEqualTo("Employee A");
    }

    @Test
    void managerCannotGetEmployeeOutsideManagedScope() {
        stubCurrentUser(
                MANAGER_EMPLOYEE_ID,
                Set.of(RoleType.MANAGER));
        when(organizationQueryService.isManagedEmployee(
                MANAGER_EMPLOYEE_ID,
                999L))
                .thenReturn(false);

        assertThatThrownBy(() ->
                service.getProgress(999L, 0, 20))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(ErrorCode.FORBIDDEN));

        verifyNoInteractions(progressRepository);
    }

    @Test
    void regularEmployeeCannotManageProgress() {
        stubCurrentUser(
                TARGET_EMPLOYEE_ID,
                Set.of(RoleType.EMPLOYEE));

        assertThatThrownBy(() ->
                service.getProgress(null, 0, 20))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(ErrorCode.FORBIDDEN));

        verifyNoInteractions(progressRepository);
        verifyNoInteractions(organizationQueryService);
    }

    @Test
    void managerStartsManagedEmployeesProgress() {
        stubCurrentUser(
                MANAGER_EMPLOYEE_ID,
                Set.of(RoleType.MANAGER));
        when(organizationQueryService.isManagedEmployee(
                MANAGER_EMPLOYEE_ID,
                TARGET_EMPLOYEE_ID))
                .thenReturn(true);

        OnboardingProgress progress =
                progressForEmployee(TARGET_EMPLOYEE_ID);
        when(progressRepository
                .findByOnboardingAssignment_OnboardingAssignmentId(
                        100L))
                .thenReturn(Optional.of(progress));
        stubEmployeeSummary();

        OnboardingManagementItemResponse response =
                service.start(100L);

        assertThat(response.completionStatus())
                .isEqualTo(OnboardingCompletionStatus.IN_PROGRESS);
        assertThat(progress.getCompletionStatus())
                .isEqualTo(OnboardingCompletionStatus.IN_PROGRESS);
    }

    @Test
    void managerCannotStartEmployeeOutsideManagedScope() {
        stubCurrentUser(
                MANAGER_EMPLOYEE_ID,
                Set.of(RoleType.MANAGER));
        when(organizationQueryService.isManagedEmployee(
                MANAGER_EMPLOYEE_ID,
                TARGET_EMPLOYEE_ID))
                .thenReturn(false);

        OnboardingProgress progress =
                progressForEmployee(TARGET_EMPLOYEE_ID);
        when(progressRepository
                .findByOnboardingAssignment_OnboardingAssignmentId(
                        100L))
                .thenReturn(Optional.of(progress));

        assertThatThrownBy(() -> service.start(100L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(ErrorCode.FORBIDDEN));

        assertThat(progress.getCompletionStatus())
                .isEqualTo(OnboardingCompletionStatus.NOT_STARTED);
    }

    @Test
    void hrManagerCompletesStartedProgress() {
        stubCurrentUser(200L, Set.of(RoleType.HR_MANAGER));

        OnboardingProgress progress =
                progressForEmployee(TARGET_EMPLOYEE_ID);
        progress.start();

        when(progressRepository
                .findByOnboardingAssignment_OnboardingAssignmentId(
                        100L))
                .thenReturn(Optional.of(progress));
        stubEmployeeSummary();

        OnboardingManagementItemResponse response =
                service.complete(
                        100L,
                        new OnboardingCompletionRequest("Checked by HR"));

        assertThat(response.completionStatus())
                .isEqualTo(OnboardingCompletionStatus.COMPLETED);
        assertThat(response.assignmentStatus())
                .isEqualTo(OnboardingAssignmentStatus.COMPLETED);
        assertThat(response.completionNote())
                .isEqualTo("Checked by HR");
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void cannotCompleteProgressBeforeItIsStarted() {
        stubCurrentUser(200L, Set.of(RoleType.HR_MANAGER));

        OnboardingProgress progress =
                progressForEmployee(TARGET_EMPLOYEE_ID);
        when(progressRepository
                .findByOnboardingAssignment_OnboardingAssignmentId(
                        100L))
                .thenReturn(Optional.of(progress));

        assertThatThrownBy(() ->
                service.complete(
                        100L,
                        new OnboardingCompletionRequest(null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(ErrorCode.CONFLICT));

        assertThat(progress.getCompletionStatus())
                .isEqualTo(OnboardingCompletionStatus.NOT_STARTED);
    }

    private void stubCurrentUser(
            Long employeeId,
            Set<RoleType> roles
    ) {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(
                        1000L,
                        employeeId,
                        roles,
                        null,
                        null,
                        null));
    }

    private void stubEmployeeSummary() {
        when(organizationQueryService.findEmployeeSummaries(
                List.of(TARGET_EMPLOYEE_ID)))
                .thenReturn(List.of(new EmployeeSummary(
                        TARGET_EMPLOYEE_ID,
                        "Employee A",
                        20L,
                        "Development",
                        30L,
                        "Junior")));
    }

    private OnboardingProgress progressForEmployee(
            Long employeeId
    ) {
        OnboardingTask task = OnboardingTask.create(
                20L,
                "Submit documents",
                "Submit required onboarding documents",
                5,
                1000L);
        ReflectionTestUtils.setField(
                task,
                "onboardingTaskId",
                50L);

        OnboardingAssignment assignment =
                OnboardingAssignment.create(
                        task,
                        employeeId,
                        1000L,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 15));
        ReflectionTestUtils.setField(
                assignment,
                "onboardingAssignmentId",
                100L);

        return OnboardingProgress.create(assignment);
    }
}
