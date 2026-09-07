package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

public record EvaluationProgressSummary(
        long notStartedCount,
        long inProgressCount,
        long submittedCount
) {
}
