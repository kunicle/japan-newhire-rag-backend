package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;

public record EvaluationTemplateCreateRequest(
        Long evaluationCycleId,
        String templateName,
        EvaluationType evaluationType,
        String templateDescription,
        Boolean isActive
) {
}
