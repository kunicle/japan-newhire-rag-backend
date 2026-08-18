package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

public record EvaluationResultResponse(
        EvaluationResultCycleResponse cycle,
        EvaluationResultDetailResponse self,
        EvaluationResultDetailResponse manager
) {
}
