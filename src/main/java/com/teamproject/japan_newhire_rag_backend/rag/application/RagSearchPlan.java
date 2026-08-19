package com.teamproject.japan_newhire_rag_backend.rag.application;

import java.util.Set;

public record RagSearchPlan(
        Long aiModelId,
        Set<Long> allowedDocumentVersionIds,
        String providerName,
        String modelName) {

    public RagSearchPlan {
        allowedDocumentVersionIds = Set.copyOf(allowedDocumentVersionIds);
    }
}
