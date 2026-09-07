package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import com.teamproject.japan_newhire_rag_backend.domain.organization.api.EmployeeSummary;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;

public record ManagerEvaluationSummaryResponse(
        Long evaluationId,
        Long evaluationCycleId,
        EvaluationStatus evaluationStatus,
        EvaluationCycleStatus currentCycleStatus,
        EmployeeSummary targetEmployee
) {
}
