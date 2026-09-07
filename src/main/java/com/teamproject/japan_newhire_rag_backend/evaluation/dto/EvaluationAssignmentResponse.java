package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

public record EvaluationAssignmentResponse(
        Long evaluationCycleId,
        Long targetEmployeeId,
        Long managerEmployeeId,
        Long selfEvaluationId,
        Long managerEvaluationId
) {
}
