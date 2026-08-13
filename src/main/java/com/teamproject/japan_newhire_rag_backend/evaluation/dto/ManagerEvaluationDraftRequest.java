package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.util.List;

public record ManagerEvaluationDraftRequest(
        List<ManagerEvaluationItemDraftRequest> items,
        String overallFeedback
) {
}
