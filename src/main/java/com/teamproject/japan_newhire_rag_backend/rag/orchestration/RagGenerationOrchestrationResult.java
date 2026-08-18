package com.teamproject.japan_newhire_rag_backend.rag.orchestration;

import java.util.List;

public record RagGenerationOrchestrationResult(
        String answer,
        List<Long> validCitedChunkIds) {

    public RagGenerationOrchestrationResult {
        validCitedChunkIds = List.copyOf(validCitedChunkIds);
    }
}
