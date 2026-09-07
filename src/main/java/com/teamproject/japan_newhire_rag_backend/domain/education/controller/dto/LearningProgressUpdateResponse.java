package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.LearningProgress;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.LearningCompletionStatus;

public record LearningProgressUpdateResponse(
        Long progressId,
        Long enrollmentId,
        Long moduleId,
        LearningCompletionStatus completionStatus,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        BigDecimal progressRate,
        EnrollmentStatus enrollmentStatus
) {

    public static LearningProgressUpdateResponse from(
            LearningProgress progress,
            LocalDate today
    ) {
        CourseEnrollment enrollment =
                progress.getCourseEnrollment();

        return new LearningProgressUpdateResponse(
                progress.getLearningProgressId(),
                enrollment.getCourseEnrollmentId(),
                progress.getCourseModule().getCourseModuleId(),
                progress.getCompletionStatus(),
                progress.getStartedAt(),
                progress.getCompletedAt(),
                enrollment.getProgressRate(),
                enrollment.getEffectiveStatus(today));
    }
}