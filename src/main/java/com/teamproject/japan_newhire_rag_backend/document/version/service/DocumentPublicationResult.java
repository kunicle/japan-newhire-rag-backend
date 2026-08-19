package com.teamproject.japan_newhire_rag_backend.document.version.service;

import java.time.LocalDateTime;

public record DocumentPublicationResult(
        Long documentId,
        Long documentVersionId,
        String publicationStatus,
        boolean active,
        LocalDateTime publishedAt,
        Long publishedBy) {
}
