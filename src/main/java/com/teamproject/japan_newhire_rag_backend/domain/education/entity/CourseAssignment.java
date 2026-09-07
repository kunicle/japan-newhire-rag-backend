package com.teamproject.japan_newhire_rag_backend.domain.education.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.teamproject.japan_newhire_rag_backend.domain.education.enums.AssignmentTargetType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course_assignment")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_assignment_id")
    private Long courseAssignmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private AssignmentTargetType targetType;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "job_grade_id")
    private Long jobGradeId;

    @Column(name = "is_new_employee_target", nullable = false)
    private boolean newEmployeeTarget = false;

    @Column(name = "enrollment_round", nullable = false, length = 30)
    private String enrollmentRound = "1";

    @Column(name = "enrollment_start_date", nullable = false)
    private LocalDate enrollmentStartDate;

    @Column(name = "enrollment_due_date", nullable = false)
    private LocalDate enrollmentDueDate;

    @Column(name = "assigned_by", nullable = false)
    private Long assignedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static CourseAssignment create(
            Course course,
            AssignmentTargetType targetType,
            Long employeeId,
            Long departmentId,
            Long jobGradeId,
            String enrollmentRound,
            LocalDate enrollmentStartDate,
            LocalDate enrollmentDueDate,
            Long assignedBy
    ) {
        CourseAssignment assignment = new CourseAssignment();
        assignment.course = course;
        assignment.targetType = targetType;
        assignment.employeeId = employeeId;
        assignment.departmentId = departmentId;
        assignment.jobGradeId = jobGradeId;
        assignment.newEmployeeTarget =
                targetType == AssignmentTargetType.NEW_HIRE;
        assignment.enrollmentRound = enrollmentRound;
        assignment.enrollmentStartDate = enrollmentStartDate;
        assignment.enrollmentDueDate = enrollmentDueDate;
        assignment.assignedBy = assignedBy;
        return assignment;
    }
}
