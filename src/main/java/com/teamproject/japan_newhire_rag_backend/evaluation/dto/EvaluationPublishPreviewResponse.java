package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.util.List;

public record EvaluationPublishPreviewResponse(
        Long evaluationCycleId,
        Long targetEmployeeId,
        Long selfEvaluationId,
        Long managerEvaluationId,
        List<EvaluationPublishPreviewFeedbackResponse> managerFeedbacks
) {
    public EvaluationPublishPreviewResponse {
        managerFeedbacks = List.copyOf(managerFeedbacks);
    }
}
