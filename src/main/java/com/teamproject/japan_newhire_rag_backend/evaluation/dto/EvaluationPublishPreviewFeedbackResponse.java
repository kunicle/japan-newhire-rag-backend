package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import com.teamproject.japan_newhire_rag_backend.evaluation.FeedbackType;

public record EvaluationPublishPreviewFeedbackResponse(
        Long evaluationFeedbackId,
        Long evaluationItemId,
        FeedbackType feedbackType,
        String feedbackContent,
        Boolean isVisibleToEmployee
) {
}
