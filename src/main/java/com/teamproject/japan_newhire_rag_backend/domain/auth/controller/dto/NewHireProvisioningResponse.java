package com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto;

import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmploymentStatus;

public record NewHireProvisioningResponse(
        Long appUserId,
        Long employeeId,
        AccountStatus accountStatus,
        EmploymentStatus employmentStatus,
        Set<RoleType> roles
) {
    public NewHireProvisioningResponse {
        roles = Set.copyOf(roles);
    }
}
