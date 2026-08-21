package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;

public record EvaluationPublishResponse(
        Long cycleId,
        Long targetEmployeeId,
        Long selfEvaluationId,
        Long managerEvaluationId,
        EvaluationStatus selfStatus,
        EvaluationStatus managerStatus,
        LocalDateTime publishedAt,
        List<Long> visibleManagerFeedbackIds,
        boolean idempotent
) {
}
