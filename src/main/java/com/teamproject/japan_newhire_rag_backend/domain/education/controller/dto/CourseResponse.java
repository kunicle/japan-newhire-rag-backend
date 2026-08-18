package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.CoursePublicationStatus;

public record CourseResponse(
        Long courseId,
        String courseName,
        String courseDescription,
        boolean required,
        LocalDate trainingStartDate,
        LocalDate trainingEndDate,
        CoursePublicationStatus publicationStatus,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getCourseId(),
                course.getCourseName(),
                course.getCourseDescription(),
                course.isRequired(),
                course.getTrainingStartDate(),
                course.getTrainingEndDate(),
                course.getPublicationStatus(),
                course.getCreatedBy(),
                course.getCreatedAt(),
                course.getUpdatedAt());
    }
}
