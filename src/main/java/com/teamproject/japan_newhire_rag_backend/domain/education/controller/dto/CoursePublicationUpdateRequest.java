package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import com.teamproject.japan_newhire_rag_backend.domain.education.enums.CoursePublicationStatus;

import jakarta.validation.constraints.NotNull;

public record CoursePublicationUpdateRequest(
        @NotNull CoursePublicationStatus publicationStatus
) {
}
