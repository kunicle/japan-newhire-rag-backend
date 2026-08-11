package com.teamproject.japan_newhire_rag_backend.rag.orchestration;

import java.util.List;

public record RagOrchestrationResult(
        boolean hasSufficientEvidence,
        String answer,
        List<Long> validCitedChunkIds) {
}
