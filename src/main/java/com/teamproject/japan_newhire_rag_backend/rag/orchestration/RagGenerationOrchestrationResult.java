package com.teamproject.japan_newhire_rag_backend.rag.orchestration;

import java.util.List;
import com.teamproject.japan_newhire_rag_backend.rag.RagAnswerStatus;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiHttpAttempt;

public record RagGenerationOrchestrationResult(
        RagAnswerStatus status,
        String answer,
        List<Long> validCitedChunkIds,
        List<AiHttpAttempt> apiAttempts) {

    public RagGenerationOrchestrationResult(
            RagAnswerStatus status, String answer, List<Long> validCitedChunkIds) {
        this(status, answer, validCitedChunkIds, List.of());
    }

    public RagGenerationOrchestrationResult {
        validCitedChunkIds = List.copyOf(validCitedChunkIds);
        apiAttempts = List.copyOf(apiAttempts);
    }
}
