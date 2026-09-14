package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto;

import java.time.LocalDate;

public record OnboardingAssignmentResponse(
        Long onboardingAssignmentId,
        Long employeeId,
        LocalDate assignedDate,
        LocalDate dueDate,
        String status
) {
}