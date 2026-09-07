package com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto;

import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmploymentStatus;

public record CreateUserResponse(
        Long appUserId,
        Long employeeId,
        AccountStatus accountStatus,
        EmploymentStatus employmentStatus
) {
}
