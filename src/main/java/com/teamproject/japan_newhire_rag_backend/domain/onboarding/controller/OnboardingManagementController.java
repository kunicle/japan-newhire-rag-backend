package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingAssignmentCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingAssignmentCreateResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingCompletionRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingManagementItemResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingManagementPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.OnboardingAssignmentService;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.OnboardingManagementService;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.OnboardingTaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/onboarding-management")
@PreAuthorize("hasAnyRole('HR_MANAGER', 'MANAGER')")
public class OnboardingManagementController {

    private final OnboardingManagementService managementService;
    private final OnboardingAssignmentService assignmentService;
    private final OnboardingTaskService taskService;

    public OnboardingManagementController(
            OnboardingManagementService managementService,
            OnboardingAssignmentService assignmentService,
            OnboardingTaskService taskService
    ) {
        this.managementService = managementService;
        this.assignmentService = assignmentService;
        this.taskService = taskService;
    }

    @GetMapping("/progress")
    public OnboardingManagementPageResponse getProgress(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return managementService.getProgress(
                employeeId,
                page,
                size);
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasRole('MANAGER')")
    public OnboardingTaskPageResponse getManagedTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return taskService.getManagedTasks(page, size);
    }

    @PostMapping("/tasks/{taskId}/assignments")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<OnboardingAssignmentCreateResponse>
            assignManaged(
                    @PathVariable Long taskId,
                    @Valid @RequestBody
                    OnboardingAssignmentCreateRequest request
            ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.assignManaged(
                        taskId,
                        request));
    }

    @PatchMapping("/assignments/{assignmentId}/start")
    public OnboardingManagementItemResponse start(
            @PathVariable Long assignmentId
    ) {
        return managementService.start(assignmentId);
    }

    @PatchMapping("/assignments/{assignmentId}/complete")
    public OnboardingManagementItemResponse complete(
            @PathVariable Long assignmentId,
            @Valid @RequestBody OnboardingCompletionRequest request
    ) {
        return managementService.complete(
                assignmentId,
                request);
    }
}
