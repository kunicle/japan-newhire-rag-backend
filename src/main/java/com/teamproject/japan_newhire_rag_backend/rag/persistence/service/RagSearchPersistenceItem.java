package com.teamproject.japan_newhire_rag_backend.rag.persistence.service;

public record RagSearchPersistenceItem(
        Long documentChunkId,
        Long documentVersionId,
        double similarityScore) {
}
