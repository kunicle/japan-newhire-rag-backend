package com.teamproject.japan_newhire_rag_backend.domain.system.error.api;

import java.time.LocalDateTime;

public record SystemErrorRecordCommand(
        Long appUserId, Long externalApiCallLogId, String errorSource, String errorType,
        String errorCode, int retryCount, String errorMessage, LocalDateTime occurredAt) {
}
