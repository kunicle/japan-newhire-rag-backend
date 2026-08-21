package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;

public record SelfEvaluationResponse(
        Long evaluationId,
        Long evaluationCycleId,
        Long evaluationTemplateId,
        EvaluationStatus evaluationStatus,
        EvaluationCycleStatus currentCycleStatus,
        List<SelfEvaluationItemResponse> items,
        String overallFeedback
) {
}
