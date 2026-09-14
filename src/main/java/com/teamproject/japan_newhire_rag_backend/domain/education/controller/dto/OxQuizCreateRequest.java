package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OxQuizCreateRequest(
        @NotBlank
        @Size(max = 200)
        String quizTitle,

        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        @Digits(integer = 3, fraction = 2)
        BigDecimal passingScore,

        @Min(1)
        Integer maxAttemptCount,

        Boolean required,

        @NotEmpty
        @Size(max = 100)
        List<@Valid OxQuizQuestionRequest> questions
) {
    public OxQuizCreateRequest(
            String quizTitle,
            BigDecimal passingScore,
            Integer maxAttemptCount,
            List<OxQuizQuestionRequest> questions
    ) {
        this(
                quizTitle,
                passingScore,
                maxAttemptCount,
                true,
                questions);
    }

    public OxQuizCreateRequest {
        if (required == null) {
            required = true;
        }
    }
}
