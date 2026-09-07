package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;

public record EvaluationTemplateUpdateRequest(
        String templateName,
        EvaluationType evaluationType,
        String templateDescription,
        Boolean isActive
) {
}
