package com.teamproject.japan_newhire_rag_backend.domain.onboarding.service;

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
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskActivationRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingTask;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingTaskRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;

@Service
public class OnboardingTaskService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort TASK_LIST_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("onboardingTaskId"));

    private final OnboardingTaskRepository onboardingTaskRepository;
    private final OrganizationQueryService organizationQueryService;
    private final CurrentUserProvider currentUserProvider;

    public OnboardingTaskService(
            OnboardingTaskRepository onboardingTaskRepository,
            OrganizationQueryService organizationQueryService,
            CurrentUserProvider currentUserProvider
    ) {
        this.onboardingTaskRepository = onboardingTaskRepository;
        this.organizationQueryService = organizationQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public OnboardingTaskResponse createTask(
            OnboardingTaskCreateRequest request
    ) {
        CurrentUserContext currentUser =
                validateCurrentHrManager();

        validateDepartment(request.departmentId());

        OnboardingTask task = OnboardingTask.create(
                request.departmentId(),
                request.taskTitle(),
                request.taskDescription(),
                request.defaultDueDays(),
                currentUser.appUserId());

        return OnboardingTaskResponse.from(
                onboardingTaskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public OnboardingTaskPageResponse getTasks(
            int page,
            int size
    ) {
        validateCurrentHrManager();
        validatePageRequest(page, size);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                TASK_LIST_SORT);
        Page<OnboardingTaskResponse> taskPage =
                onboardingTaskRepository.findAll(pageRequest)
                        .map(OnboardingTaskResponse::from);

        return OnboardingTaskPageResponse.from(taskPage);
    }

    @Transactional(readOnly = true)
    public OnboardingTaskResponse getTask(Long taskId) {
        validateCurrentHrManager();
        validateTaskId(taskId);
        return OnboardingTaskResponse.from(findTask(taskId));
    }

    @Transactional
    public OnboardingTaskResponse updateTask(
            Long taskId,
            OnboardingTaskUpdateRequest request
    ) {
        validateCurrentHrManager();
        validateTaskId(taskId);
        validateDepartment(request.departmentId());

        OnboardingTask task = findTask(taskId);
        task.update(
                request.departmentId(),
                request.taskTitle(),
                request.taskDescription(),
                request.defaultDueDays());

        return OnboardingTaskResponse.from(task);
    }

    @Transactional
    public OnboardingTaskResponse changeActivation(
            Long taskId,
            OnboardingTaskActivationRequest request
    ) {
        validateCurrentHrManager();
        validateTaskId(taskId);

        OnboardingTask task = findTask(taskId);
        task.changeActivation(request.active());

        return OnboardingTaskResponse.from(task);
    }

    private OnboardingTask findTask(Long taskId) {
        return onboardingTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Onboarding task not found"));
    }

    private void validateDepartment(Long departmentId) {
        if (!organizationQueryService
                .isValidDepartment(departmentId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Department not found or inactive");
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

    private void validateTaskId(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Task ID must be a positive number");
        }
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Page must not be negative");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Size must be between 1 and "
                            + MAX_PAGE_SIZE);
        }
    }
}
