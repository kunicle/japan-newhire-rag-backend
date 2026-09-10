package com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
public record CreateDepartmentRequest(
        @NotBlank @Size(max = 30) String departmentCode,
        @NotBlank @Size(max = 100) String departmentName,
        @Positive Long parentDepartmentId
) {}
