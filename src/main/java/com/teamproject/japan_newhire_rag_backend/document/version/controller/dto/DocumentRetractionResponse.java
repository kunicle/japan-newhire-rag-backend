package com.teamproject.japan_newhire_rag_backend.document.version.controller.dto;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.document.version.service.DocumentRetractionResult;

public record DocumentRetractionResponse(
        Long documentId,
        Long documentVersionId,
        String publicationStatus,
        boolean isActive,
        LocalDateTime retractedAt,
        Long retractedBy) {

    public static DocumentRetractionResponse from(DocumentRetractionResult result) {
        return new DocumentRetractionResponse(
                result.documentId(),
                result.documentVersionId(),
                result.publicationStatus(),
                result.isActive(),
                result.retractedAt(),
                result.retractedBy());
    }
}
