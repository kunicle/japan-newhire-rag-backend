package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

public record EvaluationAssignmentRequest(
        Long evaluationCycleId,
        Long targetEmployeeId
) {
}
