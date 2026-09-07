package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.math.BigDecimal;
import java.util.List;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;

public record EvaluationResultDetailResponse(
        Long evaluationId,
        EvaluationStatus evaluationStatus,
        BigDecimal totalScore,
        List<EvaluationResultItemResponse> items,
        String overallFeedback
) {
}
