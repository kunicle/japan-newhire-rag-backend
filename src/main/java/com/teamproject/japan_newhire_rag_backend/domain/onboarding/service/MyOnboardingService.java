package com.teamproject.japan_newhire_rag_backend.domain.onboarding.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.MyOnboardingResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingProgress;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingAssignmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingCompletionRequest;

@Service
public class MyOnboardingService {

    private final OnboardingProgressRepository progressRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public MyOnboardingService(
            OnboardingProgressRepository progressRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.progressRepository = progressRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MyOnboardingResponse> getMyOnboarding() {
        CurrentUserContext currentUser
                = validateCurrentEmployee();

        LocalDate today = LocalDate.now(clock);

        return progressRepository
                .findByOnboardingAssignment_EmployeeIdOrderByOnboardingAssignment_DueDateAsc(
                        currentUser.employeeId())
                .stream()
                .map(progress
                        -> MyOnboardingResponse.from(
                        progress,
                        today))
                .toList();
    }

    @Transactional
    public MyOnboardingResponse start(
            Long onboardingAssignmentId
    ) {
        CurrentUserContext currentUser
                = validateCurrentEmployee();
        validateAssignmentId(onboardingAssignmentId);

        OnboardingProgress progress
                = findProgress(onboardingAssignmentId);
        OnboardingAssignment assignment
                = progress.getOnboardingAssignment();

        validateOwner(
                currentUser.employeeId(),
                assignment);
        validateStartableAssignment(assignment);

        progress.start();

        return MyOnboardingResponse.from(
                progress,
                LocalDate.now(clock));
    }

    @Transactional
    public MyOnboardingResponse complete(
            Long onboardingAssignmentId,
            OnboardingCompletionRequest request
    ) {
        CurrentUserContext currentUser
                = validateCurrentEmployee();
        validateAssignmentId(onboardingAssignmentId);
        validateCompletionRequest(request);

        OnboardingProgress progress
                = findProgress(onboardingAssignmentId);
        OnboardingAssignment assignment
                = progress.getOnboardingAssignment();

        validateOwner(
                currentUser.employeeId(),
                assignment);
        validateCompletableAssignment(assignment);

        try {
            progress.complete(
                    request.completionNote(),
                    LocalDateTime.now(clock));
            assignment.complete();
        } catch (IllegalStateException exception) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    exception.getMessage());
        }

        return MyOnboardingResponse.from(
                progress,
                LocalDate.now(clock));
    }

    private OnboardingProgress findProgress(
            Long onboardingAssignmentId
    ) {
        return progressRepository
                .findByOnboardingAssignment_OnboardingAssignmentId(
                        onboardingAssignmentId)
                .orElseThrow(() -> new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "Onboarding assignment not found"));
    }

    private void validateOwner(
            Long currentEmployeeId,
            OnboardingAssignment assignment
    ) {
        if (!currentEmployeeId.equals(
                assignment.getEmployeeId())) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Only the assigned employee can update onboarding progress");
        }
    }

    private void validateStartableAssignment(
            OnboardingAssignment assignment
    ) {
        if (assignment.getAssignmentStatus()
                == OnboardingAssignmentStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Cancelled onboarding assignment cannot be started");
        }
    }

    private void validateCompletableAssignment(
            OnboardingAssignment assignment
    ) {
        if (assignment.getAssignmentStatus()
                == OnboardingAssignmentStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Cancelled onboarding assignment cannot be completed");
        }
    }

    private void validateCompletionRequest(
            OnboardingCompletionRequest request
    ) {
        if (request == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Completion request is required");
        }
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

    private CurrentUserContext validateCurrentEmployee() {
        CurrentUserContext currentUser
                = currentUserProvider.getCurrentUser();

        if (currentUser == null
                || currentUser.appUserId() == null
                || currentUser.employeeId() == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED);
        }

        return currentUser;
    }
}
