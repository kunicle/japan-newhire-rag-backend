package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.domain.organization.api.EmployeeSummary;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;

public record ManagerEvaluationResponse(
        Long evaluationId,
        Long evaluationCycleId,
        Long evaluationTemplateId,
        Long targetEmployeeId,
        EvaluationStatus evaluationStatus,
        EvaluationCycleStatus currentCycleStatus,
        EmployeeSummary targetEmployee,
        List<ManagerEvaluationItemResponse> items,
        String overallFeedback
) {
}
