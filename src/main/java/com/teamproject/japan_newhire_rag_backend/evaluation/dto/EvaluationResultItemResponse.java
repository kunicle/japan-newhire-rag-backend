package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.math.BigDecimal;

public record EvaluationResultItemResponse(
        Long evaluationItemId,
        Integer itemOrder,
        String itemName,
        String itemDescription,
        BigDecimal weight,
        Boolean isRequired,
        BigDecimal score,
        String itemFeedback
) {
}
