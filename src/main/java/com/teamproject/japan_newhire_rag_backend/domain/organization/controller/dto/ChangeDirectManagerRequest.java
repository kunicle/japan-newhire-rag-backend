package com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChangeDirectManagerRequest(
        @NotNull @Positive Long managerEmployeeId
) {
}
