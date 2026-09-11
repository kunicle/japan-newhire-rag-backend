package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.time.LocalDateTime;

public record AiHttpAttempt(
        int attemptNumber,
        String callStatus,
        Integer httpStatusCode,
        String errorType,
        String errorCode,
        String errorMessage,
        int durationMs,
        LocalDateTime requestedAt,
        LocalDateTime completedAt) {
}
