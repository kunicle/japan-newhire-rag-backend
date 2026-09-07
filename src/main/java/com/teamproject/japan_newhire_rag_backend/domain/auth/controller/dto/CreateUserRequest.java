package com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto;

import java.time.LocalDate;

import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank String password,
        @NotBlank @Size(max = 30) String employeeNumber,
        @NotBlank @Size(max = 50) String employeeName,
        @NotNull Long departmentId,
        @NotNull Long jobGradeId,
        @NotNull EmployeeType employeeType,
        @NotNull LocalDate hireDate
) {
}
