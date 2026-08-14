package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.time.LocalDate;
import java.util.List;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;

public record EvaluationProgressResponse(
        Long cycleId,
        String cycleName,
        LocalDate startDate,
        LocalDate endDate,
        EvaluationCycleStatus currentCycleStatus,
        long totalTargetCount,
        EvaluationProgressSummary selfSummary,
        EvaluationProgressSummary managerSummary,
        List<EvaluationProgressEmployeeResponse> employees
) {
}
