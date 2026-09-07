package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.math.BigDecimal;

public record SelfEvaluationItemDraftRequest(
        Long evaluationItemId,
        BigDecimal score,
        String itemFeedback
) {
}
