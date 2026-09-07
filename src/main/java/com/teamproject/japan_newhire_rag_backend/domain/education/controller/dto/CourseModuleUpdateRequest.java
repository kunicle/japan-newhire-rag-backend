package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseModuleUpdateRequest(
        @NotBlank
        @Size(max = 200)
        String moduleTitle,

        String moduleContent,

        @Size(max = 500)
        String referenceUrl,

        @NotNull
        @Min(1)
        Integer moduleOrder,

        @NotNull
        Boolean required
) {

    @AssertTrue(message = "moduleContent or referenceUrl must be provided")
    public boolean isContentOrReferenceProvided() {
        return hasText(moduleContent) || hasText(referenceUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
