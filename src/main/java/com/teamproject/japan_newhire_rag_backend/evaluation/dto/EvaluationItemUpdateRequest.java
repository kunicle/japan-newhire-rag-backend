package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.math.BigDecimal;

public record EvaluationItemUpdateRequest(
        String itemName,
        String itemDescription,
        Integer itemOrder,
        BigDecimal weight,
        Boolean isRequired,
        Integer minimumScore,
        Integer maximumScore
) {
}
