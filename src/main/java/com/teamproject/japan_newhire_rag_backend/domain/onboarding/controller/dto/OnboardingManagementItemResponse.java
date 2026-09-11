package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingProgress;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingTask;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingAssignmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.EmployeeSummary;

public record OnboardingManagementItemResponse(
        Long employeeId,
        String employeeName,
        Long employeeDepartmentId,
        String employeeDepartmentName,
        Long onboardingAssignmentId,
        Long onboardingTaskId,
        Long taskDepartmentId,
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

    public static OnboardingManagementItemResponse from(
            OnboardingProgress progress,
            EmployeeSummary employee,
            LocalDate today
    ) {
        Objects.requireNonNull(progress, "Onboarding progress is required");
        Objects.requireNonNull(employee, "Employee information is required");
        Objects.requireNonNull(today, "Current date is required");

        OnboardingAssignment assignment = progress.getOnboardingAssignment();
        OnboardingTask task = assignment.getOnboardingTask();

        boolean overdue =
                assignment.getAssignmentStatus() == OnboardingAssignmentStatus.ASSIGNED
                        && assignment.getDueDate().isBefore(today)
                        && progress.getCompletionStatus()
                                != OnboardingCompletionStatus.COMPLETED;

        return new OnboardingManagementItemResponse(
                assignment.getEmployeeId(),
                employee.employeeName(),
                employee.departmentId(),
                employee.departmentName(),
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
