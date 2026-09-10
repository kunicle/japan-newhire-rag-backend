package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingCompletionRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingManagementItemResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingManagementPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.OnboardingManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/onboarding-management")
@PreAuthorize("hasAnyRole('HR_MANAGER', 'MANAGER')")
public class OnboardingManagementController {

    private final OnboardingManagementService managementService;

    public OnboardingManagementController(
            OnboardingManagementService managementService
    ) {
        this.managementService = managementService;
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
