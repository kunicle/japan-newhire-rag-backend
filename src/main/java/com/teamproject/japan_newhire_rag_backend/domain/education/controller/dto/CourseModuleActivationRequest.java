package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import jakarta.validation.constraints.NotNull;

public record CourseModuleActivationRequest(
        @NotNull Boolean active
) {
}
