package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import java.time.LocalDateTime;

public record DocumentProcessingJobStatus(
        Long documentProcessingJobId,
        Long documentVersionId,
        String status,
        String failureReason,
        LocalDateTime createdAt) {
}
