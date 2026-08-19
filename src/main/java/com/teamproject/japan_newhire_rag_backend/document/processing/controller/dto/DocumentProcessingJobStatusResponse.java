package com.teamproject.japan_newhire_rag_backend.document.processing.controller.dto;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.document.processing.service.DocumentProcessingJobStatus;

public record DocumentProcessingJobStatusResponse(
        Long documentProcessingJobId,
        Long documentVersionId,
        String status,
        String failureReason,
        LocalDateTime createdAt) {

    public static DocumentProcessingJobStatusResponse from(DocumentProcessingJobStatus status) {
        return new DocumentProcessingJobStatusResponse(
                status.documentProcessingJobId(),
                status.documentVersionId(),
                status.status(),
                status.failureReason(),
                status.createdAt());
    }
}
