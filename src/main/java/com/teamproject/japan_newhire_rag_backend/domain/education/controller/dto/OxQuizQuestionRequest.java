package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OxQuizQuestionRequest(
        @NotBlank
        @Size(max = 2000)
        String questionContent,

        @NotNull
        @DecimalMin("0.01")
        @DecimalMax("100.00")
        @Digits(integer = 3, fraction = 2)
        BigDecimal score,

        @NotBlank
        @Pattern(
                regexp = "O|X",
                message = "Correct answer must be O or X")
        String correctAnswer
) {
}