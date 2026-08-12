package com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "onboarding_task")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "onboarding_task_id")
    private Long onboardingTaskId;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "task_title", nullable = false, length = 200)
    private String taskTitle;

    @Column(name = "task_description", nullable = false, length = 2000)
    private String taskDescription;

    @Column(name = "default_due_days", nullable = false)
    private int defaultDueDays;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;
}
