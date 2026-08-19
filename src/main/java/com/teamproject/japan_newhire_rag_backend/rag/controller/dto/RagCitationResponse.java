package com.teamproject.japan_newhire_rag_backend.rag.controller.dto;

import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagCitationSnapshot;

public record RagCitationResponse(
        Long documentChunkId,
        String documentName,
        String versionName,
        String articleNumber,
        String citedText) {

    public static RagCitationResponse from(RagCitationSnapshot snapshot) {
        return new RagCitationResponse(
                snapshot.documentChunkId(),
                snapshot.documentName(),
                snapshot.versionName(),
                snapshot.articleNumber(),
                snapshot.citedText());
    }
}
