package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.OnboardingAssignmentService;

@RestController
@RequestMapping("/api/hr/onboarding-assignments")
public class OnboardingAssignmentController {

    private final OnboardingAssignmentService assignmentService;

    public OnboardingAssignmentController(
            OnboardingAssignmentService assignmentService
    ) {
        this.assignmentService = assignmentService;
    }

    @PatchMapping("/{assignmentId}/cancel")
    public ResponseEntity<Void> cancelAssignment(
            @PathVariable String assignmentId
    ) {
        assignmentService.cancel(
                parseAssignmentId(assignmentId));

        return ResponseEntity.noContent().build();
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