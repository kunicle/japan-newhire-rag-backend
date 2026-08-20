package com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity;

import java.util.Objects;

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

    public static OnboardingTask create(
            Long departmentId,
            String taskTitle,
            String taskDescription,
            int defaultDueDays,
            Long createdBy
    ) {
        validateRequiredValues(
                departmentId,
                taskTitle,
                taskDescription,
                defaultDueDays,
                createdBy);

        OnboardingTask task = new OnboardingTask();
        task.departmentId = departmentId;
        task.taskTitle = taskTitle;
        task.taskDescription = taskDescription;
        task.defaultDueDays = defaultDueDays;
        task.active = true;
        task.createdBy = createdBy;
        return task;
    }

    public void update(
            Long departmentId,
            String taskTitle,
            String taskDescription,
            int defaultDueDays
    ) {
        validateRequiredValues(
                departmentId,
                taskTitle,
                taskDescription,
                defaultDueDays,
                createdBy);

        this.departmentId = departmentId;
        this.taskTitle = taskTitle;
        this.taskDescription = taskDescription;
        this.defaultDueDays = defaultDueDays;
    }

    public boolean changeActivation(boolean active) {
        if (this.active == active) {
            return false;
        }

        this.active = active;
        return true;
    }

    private static void validateRequiredValues(
            Long departmentId,
            String taskTitle,
            String taskDescription,
            int defaultDueDays,
            Long createdBy
    ) {
        Objects.requireNonNull(
                departmentId,
                "Department ID is required");
        Objects.requireNonNull(
                taskTitle,
                "Task title is required");
        Objects.requireNonNull(
                taskDescription,
                "Task description is required");
        Objects.requireNonNull(
                createdBy,
                "Creator ID is required");

        if (defaultDueDays <= 0) {
            throw new IllegalArgumentException(
                    "Default due days must be positive");
        }
    }
}
