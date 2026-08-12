package com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto;

import java.time.LocalDate;

public record OrganizationEmployeeResponse(
        Long employeeId,
        String employeeNumber,
        String employeeName,
        Long departmentId,
        Long jobGradeId,
        String jobGradeName,
        Integer jobGradeLevel,
        LocalDate hireDate
) {
}
