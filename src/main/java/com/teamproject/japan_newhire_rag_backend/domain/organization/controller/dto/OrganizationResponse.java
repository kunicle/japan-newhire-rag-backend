package com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto;

import java.util.List;

public record OrganizationResponse(List<OrganizationDepartmentResponse> departments) {

    public OrganizationResponse {
        departments = departments == null ? List.of() : List.copyOf(departments);
    }
}
