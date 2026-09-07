package com.teamproject.japan_newhire_rag_backend.rag.application;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagCitationSnapshot;

public record RagQueryResult(
        boolean hasSufficientEvidence,
        String answer,
        List<Long> validCitedChunkIds,
        List<RagCitationSnapshot> citations) {
}
