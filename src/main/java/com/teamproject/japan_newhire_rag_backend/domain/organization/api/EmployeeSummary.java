package com.teamproject.japan_newhire_rag_backend.domain.organization.api;

public record EmployeeSummary(
        Long employeeId,
        String employeeName,
        Long departmentId,
        String departmentName,
        Long jobGradeId,
        String jobGradeName
) {
}
