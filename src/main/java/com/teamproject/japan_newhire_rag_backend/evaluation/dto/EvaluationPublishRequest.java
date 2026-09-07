package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.util.List;

public record EvaluationPublishRequest(
        String publishReason,
        List<Long> visibleManagerFeedbackIds
) {
}
