package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuizAnswerRequest(
        @NotNull
        @Positive
        Long questionId,

        @NotNull
        @Positive
        Long optionId
) {
}