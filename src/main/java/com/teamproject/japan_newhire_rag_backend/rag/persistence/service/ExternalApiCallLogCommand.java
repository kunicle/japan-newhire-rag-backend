package com.teamproject.japan_newhire_rag_backend.rag.persistence.service;

import java.time.LocalDateTime;

public record ExternalApiCallLogCommand(
        Long aiModelId, Long ragQuestionId, Long documentProcessingJobId,
        String apiType, String callStatus, int attemptNumber, Integer httpStatusCode,
        String errorType, String errorMessage, Integer durationMs,
        LocalDateTime requestedAt, LocalDateTime completedAt) {
}
