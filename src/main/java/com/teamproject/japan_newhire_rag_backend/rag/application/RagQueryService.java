package com.teamproject.japan_newhire_rag_backend.rag.application;

import java.util.Optional;
import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.document.access.service.DocumentSearchScopeService;
import com.teamproject.japan_newhire_rag_backend.rag.model.EmbeddingModelSelection;
import com.teamproject.japan_newhire_rag_backend.rag.model.service.EmbeddingModelSelectionService;

public class RagQueryService {

    private final DocumentSearchScopeService documentSearchScopeService;
    private final EmbeddingModelSelectionService embeddingModelSelectionService;

    public RagQueryService(
            DocumentSearchScopeService documentSearchScopeService,
            EmbeddingModelSelectionService embeddingModelSelectionService) {
        this.documentSearchScopeService = documentSearchScopeService;
        this.embeddingModelSelectionService = embeddingModelSelectionService;
    }

    public Optional<RagSearchPlan> prepareSearch(String question) {
        validateQuestion(question);
        Set<Long> allowedDocumentVersionIds =
                documentSearchScopeService.findAllowedDocumentVersionIds();

        if (allowedDocumentVersionIds.isEmpty()) {
            return Optional.empty();
        }

        EmbeddingModelSelection modelSelection =
                embeddingModelSelectionService.selectDefaultEmbeddingModel();
        return Optional.of(new RagSearchPlan(
                modelSelection.aiModelId(),
                allowedDocumentVersionIds,
                modelSelection.providerName(),
                modelSelection.modelName()));
    }

    private void validateQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("질문이 비어 있습니다.");
        }
    }
}
