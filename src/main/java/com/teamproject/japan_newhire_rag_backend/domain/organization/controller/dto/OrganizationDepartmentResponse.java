package com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto;

import java.util.List;

public record OrganizationDepartmentResponse(
        Long departmentId,
        String departmentCode,
        String departmentName,
        Long parentDepartmentId,
        Integer displayOrder,
        List<OrganizationEmployeeResponse> employees,
        List<OrganizationDepartmentResponse> children
) {

    public OrganizationDepartmentResponse {
        employees = employees == null ? List.of() : List.copyOf(employees);
        children = children == null ? List.of() : List.copyOf(children);
    }
}
