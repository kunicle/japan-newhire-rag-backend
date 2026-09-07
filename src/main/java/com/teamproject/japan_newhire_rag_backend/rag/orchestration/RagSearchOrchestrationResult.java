package com.teamproject.japan_newhire_rag_backend.rag.orchestration;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResultItem;

public record RagSearchOrchestrationResult(
        boolean hasSufficientEvidence,
        List<AiRagSearchResultItem> verifiedSearchResults) {

    public RagSearchOrchestrationResult {
        verifiedSearchResults = List.copyOf(verifiedSearchResults);
    }
}
