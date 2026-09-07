package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.LearningProgress;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;

public record MyCourseDetailResponse(
        Long enrollmentId,
        Long courseId,
        String courseName,
        String courseDescription,
        boolean required,
        String enrollmentRound,
        LocalDate enrollmentStartDate,
        LocalDate enrollmentDueDate,
        BigDecimal progressRate,
        EnrollmentStatus status,
        LocalDateTime completedAt,
        List<MyCourseModuleResponse> modules
) {

    public static MyCourseDetailResponse from(
            CourseEnrollment enrollment,
            List<LearningProgress> progresses,
            LocalDate today
    ) {
        Course course = enrollment.getCourse();

        List<MyCourseModuleResponse> modules = progresses.stream()
                .map(MyCourseModuleResponse::from)
                .toList();

        return new MyCourseDetailResponse(
                enrollment.getCourseEnrollmentId(),
                course.getCourseId(),
                course.getCourseName(),
                course.getCourseDescription(),
                course.isRequired(),
                enrollment.getEnrollmentRound(),
                enrollment.getEnrollmentStartDate(),
                enrollment.getEnrollmentDueDate(),
                enrollment.getProgressRate(),
                enrollment.getEffectiveStatus(today),
                enrollment.getCompletedAt(),
                modules);
    }
}