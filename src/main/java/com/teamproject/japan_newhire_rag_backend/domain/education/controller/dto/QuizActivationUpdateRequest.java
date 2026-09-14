package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import jakarta.validation.constraints.NotNull;

public record QuizActivationUpdateRequest(
        @NotNull
        Boolean active
) {
}