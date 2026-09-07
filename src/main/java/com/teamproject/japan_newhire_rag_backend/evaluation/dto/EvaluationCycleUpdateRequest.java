package com.teamproject.japan_newhire_rag_backend.evaluation.dto;

import java.time.LocalDate;

public record EvaluationCycleUpdateRequest(
        String cycleName,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate plannedPublishDate
) {
}
