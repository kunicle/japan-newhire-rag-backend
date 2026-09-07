package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto;

import jakarta.validation.constraints.NotNull;

public record OnboardingTaskActivationRequest(
        @NotNull
        Boolean active
) {
}