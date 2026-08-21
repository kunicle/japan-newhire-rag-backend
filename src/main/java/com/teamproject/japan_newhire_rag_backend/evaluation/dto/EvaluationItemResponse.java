package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationItem;

public record EvaluationItemResponse(
        Long evaluationItemId,
        Long evaluationTemplateId,
        String itemName,
        String itemDescription,
        Integer itemOrder,
        BigDecimal weight,
        Boolean isRequired,
        Integer minimumScore,
        Integer maximumScore,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static EvaluationItemResponse from(EvaluationItem item) {
        return new EvaluationItemResponse(
                item.getEvaluationItemId(),
                item.getEvaluationTemplateId(),
                item.getItemName(),
                item.getItemDescription(),
                item.getItemOrder(),
                item.getWeight(),
                item.getIsRequired(),
                item.getMinimumScore(),
                item.getMaximumScore(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}
