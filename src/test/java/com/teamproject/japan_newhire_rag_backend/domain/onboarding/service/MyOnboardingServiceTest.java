package com.teamproject.japan_newhire_rag_backend.domain.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.MyOnboardingResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingProgress;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingTask;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingAssignmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@ExtendWith(MockitoExtension.class)
class MyOnboardingServiceTest {

    @Mock
    private OnboardingProgressRepository progressRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private MyOnboardingService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-19T15:00:00Z"),
                ZoneId.of("Asia/Tokyo"));

        service = new MyOnboardingService(
                progressRepository,
                currentUserProvider,
                clock);
    }

    @Test
    void returnsOnlyCurrentEmployeesOnboardingWithOverdueCalculation() {
        stubCurrentEmployee(200L);

        OnboardingProgress overdue = progress(
                1L,
                LocalDate.of(2026, 8, 19),
                OnboardingAssignmentStatus.ASSIGNED,
                OnboardingCompletionStatus.NOT_STARTED);

        OnboardingProgress dueToday = progress(
                2L,
                LocalDate.of(2026, 8, 20),
                OnboardingAssignmentStatus.ASSIGNED,
                OnboardingCompletionStatus.IN_PROGRESS);

        OnboardingProgress completed = progress(
                3L,
                LocalDate.of(2026, 8, 10),
                OnboardingAssignmentStatus.COMPLETED,
                OnboardingCompletionStatus.COMPLETED);

        OnboardingProgress cancelled = progress(
                4L,
                LocalDate.of(2026, 8, 10),
                OnboardingAssignmentStatus.CANCELLED,
                OnboardingCompletionStatus.NOT_STARTED);

        when(progressRepository
                .findByOnboardingAssignment_EmployeeIdOrderByOnboardingAssignment_DueDateAsc(
                        200L))
                .thenReturn(List.of(
                        completed,
                        cancelled,
                        overdue,
                        dueToday));

        List<MyOnboardingResponse> responses =
                service.getMyOnboarding();

        assertEquals(4, responses.size());

        assertFalse(responses.get(0).overdue());
        assertFalse(responses.get(1).overdue());
        assertTrue(responses.get(2).overdue());
        assertFalse(responses.get(3).overdue());

        assertEquals(
                OnboardingCompletionStatus.NOT_STARTED,
                responses.get(2).completionStatus());
        assertEquals(
                LocalDate.of(2026, 8, 19),
                responses.get(2).dueDate());

        verify(progressRepository)
                .findByOnboardingAssignment_EmployeeIdOrderByOnboardingAssignment_DueDateAsc(
                        200L);
    }

    @Test
    void returnsEmptyListWhenEmployeeHasNoOnboarding() {
        stubCurrentEmployee(200L);

        when(progressRepository
                .findByOnboardingAssignment_EmployeeIdOrderByOnboardingAssignment_DueDateAsc(
                        200L))
                .thenReturn(List.of());

        List<MyOnboardingResponse> responses =
                service.getMyOnboarding();

        assertTrue(responses.isEmpty());
    }

    @Test
    void unauthenticatedUserCannotGetMyOnboarding() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(null);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        service::getMyOnboarding);

        assertEquals(
                ErrorCode.UNAUTHORIZED,
                exception.getErrorCode());

        verifyNoInteractions(progressRepository);
    }

    @Test
    void userWithoutEmployeeIdCannotGetMyOnboarding() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(
                        100L,
                        null,
                        Set.of(),
                        10L,
                        1,
                        EmployeeType.NEW_HIRE));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        service::getMyOnboarding);

        assertEquals(
                ErrorCode.UNAUTHORIZED,
                exception.getErrorCode());

        verifyNoInteractions(progressRepository);
    }

    private void stubCurrentEmployee(Long employeeId) {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(
                        100L,
                        employeeId,
                        Set.of(),
                        10L,
                        1,
                        EmployeeType.NEW_HIRE));
    }

    private OnboardingProgress progress(
            Long assignmentId,
            LocalDate dueDate,
            OnboardingAssignmentStatus assignmentStatus,
            OnboardingCompletionStatus completionStatus
    ) {
        OnboardingTask task =
                mock(OnboardingTask.class);
        OnboardingAssignment assignment =
                mock(OnboardingAssignment.class);
        OnboardingProgress progress =
                mock(OnboardingProgress.class);

        when(progress.getOnboardingAssignment())
                .thenReturn(assignment);
        when(assignment.getOnboardingTask())
                .thenReturn(task);

        when(assignment.getOnboardingAssignmentId())
                .thenReturn(assignmentId);
        when(task.getOnboardingTaskId())
                .thenReturn(assignmentId * 10);
        when(task.getDepartmentId())
                .thenReturn(10L);
        when(task.getTaskTitle())
                .thenReturn("Task " + assignmentId);
        when(task.getTaskDescription())
                .thenReturn("Description " + assignmentId);

        when(assignment.getAssignedDate())
                .thenReturn(LocalDate.of(2026, 8, 1));
        when(assignment.getDueDate())
                .thenReturn(dueDate);
        when(assignment.getAssignmentStatus())
                .thenReturn(assignmentStatus);
        when(progress.getCompletionStatus())
                .thenReturn(completionStatus);

        return progress;
    }
}