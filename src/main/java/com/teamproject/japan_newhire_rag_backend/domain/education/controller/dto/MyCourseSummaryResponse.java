package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;

public record MyCourseSummaryResponse(
        Long enrollmentId,
        Long courseId,
        String courseName,
        boolean required,
        LocalDate enrollmentDueDate,
        BigDecimal progressRate,
        EnrollmentStatus status
) {

    public static MyCourseSummaryResponse from(
            CourseEnrollment enrollment,
            LocalDate today
    ) {
        Course course = enrollment.getCourse();

        return new MyCourseSummaryResponse(
                enrollment.getCourseEnrollmentId(),
                course.getCourseId(),
                course.getCourseName(),
                course.isRequired(),
                enrollment.getEnrollmentDueDate(),
                enrollment.getProgressRate(),
                enrollment.getEffectiveStatus(today));
    }
}