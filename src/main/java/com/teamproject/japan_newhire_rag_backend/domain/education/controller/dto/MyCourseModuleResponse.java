package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseModule;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.LearningProgress;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.LearningCompletionStatus;

public record MyCourseModuleResponse(
        Long progressId,
        Long moduleId,
        String moduleTitle,
        String moduleContent,
        String referenceUrl,
        int moduleOrder,
        boolean required,
        LearningCompletionStatus completionStatus,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {

    public static MyCourseModuleResponse from(
            LearningProgress progress
    ) {
        CourseModule module = progress.getCourseModule();

        return new MyCourseModuleResponse(
                progress.getLearningProgressId(),
                module.getCourseModuleId(),
                module.getModuleTitle(),
                module.getModuleContent(),
                module.getReferenceUrl(),
                module.getModuleOrder(),
                module.isRequired(),
                progress.getCompletionStatus(),
                progress.getStartedAt(),
                progress.getCompletedAt());
    }
}