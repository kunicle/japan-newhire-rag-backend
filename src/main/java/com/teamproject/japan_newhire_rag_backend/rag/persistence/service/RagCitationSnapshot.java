package com.teamproject.japan_newhire_rag_backend.rag.persistence.service;

public record RagCitationSnapshot(
        Long documentChunkId,
        String documentName,
        String versionName,
        String articleNumber,
        String citedText) {
}
