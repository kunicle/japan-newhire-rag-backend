package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingProgress;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingTask;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingAssignmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingCompletionStatus;

public record MyOnboardingResponse(
        Long onboardingAssignmentId,
        Long onboardingTaskId,
        Long departmentId,
        String taskTitle,
        String taskDescription,
        LocalDate assignedDate,
        LocalDate dueDate,
        OnboardingAssignmentStatus assignmentStatus,
        OnboardingCompletionStatus completionStatus,
        String completionNote,
        LocalDateTime completedAt,
        boolean overdue
) {

    public static MyOnboardingResponse from(
            OnboardingProgress progress,
            LocalDate today
    ) {
        Objects.requireNonNull(
                progress,
                "Onboarding progress is required");
        Objects.requireNonNull(
                today,
                "Current date is required");

        OnboardingAssignment assignment =
                progress.getOnboardingAssignment();
        OnboardingTask task =
                assignment.getOnboardingTask();

        boolean overdue =
                assignment.getAssignmentStatus()
                        == OnboardingAssignmentStatus.ASSIGNED
                        && assignment.getDueDate().isBefore(today)
                        && progress.getCompletionStatus()
                                != OnboardingCompletionStatus.COMPLETED;

        return new MyOnboardingResponse(
                assignment.getOnboardingAssignmentId(),
                task.getOnboardingTaskId(),
                task.getDepartmentId(),
                task.getTaskTitle(),
                task.getTaskDescription(),
                assignment.getAssignedDate(),
                assignment.getDueDate(),
                assignment.getAssignmentStatus(),
                progress.getCompletionStatus(),
                progress.getCompletionNote(),
                progress.getCompletedAt(),
                overdue);
    }
}