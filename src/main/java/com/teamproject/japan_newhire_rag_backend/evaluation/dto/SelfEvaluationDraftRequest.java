package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.util.List;

public record SelfEvaluationDraftRequest(
        List<SelfEvaluationItemDraftRequest> items,
        String overallFeedback
) {
}
