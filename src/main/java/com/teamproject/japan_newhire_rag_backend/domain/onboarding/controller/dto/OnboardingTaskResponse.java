package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingTask;

public record OnboardingTaskResponse(
        Long taskId,
        Long departmentId,
        String taskTitle,
        String taskDescription,
        int defaultDueDays,
        boolean active,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OnboardingTaskResponse from(
            OnboardingTask task
    ) {
        return new OnboardingTaskResponse(
                task.getOnboardingTaskId(),
                task.getDepartmentId(),
                task.getTaskTitle(),
                task.getTaskDescription(),
                task.getDefaultDueDays(),
                task.isActive(),
                task.getCreatedBy(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}