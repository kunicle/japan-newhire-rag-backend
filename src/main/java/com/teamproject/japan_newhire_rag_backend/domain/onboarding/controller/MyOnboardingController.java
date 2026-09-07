package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.MyOnboardingResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.MyOnboardingService;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingCompletionRequest;

@RestController
@RequestMapping("/api/me/onboarding")
public class MyOnboardingController {

    private final MyOnboardingService myOnboardingService;

    public MyOnboardingController(
            MyOnboardingService myOnboardingService
    ) {
        this.myOnboardingService = myOnboardingService;
    }

    @GetMapping
    public List<MyOnboardingResponse> getMyOnboarding() {
        return myOnboardingService.getMyOnboarding();
    }

    @PatchMapping("/{assignmentId}/start")
    public MyOnboardingResponse start(
            @PathVariable String assignmentId
    ) {
        return myOnboardingService.start(
                parseAssignmentId(assignmentId));
    }

    @PatchMapping("/{assignmentId}/complete")
    public MyOnboardingResponse complete(
            @PathVariable String assignmentId,
            @Valid
            @RequestBody OnboardingCompletionRequest request
    ) {
        return myOnboardingService.complete(
                parseAssignmentId(assignmentId),
                request);
    }

    private Long parseAssignmentId(String assignmentId) {
        try {
            return Long.valueOf(assignmentId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Assignment ID must be a number");
        }
    }
}
