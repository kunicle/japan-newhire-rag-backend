package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuizAttemptSubmitRequest(
        @NotNull
        @Positive
        Long enrollmentId,

        @NotEmpty
        List<@Valid QuizAnswerRequest> answers
) {
}