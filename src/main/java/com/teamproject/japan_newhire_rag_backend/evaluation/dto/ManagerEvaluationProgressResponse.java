package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.math.BigDecimal;
import java.util.List;

public record ManagerEvaluationProgressResponse(
        Long cycleId,
        String cycleName,
        long totalEmployees,
        long completedEmployees,
        BigDecimal completionRate,
        long selfCompletedCount,
        long managerCompletedCount,
        List<ManagerEvaluationProgressEmployeeResponse> employees
) {
}
