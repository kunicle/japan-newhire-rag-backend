package com.teamproject.japan_newhire_rag_backend.document.version.controller.dto;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.document.version.service.DocumentPublicationResult;

public record DocumentPublicationResponse(
        Long documentId,
        Long documentVersionId,
        String publicationStatus,
        boolean active,
        LocalDateTime publishedAt,
        Long publishedBy) {

    public static DocumentPublicationResponse from(DocumentPublicationResult result) {
        return new DocumentPublicationResponse(
                result.documentId(),
                result.documentVersionId(),
                result.publicationStatus(),
                result.active(),
                result.publishedAt(),
                result.publishedBy());
    }
}
