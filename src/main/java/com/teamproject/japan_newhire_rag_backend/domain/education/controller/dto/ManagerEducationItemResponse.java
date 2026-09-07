package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.EmployeeSummary;

public record ManagerEducationItemResponse(
        Long employeeId,
        String employeeName,
        Long departmentId,
        String departmentName,
        Long enrollmentId,
        Long courseId,
        String courseName,
        BigDecimal progressRate,
        EnrollmentStatus status,
        LocalDate dueDate,
        boolean overdue
) {

    public static ManagerEducationItemResponse from(
            CourseEnrollment enrollment,
            EmployeeSummary employee,
            LocalDate today
    ) {
        Course course = enrollment.getCourse();
        EnrollmentStatus effectiveStatus =
                enrollment.getEffectiveStatus(today);

        return new ManagerEducationItemResponse(
                employee.employeeId(),
                employee.employeeName(),
                employee.departmentId(),
                employee.departmentName(),
                enrollment.getCourseEnrollmentId(),
                course.getCourseId(),
                course.getCourseName(),
                enrollment.getProgressRate(),
                effectiveStatus,
                enrollment.getEnrollmentDueDate(),
                effectiveStatus == EnrollmentStatus.OVERDUE);
    }
}