package com.teamproject.japan_newhire_rag_backend.domain.system.error.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Entity @Table(name = "system_error_log") @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemErrorLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "system_error_log_id") private Long systemErrorLogId;
    @Column(name = "app_user_id") private Long appUserId;
    @Column(name = "external_api_call_log_id") private Long externalApiCallLogId;
    @Column(name = "error_source", nullable = false, length = 50) private String errorSource;
    @Column(name = "error_type", nullable = false, length = 100) private String errorType;
    @Column(name = "error_code", length = 100) private String errorCode;
    @Column(name = "error_status", nullable = false, length = 20) private String errorStatus;
    @Column(name = "retry_count", nullable = false) private int retryCount;
    @Column(name = "error_message", nullable = false, length = 2000) private String errorMessage;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    public static SystemErrorLog open(Long appUserId, Long externalApiCallLogId, String errorSource,
            String errorType, String errorCode, int retryCount, String errorMessage, LocalDateTime occurredAt) {
        SystemErrorLog log = new SystemErrorLog();
        log.appUserId = appUserId; log.externalApiCallLogId = externalApiCallLogId;
        log.errorSource = errorSource; log.errorType = errorType; log.errorCode = errorCode;
        log.errorStatus = "OPEN"; log.retryCount = retryCount; log.errorMessage = errorMessage;
        log.occurredAt = occurredAt; log.createdAt = occurredAt; log.updatedAt = occurredAt;
        return log;
    }
}
