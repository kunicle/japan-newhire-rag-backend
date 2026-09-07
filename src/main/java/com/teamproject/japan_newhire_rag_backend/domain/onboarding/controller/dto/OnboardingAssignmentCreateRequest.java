package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OnboardingAssignmentCreateRequest(

        @NotEmpty
        @Valid
        List<
                @NotNull
                @Positive
                Long
        > employeeIds
) {
}