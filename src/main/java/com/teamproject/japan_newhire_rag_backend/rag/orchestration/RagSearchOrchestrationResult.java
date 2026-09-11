package com.teamproject.japan_newhire_rag_backend.rag.orchestration;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResultItem;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiHttpAttempt;

public record RagSearchOrchestrationResult(
        boolean hasSufficientEvidence,
        List<AiRagSearchResultItem> verifiedSearchResults,
        List<AiHttpAttempt> apiAttempts) {

    public RagSearchOrchestrationResult(
            boolean hasSufficientEvidence,
            List<AiRagSearchResultItem> verifiedSearchResults) {
        this(hasSufficientEvidence, verifiedSearchResults, List.of());
    }

    public RagSearchOrchestrationResult {
        verifiedSearchResults = List.copyOf(verifiedSearchResults);
        apiAttempts = List.copyOf(apiAttempts);
    }
}
