package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationTemplate;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;

public record EvaluationTemplateResponse(
        Long evaluationTemplateId,
        Long evaluationCycleId,
        String templateName,
        EvaluationType evaluationType,
        String templateDescription,
        Boolean isActive,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static EvaluationTemplateResponse from(EvaluationTemplate template) {
        return new EvaluationTemplateResponse(
                template.getEvaluationTemplateId(),
                template.getEvaluationCycleId(),
                template.getTemplateName(),
                template.getEvaluationType(),
                template.getTemplateDescription(),
                template.getIsActive(),
                template.getCreatedBy(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }
}
