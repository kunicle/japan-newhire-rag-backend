package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.time.LocalDate;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;

public record MyEvaluationSummaryResponse(
        Long evaluationId,
        Long evaluationCycleId,
        String cycleName,
        LocalDate cycleStartDate,
        LocalDate cycleEndDate,
        EvaluationStatus evaluationStatus,
        EvaluationCycleStatus currentCycleStatus
) {
}
