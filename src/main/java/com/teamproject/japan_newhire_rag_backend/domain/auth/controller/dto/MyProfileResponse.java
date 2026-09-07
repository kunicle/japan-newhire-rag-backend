package com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto;

import java.time.LocalDate;
import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;

public record MyProfileResponse(
        Long appUserId,
        Long employeeId,
        String employeeNumber,
        String employeeName,
        String email,
        Long departmentId,
        String departmentName,
        Long jobGradeId,
        String jobGradeName,
        Integer jobGradeLevel,
        Set<RoleType> roles,
        LocalDate hireDate,
        Long managerEmployeeId,
        String managerName
) {

    public MyProfileResponse {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
