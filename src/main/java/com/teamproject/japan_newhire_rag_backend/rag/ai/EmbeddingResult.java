package com.teamproject.japan_newhire_rag_backend.rag.ai;

public record EmbeddingResult(String vectorReference, int embeddingDimension) {

    public EmbeddingResult {
        if (vectorReference == null || vectorReference.trim().isEmpty()) {
            throw new IllegalArgumentException("vectorReference가 비어 있습니다.");
        }
        if (embeddingDimension <= 0) {
            throw new IllegalArgumentException("embeddingDimension은 0보다 커야 합니다.");
        }
    }
}
