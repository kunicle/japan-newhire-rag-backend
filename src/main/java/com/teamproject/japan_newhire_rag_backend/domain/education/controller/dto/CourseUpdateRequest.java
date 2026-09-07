package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseUpdateRequest(
        @NotBlank
        @Size(max = 100)
        String courseName,

        @NotBlank
        @Size(max = 2000)
        String courseDescription,

        @NotNull
        Boolean required,

        @NotNull
        LocalDate trainingStartDate,

        @NotNull
        LocalDate trainingEndDate
) {
}
