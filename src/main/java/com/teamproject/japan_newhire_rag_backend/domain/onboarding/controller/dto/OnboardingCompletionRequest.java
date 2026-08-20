package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto;

import jakarta.validation.constraints.Size;

public record OnboardingCompletionRequest(

        @Size(max = 1000)
        String completionNote
) {
}