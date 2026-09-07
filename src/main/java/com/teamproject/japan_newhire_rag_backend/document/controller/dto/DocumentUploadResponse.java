package com.teamproject.japan_newhire_rag_backend.document.controller.dto;

import com.teamproject.japan_newhire_rag_backend.document.processing.service.DocumentIngestionResult;

public record DocumentUploadResponse(
        Long documentId,
        Long documentVersionId,
        Long documentProcessingJobId,
        String processingStatus) {

    public static DocumentUploadResponse from(DocumentIngestionResult result) {
        return new DocumentUploadResponse(
                result.documentId(),
                result.documentVersionId(),
                result.documentProcessingJobId(),
                result.processingStatus());
    }
}
