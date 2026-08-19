package com.teamproject.japan_newhire_rag_backend.domain.education.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course_enrollment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_enrollment", columnNames = {
                "course_id", "employee_id", "enrollment_round"
        })
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseEnrollment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_enrollment_id")
    private Long courseEnrollmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_assignment_id", nullable = false)
    private CourseAssignment courseAssignment;

    @Column(name = "enrollment_round", nullable = false, length = 30)
    private String enrollmentRound = "1";

    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_status", nullable = false, length = 20)
    private EnrollmentStatus enrollmentStatus = EnrollmentStatus.NOT_STARTED;

    @Column(name = "progress_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal progressRate = BigDecimal.ZERO;

    @Column(name = "enrollment_start_date", nullable = false)
    private LocalDate enrollmentStartDate;

    @Column(name = "enrollment_due_date", nullable = false)
    private LocalDate enrollmentDueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static CourseEnrollment create(
        Course course,
        Long employeeId,
        CourseAssignment courseAssignment,
        String enrollmentRound,
        LocalDate enrollmentStartDate,
        LocalDate enrollmentDueDate
    ) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.course = course;
        enrollment.employeeId = employeeId;
        enrollment.courseAssignment = courseAssignment;
        enrollment.enrollmentRound = enrollmentRound;
        enrollment.enrollmentStatus = EnrollmentStatus.NOT_STARTED;
        enrollment.progressRate = BigDecimal.ZERO;
        enrollment.enrollmentStartDate = enrollmentStartDate;
        enrollment.enrollmentDueDate = enrollmentDueDate;
        enrollment.completedAt = null;
        return enrollment;
    }
}
