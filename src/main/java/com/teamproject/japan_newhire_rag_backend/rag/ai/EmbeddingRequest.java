package com.teamproject.japan_newhire_rag_backend.rag.ai;

public record EmbeddingRequest(
        Long documentChunkId,
        Long documentVersionId,
        String chunkContent,
        String providerName,
        String modelName) {

    public EmbeddingRequest {
        if (documentChunkId == null) {
            throw new IllegalArgumentException("documentChunkId가 없습니다.");
        }
        if (documentVersionId == null) {
            throw new IllegalArgumentException("documentVersionId가 없습니다.");
        }
        if (chunkContent == null || chunkContent.trim().isEmpty()) {
            throw new IllegalArgumentException("chunkContent가 비어 있습니다.");
        }
        if (providerName == null || providerName.trim().isEmpty()) {
            throw new IllegalArgumentException("providerName이 비어 있습니다.");
        }
        if (modelName == null || modelName.trim().isEmpty()) {
            throw new IllegalArgumentException("modelName이 비어 있습니다.");
        }
    }
}
