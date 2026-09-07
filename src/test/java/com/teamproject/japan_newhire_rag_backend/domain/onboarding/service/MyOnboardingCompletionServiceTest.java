package com.teamproject.japan_newhire_rag_backend.domain.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
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
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingCompletionRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingProgress;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingTask;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingAssignmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@ExtendWith(MockitoExtension.class)
class MyOnboardingCompletionServiceTest {

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
    void completesStartedOnboardingProgress() {
        stubCurrentEmployee(200L);

        OnboardingProgress progress =
                progressForEmployee(200L);
        progress.start();

        when(progressRepository
                .findByOnboardingAssignment_OnboardingAssignmentId(
                        1L))
                .thenReturn(Optional.of(progress));

        MyOnboardingResponse response =
                service.complete(
                        1L,
                        new OnboardingCompletionRequest(
                                "All documents submitted"));

        assertEquals(
                OnboardingCompletionStatus.COMPLETED,
                progress.getCompletionStatus());
        assertEquals(
                OnboardingAssignmentStatus.COMPLETED,
                progress.getOnboardingAssignment()
                        .getAssignmentStatus());
        assertEquals(
                "All documents submitted",
                progress.getCompletionNote());
        assertEquals(
                LocalDateTime.of(
                        2026, 8, 20, 0, 0),
                progress.getCompletedAt());

        assertEquals(
                OnboardingCompletionStatus.COMPLETED,
                response.completionStatus());
        assertEquals(
                OnboardingAssignmentStatus.COMPLETED,
                response.assignmentStatus());
        assertFalse(response.overdue());
    }

    @Test
    void repeatedCompletionPreservesOriginalNoteAndTime() {
        stubCurrentEmployee(200L);

        OnboardingProgress progress =
                progressForEmployee(200L);
        progress.start();

        when(progressRepository
                .findByOnboardingAssignment_OnboardingAssignmentId(
                        1L))
                .thenReturn(Optional.of(progress));

        service.complete(
                1L,
                new OnboardingCompletionRequest(
                        "Original note"));

        LocalDateTime originalCompletedAt =
                progress.getCompletedAt();

        service.complete(
                1L,
                new OnboardingCompletionRequest(
                        "Changed note"));

        assertEquals(
                "Original note",
                progress.getCompletionNote());
        assertEquals(
                originalCompletedAt,
                progress.getCompletedAt());
        assertEquals(
                OnboardingAssignmentStatus.COMPLETED,
                progress.getOnboardingAssignment()
                        .getAssignmentStatus());
    }

    @Test
    void notStartedProgressCannotBeCompleted() {
        stubCurrentEmployee(200L);

        OnboardingProgress progress =
                progressForEmployee(200L);

        when(progressRepository
                .findByOnboardingAssignment_OnboardingAssignmentId(
                        1L))
                .thenReturn(Optional.of(progress));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.complete(
                                1L,
                                new OnboardingCompletionRequest(
                                        "Completed")));

        assertEquals(
                ErrorCode.CONFLICT,
                exception.getErrorCode());
        assertEquals(
                OnboardingCompletionStatus.NOT_STARTED,
                progress.getCompletionStatus());
        assertEquals(
                OnboardingAssignmentStatus.ASSIGNED,
                progress.getOnboardingAssignment()
                        .getAssignmentStatus());
    }

    @Test
    void cancelledAssignmentCannotBeCompleted() {
        stubCurrentEmployee(200L);

        OnboardingProgress progress =
                progressForEmployee(200L);
        progress.getOnboardingAssignment().cancel();

        when(progressRepository
                .findByOnboardingAssignment_OnboardingAssignmentId(
                        1L))
                .thenReturn(Optional.of(progress));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.complete(
                                1L,
                                new OnboardingCompletionRequest(
                                        "Completed")));

        assertEquals(
                ErrorCode.CONFLICT,
                exception.getErrorCode());
        assertEquals(
                OnboardingCompletionStatus.NOT_STARTED,
                progress.getCompletionStatus());
    }

    @Test
    void employeeCannotCompleteAnotherEmployeesAssignment() {
        stubCurrentEmployee(999L);

        OnboardingProgress progress =
                progressForEmployee(200L);
        progress.start();

        when(progressRepository
                .findByOnboardingAssignment_OnboardingAssignmentId(
                        1L))
                .thenReturn(Optional.of(progress));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.complete(
                                1L,
                                new OnboardingCompletionRequest(
                                        "Completed")));

        assertEquals(
                ErrorCode.FORBIDDEN,
                exception.getErrorCode());
        assertEquals(
                OnboardingCompletionStatus.IN_PROGRESS,
                progress.getCompletionStatus());
    }

    @Test
    void nullCompletionRequestReturnsBadRequest() {
        stubCurrentEmployee(200L);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.complete(
                                1L,
                                null));

        assertEquals(
                ErrorCode.INVALID_REQUEST,
                exception.getErrorCode());
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

    private OnboardingProgress progressForEmployee(
            Long employeeId
    ) {
        OnboardingTask task = OnboardingTask.create(
                10L,
                "Submit documents",
                "Submit required onboarding documents",
                5,
                100L);

        OnboardingAssignment assignment =
                OnboardingAssignment.create(
                        task,
                        employeeId,
                        100L,
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 10));

        return OnboardingProgress.create(assignment);
    }
}