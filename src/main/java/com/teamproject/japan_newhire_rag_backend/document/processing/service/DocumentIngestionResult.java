package com.teamproject.japan_newhire_rag_backend.document.processing.service;

public record DocumentIngestionResult(
        Long documentId,
        Long documentVersionId,
        Long documentProcessingJobId,
        String processingStatus) {
}
