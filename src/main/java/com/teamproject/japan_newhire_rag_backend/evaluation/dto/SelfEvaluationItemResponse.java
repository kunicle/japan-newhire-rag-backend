package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.math.BigDecimal;

public record SelfEvaluationItemResponse(
        Long evaluationItemId,
        Integer itemOrder,
        String itemName,
        String itemDescription,
        BigDecimal weight,
        Boolean isRequired,
        Integer minimumScore,
        Integer maximumScore,
        BigDecimal score,
        String itemFeedback
) {
}
