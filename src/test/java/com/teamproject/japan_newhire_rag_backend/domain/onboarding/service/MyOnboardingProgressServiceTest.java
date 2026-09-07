package com.teamproject.japan_newhire_rag_backend.domain.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingProgress;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingTask;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@ExtendWith(MockitoExtension.class)
class MyOnboardingProgressServiceTest {

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
    void assignedEmployeeStartsOnboardingProgress() {
        stubCurrentEmployee(200L);

        OnboardingProgress progress =
                progressForEmployee(200L);

        when(progressRepository
                .findByOnboardingAssignment_OnboardingAssignmentId(
                        1L))
                .thenReturn(Optional.of(progress));

        MyOnboardingResponse response =
                service.start(1L);

        assertEquals(
                OnboardingCompletionStatus.IN_PROGRESS,
                progress.getCompletionStatus());
        assertEquals(
                OnboardingCompletionStatus.IN_PROGRESS,
                response.completionStatus());
    }

    @Test
    void repeatedStartIsIdempotent() {
        stubCurrentEmployee(200L);

        OnboardingProgress progress =
                progressForEmployee(200L);

        when(progressRepository
                .findByOnboardingAssignment_OnboardingAssignmentId(
                        1L))
                .thenReturn(Optional.of(progress));

        service.start(1L);
        service.start(1L);

        assertEquals(
                OnboardingCompletionStatus.IN_PROGRESS,
                progress.getCompletionStatus());
    }

    @Test
    void cancelledAssignmentCannotBeStarted() {
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
                        () -> service.start(1L));

        assertEquals(
                ErrorCode.CONFLICT,
                exception.getErrorCode());
        assertEquals(
                OnboardingCompletionStatus.NOT_STARTED,
                progress.getCompletionStatus());
    }

    @Test
    void employeeCannotStartAnotherEmployeesAssignment() {
        stubCurrentEmployee(999L);

        OnboardingProgress progress =
                progressForEmployee(200L);

        when(progressRepository
                .findByOnboardingAssignment_OnboardingAssignmentId(
                        1L))
                .thenReturn(Optional.of(progress));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.start(1L));

        assertEquals(
                ErrorCode.FORBIDDEN,
                exception.getErrorCode());
        assertEquals(
                OnboardingCompletionStatus.NOT_STARTED,
                progress.getCompletionStatus());
    }

    @Test
    void missingAssignmentReturnsNotFound() {
        stubCurrentEmployee(200L);

        when(progressRepository
                .findByOnboardingAssignment_OnboardingAssignmentId(
                        999L))
                .thenReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.start(999L));

        assertEquals(
                ErrorCode.RESOURCE_NOT_FOUND,
                exception.getErrorCode());
    }

    @Test
    void invalidAssignmentIdReturnsBadRequest() {
        stubCurrentEmployee(200L);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.start(0L));

        assertEquals(
                ErrorCode.INVALID_REQUEST,
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
                        LocalDate.of(2026, 8, 20),
                        LocalDate.of(2026, 8, 25));

        return OnboardingProgress.create(assignment);
    }
}