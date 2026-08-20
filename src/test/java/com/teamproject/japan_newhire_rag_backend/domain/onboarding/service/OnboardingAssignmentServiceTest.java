package com.teamproject.japan_newhire_rag_backend.domain.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingAssignmentCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingAssignmentCreateResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingProgress;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingTask;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingAssignmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingTaskRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@ExtendWith(MockitoExtension.class)
class OnboardingAssignmentServiceTest {

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

    @Captor
    private ArgumentCaptor<List<OnboardingAssignment>>
            assignmentListCaptor;

    @Captor
    private ArgumentCaptor<List<OnboardingProgress>>
            progressListCaptor;

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
    void assignCreatesAssignmentsAndProgressesExcludingDuplicates() {
        stubHrManager();

        OnboardingTask task = activeTask();
        OnboardingAssignment existingAssignment =
                OnboardingAssignment.create(
                        task,
                        2L,
                        100L,
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 6));

        when(taskRepository.findById(10L))
                .thenReturn(Optional.of(task));
        when(organizationQueryService
                .findValidNewHireEmployeeIds())
                .thenReturn(List.of(1L, 2L, 3L));
        when(assignmentRepository
                .findByOnboardingTask_OnboardingTaskIdAndEmployeeIdIn(
                        10L,
                        Set.of(1L, 2L, 3L)))
                .thenReturn(List.of(existingAssignment));
        when(assignmentRepository.saveAll(anyList()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        OnboardingAssignmentCreateResponse response =
                service.assign(
                        10L,
                        new OnboardingAssignmentCreateRequest(
                                List.of(1L, 1L, 2L, 3L)));

        assertEquals(10L, response.onboardingTaskId());
        assertEquals(4, response.requestedCount());
        assertEquals(2, response.successCount());
        assertEquals(2, response.duplicateCount());

        verify(assignmentRepository)
                .saveAll(assignmentListCaptor.capture());
        verify(progressRepository)
                .saveAll(progressListCaptor.capture());

        List<OnboardingAssignment> assignments =
                assignmentListCaptor.getValue();
        List<OnboardingProgress> progresses =
                progressListCaptor.getValue();

        assertEquals(
                List.of(1L, 3L),
                assignments.stream()
                        .map(OnboardingAssignment::getEmployeeId)
                        .toList());

        assertTrue(assignments.stream()
                .allMatch(assignment ->
                        assignment.getAssignedBy().equals(100L)));
        assertTrue(assignments.stream()
                .allMatch(assignment ->
                        assignment.getAssignedDate()
                                .equals(LocalDate.of(2026, 8, 20))));
        assertTrue(assignments.stream()
                .allMatch(assignment ->
                        assignment.getDueDate()
                                .equals(LocalDate.of(2026, 8, 25))));

        assertEquals(2, progresses.size());
        assertTrue(progresses.stream()
                .allMatch(progress ->
                        progress.getCompletionStatus()
                                == OnboardingCompletionStatus.NOT_STARTED));

        assertSame(
                assignments.get(0),
                progresses.get(0).getOnboardingAssignment());
        assertSame(
                assignments.get(1),
                progresses.get(1).getOnboardingAssignment());
    }

    @Test
    void assignRejectsInactiveTask() {
        stubHrManager();

        OnboardingTask task = activeTask();
        task.changeActivation(false);

        when(taskRepository.findById(10L))
                .thenReturn(Optional.of(task));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.assign(
                                10L,
                                new OnboardingAssignmentCreateRequest(
                                        List.of(1L))));

        assertEquals(
                ErrorCode.CONFLICT,
                exception.getErrorCode());

        verifyNoInteractions(
                organizationQueryService,
                assignmentRepository,
                progressRepository);
    }

    @Test
    void assignRejectsEmployeeWhoIsNotValidNewHire() {
        stubHrManager();

        when(taskRepository.findById(10L))
                .thenReturn(Optional.of(activeTask()));
        when(organizationQueryService
                .findValidNewHireEmployeeIds())
                .thenReturn(List.of(1L));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.assign(
                                10L,
                                new OnboardingAssignmentCreateRequest(
                                        List.of(1L, 999L))));

        assertEquals(
                ErrorCode.INVALID_REQUEST,
                exception.getErrorCode());

        verifyNoInteractions(
                assignmentRepository,
                progressRepository);
    }

    @Test
    void assignReturnsDuplicateCountWithoutCreatingWhenAllExist() {
        stubHrManager();

        OnboardingTask task = activeTask();
        OnboardingAssignment existingAssignment =
                OnboardingAssignment.create(
                        task,
                        1L,
                        100L,
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 6));

        when(taskRepository.findById(10L))
                .thenReturn(Optional.of(task));
        when(organizationQueryService
                .findValidNewHireEmployeeIds())
                .thenReturn(List.of(1L));
        when(assignmentRepository
                .findByOnboardingTask_OnboardingTaskIdAndEmployeeIdIn(
                        10L,
                        Set.of(1L)))
                .thenReturn(List.of(existingAssignment));

        OnboardingAssignmentCreateResponse response =
                service.assign(
                        10L,
                        new OnboardingAssignmentCreateRequest(
                                List.of(1L, 1L)));

        assertEquals(2, response.requestedCount());
        assertEquals(0, response.successCount());
        assertEquals(2, response.duplicateCount());

        verify(assignmentRepository, never())
                .saveAll(anyList());
        verify(progressRepository, never())
                .saveAll(anyList());
    }

    @Test
    void assignRejectsUserWithoutHrManagerRole() {
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
                        () -> service.assign(
                                10L,
                                new OnboardingAssignmentCreateRequest(
                                        List.of(1L))));

        assertEquals(
                ErrorCode.FORBIDDEN,
                exception.getErrorCode());

        verifyNoInteractions(
                taskRepository,
                organizationQueryService,
                assignmentRepository,
                progressRepository);
    }

    @Test
    void assignReturnsNotFoundWhenTaskDoesNotExist() {
        stubHrManager();

        when(taskRepository.findById(999L))
                .thenReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.assign(
                                999L,
                                new OnboardingAssignmentCreateRequest(
                                        List.of(1L))));

        assertEquals(
                ErrorCode.RESOURCE_NOT_FOUND,
                exception.getErrorCode());

        verifyNoInteractions(
                organizationQueryService,
                assignmentRepository,
                progressRepository);
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

    private OnboardingTask activeTask() {
        return OnboardingTask.create(
                10L,
                "Submit required documents",
                "Submit all required onboarding documents",
                5,
                100L);
    }
}