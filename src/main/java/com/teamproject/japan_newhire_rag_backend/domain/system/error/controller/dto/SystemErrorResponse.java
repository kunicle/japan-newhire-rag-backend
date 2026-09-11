package com.teamproject.japan_newhire_rag_backend.domain.system.error.controller.dto;

import java.time.LocalDateTime;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.entity.SystemErrorLog;

public record SystemErrorResponse(Long systemErrorLogId, Long appUserId, Long externalApiCallLogId,
        String errorSource, String errorType, String errorCode, String errorStatus, int retryCount,
        String errorMessage, LocalDateTime occurredAt, LocalDateTime resolvedAt) {
    public static SystemErrorResponse from(SystemErrorLog log) {
        return new SystemErrorResponse(log.getSystemErrorLogId(), log.getAppUserId(),
                log.getExternalApiCallLogId(), log.getErrorSource(), log.getErrorType(), log.getErrorCode(),
                log.getErrorStatus(), log.getRetryCount(), log.getErrorMessage(), log.getOccurredAt(), log.getResolvedAt());
    }
}
