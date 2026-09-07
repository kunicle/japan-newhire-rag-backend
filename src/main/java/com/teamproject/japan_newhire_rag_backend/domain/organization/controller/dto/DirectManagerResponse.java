package com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto;

public record DirectManagerResponse(
        Long employeeId,
        Long managerEmployeeId
) {
}
