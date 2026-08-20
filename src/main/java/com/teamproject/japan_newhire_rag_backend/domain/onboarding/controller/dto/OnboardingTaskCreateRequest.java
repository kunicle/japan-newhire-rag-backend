package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OnboardingTaskCreateRequest(
        @NotNull
        @Positive
        Long departmentId,

        @NotBlank
        @Size(max = 200)
        String taskTitle,

        @NotBlank
        @Size(max = 2000)
        String taskDescription,

        @NotNull
        @Min(1)
        Integer defaultDueDays
) {
}