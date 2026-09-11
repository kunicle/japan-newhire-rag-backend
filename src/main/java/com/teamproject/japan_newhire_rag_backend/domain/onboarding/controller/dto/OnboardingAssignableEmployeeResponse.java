package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto;

import com.teamproject.japan_newhire_rag_backend.domain.organization.api.EmployeeSummary;

public record OnboardingAssignableEmployeeResponse(
        Long employeeId,
        String employeeName,
        Long departmentId,
        String departmentName,
        Long jobGradeId,
        String jobGradeName
) {

    public static OnboardingAssignableEmployeeResponse from(
            EmployeeSummary employee
    ) {
        return new OnboardingAssignableEmployeeResponse(
                employee.employeeId(),
                employee.employeeName(),
                employee.departmentId(),
                employee.departmentName(),
                employee.jobGradeId(),
                employee.jobGradeName());
    }
}
