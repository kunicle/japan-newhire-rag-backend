package com.teamproject.japan_newhire_rag_backend.domain.onboarding.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingAssignableEmployeeResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingCompletionRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingManagementItemResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingManagementPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingProgress;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingAssignmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.EmployeeSummary;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;

@Service
public class OnboardingManagementService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Sort PROGRESS_SORT =
            Sort.by(Sort.Direction.DESC, "onboardingProgressId");

    private final OnboardingProgressRepository progressRepository;
    private final OrganizationQueryService organizationQueryService;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public OnboardingManagementService(
            OnboardingProgressRepository progressRepository,
            OrganizationQueryService organizationQueryService,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.progressRepository = progressRepository;
        this.organizationQueryService = organizationQueryService;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<OnboardingAssignableEmployeeResponse>
            getAssignableEmployees() {
        CurrentUserContext actor = getCurrentDirectManager();

        Set<Long> employeeIds = new LinkedHashSet<>(
                organizationQueryService.findManagedEmployeeIds(
                        actor.employeeId()));
        Set<Long> validNewHireEmployeeIds = Set.copyOf(
                organizationQueryService
                        .findValidNewHireEmployeeIds());
        employeeIds.retainAll(validNewHireEmployeeIds);

        if (employeeIds.isEmpty()) {
            return List.of();
        }

        return organizationQueryService
                .findEmployeeSummaries(employeeIds)
                .stream()
                .map(OnboardingAssignableEmployeeResponse::from)
                .sorted(java.util.Comparator.comparing(
                        OnboardingAssignableEmployeeResponse::employeeName))
                .toList();
    }

    @Transactional(readOnly = true)
    public OnboardingManagementPageResponse getProgress(
            Long employeeId,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        if (employeeId != null) {
            validateEmployeeId(employeeId);
        }

        CurrentUserContext actor = getCurrentManager();
        PageRequest pageRequest =
                PageRequest.of(page, size, PROGRESS_SORT);

        Page<OnboardingProgress> progressPage;

        if (isHrManager(actor)) {
            progressPage = employeeId == null
                    ? progressRepository.findAllWithAssignment(pageRequest)
                    : progressRepository
                            .findAllByOnboardingAssignment_EmployeeId(
                                    employeeId,
                                    pageRequest);
        } else if (employeeId != null) {
            requireManagedEmployee(actor, employeeId);
            progressPage = progressRepository
                    .findAllByOnboardingAssignment_EmployeeId(
                            employeeId,
                            pageRequest);
        } else {
            List<Long> managedEmployeeIds =
                    organizationQueryService.findManagedEmployeeIds(
                            actor.employeeId());

            if (managedEmployeeIds.isEmpty()) {
                return OnboardingManagementPageResponse.from(
                        Page.empty(pageRequest));
            }

            progressPage = progressRepository
                    .findAllByOnboardingAssignment_EmployeeIdIn(
                            managedEmployeeIds,
                            pageRequest);
        }

        return toPageResponse(progressPage);
    }

    @Transactional
    public OnboardingManagementItemResponse start(
            Long onboardingAssignmentId
    ) {
        validateAssignmentId(onboardingAssignmentId);

        CurrentUserContext actor = getCurrentManager();
        OnboardingProgress progress =
                findProgress(onboardingAssignmentId);

        requireActorScope(
                actor,
                progress.getOnboardingAssignment().getEmployeeId());
        requireActiveAssignment(progress.getOnboardingAssignment());

        if (progress.getCompletionStatus()
                == OnboardingCompletionStatus.COMPLETED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Completed onboarding progress cannot be started");
        }

        progress.start();
        return toItemResponse(progress);
    }

    @Transactional
    public OnboardingManagementItemResponse complete(
            Long onboardingAssignmentId,
            OnboardingCompletionRequest request
    ) {
        validateAssignmentId(onboardingAssignmentId);
        if (request == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Completion request is required");
        }

        CurrentUserContext actor = getCurrentManager();
        OnboardingProgress progress =
                findProgress(onboardingAssignmentId);
        OnboardingAssignment assignment =
                progress.getOnboardingAssignment();

        requireActorScope(actor, assignment.getEmployeeId());
        requireActiveAssignment(assignment);

        if (progress.getCompletionStatus()
                != OnboardingCompletionStatus.COMPLETED) {
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
        }

        return toItemResponse(progress);
    }

    private OnboardingManagementPageResponse toPageResponse(
            Page<OnboardingProgress> progressPage
    ) {
        if (progressPage.isEmpty()) {
            return OnboardingManagementPageResponse.from(
                    Page.empty(progressPage.getPageable()));
        }

        List<Long> employeeIds = progressPage.getContent()
                .stream()
                .map(progress -> progress.getOnboardingAssignment()
                        .getEmployeeId())
                .distinct()
                .toList();

        Map<Long, EmployeeSummary> employeesById =
                organizationQueryService
                        .findEmployeeSummaries(employeeIds)
                        .stream()
                        .collect(Collectors.toMap(
                                EmployeeSummary::employeeId,
                                Function.identity(),
                                (first, ignored) -> first));

        LocalDate today = LocalDate.now(clock);

        Page<OnboardingManagementItemResponse> responsePage =
                progressPage.map(progress -> {
                    Long employeeId = progress
                            .getOnboardingAssignment()
                            .getEmployeeId();
                    EmployeeSummary employee =
                            employeesById.get(employeeId);

                    if (employee == null) {
                        throw new BusinessException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "Employee information not found");
                    }

                    return OnboardingManagementItemResponse.from(
                            progress,
                            employee,
                            today);
                });

        return OnboardingManagementPageResponse.from(responsePage);
    }

    private OnboardingManagementItemResponse toItemResponse(
            OnboardingProgress progress
    ) {
        Long employeeId = progress.getOnboardingAssignment()
                .getEmployeeId();

        EmployeeSummary employee =
                organizationQueryService
                        .findEmployeeSummaries(List.of(employeeId))
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "Employee information not found"));

        return OnboardingManagementItemResponse.from(
                progress,
                employee,
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

    private CurrentUserContext getCurrentManager() {
        CurrentUserContext currentUser =
                currentUserProvider.getCurrentUser();

        if (currentUser == null
                || currentUser.appUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (isHrManager(currentUser)) {
            return currentUser;
        }

        if (currentUser.employeeId() == null
                || !currentUser.roles().contains(RoleType.MANAGER)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "HR manager or manager role is required");
        }

        return currentUser;
    }

    private CurrentUserContext getCurrentDirectManager() {
        CurrentUserContext currentUser =
                currentUserProvider.getCurrentUser();

        if (currentUser == null
                || currentUser.appUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (currentUser.employeeId() == null
                || !currentUser.roles().contains(RoleType.MANAGER)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Manager role is required");
        }

        return currentUser;
    }

    private boolean isHrManager(CurrentUserContext actor) {
        return actor.roles().contains(RoleType.HR_MANAGER);
    }

    private void requireActorScope(
            CurrentUserContext actor,
            Long employeeId
    ) {
        if (!isHrManager(actor)) {
            requireManagedEmployee(actor, employeeId);
        }
    }

    private void requireManagedEmployee(
            CurrentUserContext actor,
            Long employeeId
    ) {
        if (!organizationQueryService.isManagedEmployee(
                actor.employeeId(),
                employeeId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Employee is outside the manager's scope");
        }
    }

    private void requireActiveAssignment(
            OnboardingAssignment assignment
    ) {
        if (assignment.getAssignmentStatus()
                == OnboardingAssignmentStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Cancelled onboarding assignment cannot be changed");
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

    private void validateEmployeeId(Long employeeId) {
        if (employeeId <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Employee ID must be positive");
        }
    }

    private void validatePageRequest(
            int page,
            int size
    ) {
        if (page < 0
                || size < 1
                || size > MAX_PAGE_SIZE) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Page must be at least 0 and size must be between 1 and 100");
        }
    }
}
