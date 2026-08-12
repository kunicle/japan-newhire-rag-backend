package com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity;

import java.time.LocalDate;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingAssignmentStatus;

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
@Table(name = "onboarding_assignment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_onboarding_assignment", columnNames = {
                "onboarding_task_id", "employee_id"
        })
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "onboarding_assignment_id")
    private Long onboardingAssignmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "onboarding_task_id", nullable = false)
    private OnboardingTask onboardingTask;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "assigned_by", nullable = false)
    private Long assignedBy;

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false, length = 20)
    private OnboardingAssignmentStatus assignmentStatus = OnboardingAssignmentStatus.ASSIGNED;
}
