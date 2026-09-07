package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.time.LocalDate;

import com.teamproject.japan_newhire_rag_backend.domain.education.enums.AssignmentTargetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CourseEnrollmentCreateRequest(

        @NotNull
        AssignmentTargetType targetType,

        @Positive
        Long employeeId,

        @Positive
        Long departmentId,

        @Positive
        Long jobGradeId,

        @NotBlank
        @Size(max = 30)
        String enrollmentRound,

        @NotNull
        LocalDate enrollmentStartDate,

        @NotNull
        LocalDate enrollmentDueDate
) {
}