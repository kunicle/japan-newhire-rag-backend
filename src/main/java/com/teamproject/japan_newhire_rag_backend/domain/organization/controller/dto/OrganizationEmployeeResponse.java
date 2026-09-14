package com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto;

import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmploymentStatus;
import java.time.LocalDate;

public record OrganizationEmployeeResponse(
        Long employeeId,
        String employeeNumber,
        String employeeName,
        Long departmentId,
        Long jobGradeId,
        String jobGradeName,
        Integer jobGradeLevel,
        LocalDate hireDate,
        String departmentName,
        Long managerEmployeeId,
        EmploymentStatus employmentStatus
) {
    public OrganizationEmployeeResponse(Long employeeId, String employeeNumber,
            String employeeName, Long departmentId, Long jobGradeId, String jobGradeName,
            Integer jobGradeLevel, LocalDate hireDate) {
        this(employeeId, employeeNumber, employeeName, departmentId, jobGradeId,
                jobGradeName, jobGradeLevel, hireDate, null, null, null);
    }
}
