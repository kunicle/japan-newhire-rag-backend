package com.teamproject.japan_newhire_rag_backend.rag.orchestration;

import java.util.List;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiHttpAttempt;

public record RagGenerationOrchestrationResult(
        String answer,
        List<Long> validCitedChunkIds,
        List<AiHttpAttempt> apiAttempts) {

    public RagGenerationOrchestrationResult(String answer, List<Long> validCitedChunkIds) {
        this(answer, validCitedChunkIds, List.of());
    }

    public RagGenerationOrchestrationResult {
        validCitedChunkIds = List.copyOf(validCitedChunkIds);
        apiAttempts = List.copyOf(apiAttempts);
    }
}
