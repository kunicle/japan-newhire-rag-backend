package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseModule;

public record CourseModuleResponse(
        Long courseModuleId,
        Long courseId,
        String moduleTitle,
        String moduleContent,
        String referenceUrl,
        int moduleOrder,
        boolean required,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CourseModuleResponse from(CourseModule module) {
        return new CourseModuleResponse(
                module.getCourseModuleId(),
                module.getCourse().getCourseId(),
                module.getModuleTitle(),
                module.getModuleContent(),
                module.getReferenceUrl(),
                module.getModuleOrder(),
                module.isRequired(),
                module.isActive(),
                module.getCreatedAt(),
                module.getUpdatedAt());
    }
}
