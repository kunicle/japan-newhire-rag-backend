package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;

public record EvaluationCycleResponse(
        Long evaluationCycleId,
        String cycleName,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate plannedPublishDate,
        EvaluationCycleStatus cycleStatus,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static EvaluationCycleResponse from(
            EvaluationCycle cycle,
            EvaluationCycleStatus currentStatus
    ) {
        return new EvaluationCycleResponse(
                cycle.getEvaluationCycleId(),
                cycle.getCycleName(),
                cycle.getStartDate(),
                cycle.getEndDate(),
                cycle.getPlannedPublishDate(),
                currentStatus,
                cycle.getCreatedBy(),
                cycle.getCreatedAt(),
                cycle.getUpdatedAt());
    }
}
