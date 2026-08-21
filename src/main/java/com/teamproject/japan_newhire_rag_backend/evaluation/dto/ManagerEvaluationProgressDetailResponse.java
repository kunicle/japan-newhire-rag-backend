package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;

public record ManagerEvaluationProgressDetailResponse(
        Long evaluationId,
        EvaluationStatus evaluationStatus,
        LocalDateTime submittedAt
) {
}
