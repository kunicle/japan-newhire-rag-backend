package com.teamproject.japan_newhire_rag_backend.rag.application;

import java.util.List;
import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.document.access.service.DocumentSearchScopeService;
import com.teamproject.japan_newhire_rag_backend.rag.model.EmbeddingModelSelection;
import com.teamproject.japan_newhire_rag_backend.rag.model.service.EmbeddingModelSelectionService;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagOrchestrationResult;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagOrchestrator;

public class RagQueryService {

    private final DocumentSearchScopeService documentSearchScopeService;
    private final EmbeddingModelSelectionService embeddingModelSelectionService;
    private final RagOrchestrator ragOrchestrator;

    public RagQueryService(
            DocumentSearchScopeService documentSearchScopeService,
            EmbeddingModelSelectionService embeddingModelSelectionService,
            RagOrchestrator ragOrchestrator) {
        this.documentSearchScopeService = documentSearchScopeService;
        this.embeddingModelSelectionService = embeddingModelSelectionService;
        this.ragOrchestrator = ragOrchestrator;
    }

    public RagOrchestrationResult handle(String question) {
        validateQuestion(question);
        Set<Long> allowedDocumentVersionIds =
                documentSearchScopeService.findAllowedDocumentVersionIds();

        if (allowedDocumentVersionIds.isEmpty()) {
            return new RagOrchestrationResult(false, null, List.of());
        }

        EmbeddingModelSelection modelSelection =
                embeddingModelSelectionService.selectDefaultEmbeddingModel();
        return ragOrchestrator.handle(
                question,
                allowedDocumentVersionIds,
                modelSelection.providerName(),
                modelSelection.modelName());
    }

    private void validateQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("질문이 비어 있습니다.");
        }
    }
}
