package com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto;

import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmploymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChangeEmployeeOrganizationRequest(
        @NotNull @Positive Long departmentId,
        @NotNull @Positive Long jobGradeId,
        @Positive Long managerEmployeeId,
        @NotNull EmploymentStatus employmentStatus
) {}
