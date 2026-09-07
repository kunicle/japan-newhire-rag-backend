package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.util.List;

public record CourseEnrollmentCreateResponse(
        int assignedCount,
        int duplicateCount,
        List<Long> duplicateEmployeeIds
) {

    public CourseEnrollmentCreateResponse {
        duplicateEmployeeIds = List.copyOf(duplicateEmployeeIds);
    }
}