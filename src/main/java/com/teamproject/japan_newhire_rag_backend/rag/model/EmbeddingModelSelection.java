package com.teamproject.japan_newhire_rag_backend.rag.model;

public record EmbeddingModelSelection(
        Long aiModelId,
        String providerName,
        String modelName,
        Integer embeddingDimension) {

    public EmbeddingModelSelection {
        if (aiModelId == null) {
            throw new IllegalArgumentException("AI 모델 ID가 없습니다.");
        }
        if (providerName == null || providerName.trim().isEmpty()) {
            throw new IllegalArgumentException("provider 이름이 비어 있습니다.");
        }
        if (modelName == null || modelName.trim().isEmpty()) {
            throw new IllegalArgumentException("모델 이름이 비어 있습니다.");
        }
    }
}
