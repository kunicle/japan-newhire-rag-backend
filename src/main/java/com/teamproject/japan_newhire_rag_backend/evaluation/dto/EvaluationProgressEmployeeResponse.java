package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import com.teamproject.japan_newhire_rag_backend.domain.organization.api.EmployeeSummary;

public record EvaluationProgressEmployeeResponse(
        EmployeeSummary employee,
        EvaluationProgressDetailResponse selfEvaluation,
        EvaluationProgressDetailResponse managerEvaluation
) {
}
