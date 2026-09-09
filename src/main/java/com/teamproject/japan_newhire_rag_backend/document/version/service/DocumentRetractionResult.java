package com.teamproject.japan_newhire_rag_backend.document.version.service;

import java.time.LocalDateTime;

public record DocumentRetractionResult(
        Long documentId,
        Long documentVersionId,
        String publicationStatus,
        boolean isActive,
        LocalDateTime retractedAt,
        Long retractedBy) {
}
