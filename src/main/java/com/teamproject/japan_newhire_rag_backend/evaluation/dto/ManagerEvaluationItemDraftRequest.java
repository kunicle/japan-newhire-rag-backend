package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.math.BigDecimal;

public record ManagerEvaluationItemDraftRequest(
        Long evaluationItemId,
        BigDecimal score,
        String itemFeedback
) {
}
