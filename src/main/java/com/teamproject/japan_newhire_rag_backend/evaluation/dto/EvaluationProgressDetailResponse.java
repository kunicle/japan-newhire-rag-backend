package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;

public record EvaluationProgressDetailResponse(
        Long evaluationId,
        EvaluationStatus evaluationStatus,
        EvaluationProgressStatus progressStatus,
        LocalDateTime submittedAt
) {
}
