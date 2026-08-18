package com.teamproject.japan_newhire_rag_backend.rag.application;

import java.util.List;

public record RagQueryResult(
        boolean hasSufficientEvidence,
        String answer,
        List<Long> validCitedChunkIds) {
}
