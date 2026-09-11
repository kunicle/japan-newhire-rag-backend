package com.teamproject.japan_newhire_rag_backend.rag.persistence.entity;

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

@Getter
@Entity
@Table(name = "external_api_call_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalApiCallLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "external_api_call_log_id") private Long externalApiCallLogId;
    @Column(name = "ai_model_id", nullable = false) private Long aiModelId;
    @Column(name = "rag_question_id") private Long ragQuestionId;
    @Column(name = "document_processing_job_id") private Long documentProcessingJobId;
    @Column(name = "api_type", nullable = false, length = 20) private String apiType;
    @Column(name = "call_status", nullable = false, length = 20) private String callStatus;
    @Column(name = "attempt_number", nullable = false) private int attemptNumber;
    @Column(name = "http_status_code") private Integer httpStatusCode;
    @Column(name = "error_type", length = 100) private String errorType;
    @Column(name = "error_message", length = 1000) private String errorMessage;
    @Column(name = "duration_ms") private Integer durationMs;
    @Column(name = "requested_at", nullable = false) private LocalDateTime requestedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    public static ExternalApiCallLog record(
            Long aiModelId, Long ragQuestionId, Long documentProcessingJobId,
            String apiType, String callStatus, int attemptNumber, Integer httpStatusCode,
            String errorType, String errorMessage, Integer durationMs,
            LocalDateTime requestedAt, LocalDateTime completedAt) {
        ExternalApiCallLog log = new ExternalApiCallLog();
        log.aiModelId = aiModelId; log.ragQuestionId = ragQuestionId;
        log.documentProcessingJobId = documentProcessingJobId; log.apiType = apiType;
        log.callStatus = callStatus; log.attemptNumber = attemptNumber;
        log.httpStatusCode = httpStatusCode; log.errorType = errorType;
        log.errorMessage = errorMessage; log.durationMs = durationMs;
        log.requestedAt = requestedAt; log.completedAt = completedAt; log.createdAt = requestedAt;
        return log;
    }
}
