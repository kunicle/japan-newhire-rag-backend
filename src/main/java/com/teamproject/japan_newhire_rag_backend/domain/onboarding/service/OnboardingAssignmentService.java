package com.teamproject.japan_newhire_rag_backend.domain.onboarding.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingAssignmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingTaskRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;

@Service
public class OnboardingAssignmentService {

    private final OnboardingTaskRepository taskRepository;
    private final OnboardingAssignmentRepository assignmentRepository;
    private final OnboardingProgressRepository progressRepository;
    private final OrganizationQueryService organizationQueryService;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public OnboardingAssignmentService(
            OnboardingTaskRepository taskRepository,
            OnboardingAssignmentRepository assignmentRepository,
            OnboardingProgressRepository progressRepository,
            OrganizationQueryService organizationQueryService,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.taskRepository = taskRepository;
        this.assignmentRepository = assignmentRepository;
        this.progressRepository = progressRepository;
        this.organizationQueryService = organizationQueryService;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional
    public OnboardingAssignmentCreateResponse assign(
            Long onboardingTaskId,
            OnboardingAssignmentCreateRequest request
    ) {
        CurrentUserContext currentUser =
                validateCurrentHrManager();
        validateTaskId(onboardingTaskId);
        validateRequest(request);

        OnboardingTask task = findTask(onboardingTaskId);
        validateActiveTask(task);

        List<Long> requestedEmployeeIds =
                request.employeeIds();
        int requestedCount = requestedEmployeeIds.size();

        Set<Long> normalizedEmployeeIds =
                new LinkedHashSet<>(requestedEmployeeIds);

        validateNewHireEmployees(normalizedEmployeeIds);

        Set<Long> existingEmployeeIds =
                assignmentRepository
                        .findByOnboardingTask_OnboardingTaskIdAndEmployeeIdIn(
                                onboardingTaskId,
                                normalizedEmployeeIds)
                        .stream()
                        .map(OnboardingAssignment::getEmployeeId)
                        .collect(java.util.stream.Collectors.toSet());

        List<Long> assignableEmployeeIds =
                normalizedEmployeeIds.stream()
                        .filter(employeeId ->
                                !existingEmployeeIds.contains(employeeId))
                        .toList();

        if (!assignableEmployeeIds.isEmpty()) {
            createAssignments(
                    task,
                    assignableEmployeeIds,
                    currentUser.appUserId());
        }

        int successCount = assignableEmployeeIds.size();
        int duplicateCount = requestedCount - successCount;

        return new OnboardingAssignmentCreateResponse(
                onboardingTaskId,
                requestedCount,
                successCount,
                duplicateCount);
    }

    @Transactional
    public void cancel(Long onboardingAssignmentId) {
        validateCurrentHrManager();
        validateAssignmentId(onboardingAssignmentId);

        OnboardingAssignment assignment =
                findAssignment(onboardingAssignmentId);

        try {
                assignment.cancel();
        } catch (IllegalStateException exception) {
                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        exception.getMessage());
        }
    }

    private void createAssignments(
            OnboardingTask task,
            List<Long> employeeIds,
            Long assignedBy
    ) {
        LocalDate assignedDate = LocalDate.now(clock);
        LocalDate dueDate =
                assignedDate.plusDays(task.getDefaultDueDays());

        List<OnboardingAssignment> assignments =
                employeeIds.stream()
                        .map(employeeId ->
                                OnboardingAssignment.create(
                                        task,
                                        employeeId,
                                        assignedBy,
                                        assignedDate,
                                        dueDate))
                        .toList();

        List<OnboardingAssignment> savedAssignments =
                assignmentRepository.saveAll(assignments);

        List<OnboardingProgress> progresses =
                savedAssignments.stream()
                        .map(OnboardingProgress::create)
                        .toList();

        progressRepository.saveAll(progresses);
    }

    private void validateNewHireEmployees(
            Set<Long> requestedEmployeeIds
    ) {
        Set<Long> validNewHireEmployeeIds =
                Set.copyOf(
                        organizationQueryService
                                .findValidNewHireEmployeeIds());

        if (!validNewHireEmployeeIds
                .containsAll(requestedEmployeeIds)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Only valid new hire employees can be assigned");
        }
    }

    private OnboardingAssignment findAssignment(
        Long onboardingAssignmentId
    ) {
        return assignmentRepository
                .findById(onboardingAssignmentId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Onboarding assignment not found"));
        }

        private OnboardingTask findTask(Long onboardingTaskId) {
                return taskRepository.findById(onboardingTaskId)
                        .orElseThrow(() -> new BusinessException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "Onboarding task not found"));
    }

    private void validateActiveTask(OnboardingTask task) {
        if (!task.isActive()) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Inactive onboarding task cannot be assigned");
        }
    }

    private CurrentUserContext validateCurrentHrManager() {
        CurrentUserContext currentUser =
                currentUserProvider.getCurrentUser();

        if (currentUser == null
                || currentUser.appUserId() == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED);
        }

        if (!currentUser.roles()
                .contains(RoleType.HR_MANAGER)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN);
        }

        return currentUser;
    }

    private void validateAssignmentId(
                Long onboardingAssignmentId
    ) {
        if (onboardingAssignmentId == null
                || onboardingAssignmentId <= 0) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "Onboarding assignment ID must be positive");
        }
    }

    private void validateTaskId(Long onboardingTaskId) {
        if (onboardingTaskId == null
                || onboardingTaskId <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Onboarding task ID must be positive");
        }
    }

    private void validateRequest(
            OnboardingAssignmentCreateRequest request
    ) {
        if (request == null
                || request.employeeIds() == null
                || request.employeeIds().isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Employee IDs are required");
        }

        boolean hasInvalidEmployeeId =
                request.employeeIds().stream()
                        .anyMatch(employeeId ->
                                employeeId == null
                                        || employeeId <= 0);

        if (hasInvalidEmployeeId) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Employee IDs must be positive");
        }
    }
}