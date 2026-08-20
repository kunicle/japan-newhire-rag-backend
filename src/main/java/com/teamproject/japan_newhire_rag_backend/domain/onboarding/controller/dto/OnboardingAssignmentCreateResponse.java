package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto;

public record OnboardingAssignmentCreateResponse(
        Long onboardingTaskId,
        int requestedCount,
        int successCount,
        int duplicateCount
) {
}