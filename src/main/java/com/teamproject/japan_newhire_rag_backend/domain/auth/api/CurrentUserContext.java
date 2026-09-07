package com.teamproject.japan_newhire_rag_backend.domain.auth.api;

import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

import java.util.Set;

public record CurrentUserContext(
        Long appUserId,
        Long employeeId,
        Set<RoleType> roles,
        Long departmentId,
        Integer jobGradeLevel,
        EmployeeType employeeType
) {

    public CurrentUserContext {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
