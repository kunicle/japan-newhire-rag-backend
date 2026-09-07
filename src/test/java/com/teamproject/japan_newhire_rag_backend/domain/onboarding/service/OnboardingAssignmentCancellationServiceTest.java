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
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingTask;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingAssignmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingAssignmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingTaskRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@ExtendWith(MockitoExtension.class)
class OnboardingAssignmentCancellationServiceTest {

    @Mock
    private OnboardingTaskRepository taskRepository;

    @Mock
    private OnboardingAssignmentRepository assignmentRepository;

    @Mock
    private OnboardingProgressRepository progressRepository;

    @Mock
    private OrganizationQueryService organizationQueryService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private OnboardingAssignmentService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-19T15:00:00Z"),
                ZoneId.of("Asia/Tokyo"));

        service = new OnboardingAssignmentService(
                taskRepository,
                assignmentRepository,
                progressRepository,
                organizationQueryService,
                currentUserProvider,
                clock);
    }

    @Test
    void cancelsAssignedOnboardingAssignment() {
        stubHrManager();

        OnboardingAssignment assignment = assignment();

        when(assignmentRepository.findById(1L))
                .thenReturn(Optional.of(assignment));

        service.cancel(1L);

        assertEquals(
                OnboardingAssignmentStatus.CANCELLED,
                assignment.getAssignmentStatus());
    }

    @Test
    void repeatedCancellationIsIdempotent() {
        stubHrManager();

        OnboardingAssignment assignment = assignment();

        when(assignmentRepository.findById(1L))
                .thenReturn(Optional.of(assignment));

        service.cancel(1L);
        service.cancel(1L);

        assertEquals(
                OnboardingAssignmentStatus.CANCELLED,
                assignment.getAssignmentStatus());
    }

    @Test
    void completedAssignmentCannotBeCancelled() {
        stubHrManager();

        OnboardingAssignment assignment = assignment();
        assignment.complete();

        when(assignmentRepository.findById(1L))
                .thenReturn(Optional.of(assignment));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.cancel(1L));

        assertEquals(
                ErrorCode.CONFLICT,
                exception.getErrorCode());
        assertEquals(
                "Completed assignment cannot be cancelled",
                exception.getMessage());
        assertEquals(
                OnboardingAssignmentStatus.COMPLETED,
                assignment.getAssignmentStatus());
    }

    @Test
    void missingAssignmentReturnsNotFound() {
        stubHrManager();

        when(assignmentRepository.findById(999L))
                .thenReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.cancel(999L));

        assertEquals(
                ErrorCode.RESOURCE_NOT_FOUND,
                exception.getErrorCode());
    }

    @Test
    void userWithoutHrManagerRoleCannotCancel() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(
                        100L,
                        200L,
                        Set.of(),
                        10L,
                        1,
                        EmployeeType.NEW_HIRE));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.cancel(1L));

        assertEquals(
                ErrorCode.FORBIDDEN,
                exception.getErrorCode());

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void invalidAssignmentIdReturnsBadRequest() {
        stubHrManager();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.cancel(0L));

        assertEquals(
                ErrorCode.INVALID_REQUEST,
                exception.getErrorCode());

        verifyNoInteractions(assignmentRepository);
    }

    private void stubHrManager() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(
                        100L,
                        200L,
                        Set.of(RoleType.HR_MANAGER),
                        10L,
                        1,
                        EmployeeType.NEW_HIRE));
    }

    private OnboardingAssignment assignment() {
        OnboardingTask task = OnboardingTask.create(
                10L,
                "Submit documents",
                "Submit required onboarding documents",
                5,
                100L);

        return OnboardingAssignment.create(
                task,
                200L,
                100L,
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 25));
    }
}