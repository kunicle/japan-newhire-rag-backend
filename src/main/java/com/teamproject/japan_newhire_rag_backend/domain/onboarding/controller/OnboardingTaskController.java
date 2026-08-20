package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingAssignmentCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingAssignmentCreateResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskActivationRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.OnboardingAssignmentService;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.OnboardingTaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/hr/onboarding-tasks")
public class OnboardingTaskController {

    private final OnboardingTaskService taskService;
    private final OnboardingAssignmentService assignmentService;

    public OnboardingTaskController(
            OnboardingTaskService taskService,
            OnboardingAssignmentService assignmentService
    ) {
        this.taskService = taskService;
        this.assignmentService = assignmentService;
    }

    @PostMapping
    public ResponseEntity<OnboardingTaskResponse> createTask(
            @Valid @RequestBody OnboardingTaskCreateRequest request
    ) {
        OnboardingTaskResponse response =
                taskService.createTask(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{taskId}")
    public OnboardingTaskResponse updateTask(
            @PathVariable String taskId,
            @Valid @RequestBody OnboardingTaskUpdateRequest request
    ) {
        return taskService.updateTask(
                parseTaskId(taskId),
                request);
    }

    @PatchMapping("/{taskId}/activation")
    public OnboardingTaskResponse changeActivation(
            @PathVariable String taskId,
            @Valid @RequestBody OnboardingTaskActivationRequest request
    ) {
        return taskService.changeActivation(
                parseTaskId(taskId),
                request);
    }

    @PostMapping("/{taskId}/assignments")
    public ResponseEntity<OnboardingAssignmentCreateResponse>
            createAssignments(
                    @PathVariable String taskId,
                    @Valid
                    @RequestBody
                    OnboardingAssignmentCreateRequest request
            ) {
        OnboardingAssignmentCreateResponse response =
                assignmentService.assign(
                        parseTaskId(taskId),
                        request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    private Long parseTaskId(String taskId) {
        try {
            return Long.valueOf(taskId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Task ID must be a number");
        }
    }
}