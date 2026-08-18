package com.teamproject.japan_newhire_rag_backend.rag.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagGenerationOrchestrationResult;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagOrchestrator;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagSearchOrchestrationResult;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagQuestion;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagSearch;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagPersistenceService;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagSearchPersistenceItem;

@Service
public class RagQueryExecutionService {

    private final RagQueryService ragQueryService;
    private final CurrentUserProvider currentUserProvider;
    private final RagPersistenceService ragPersistenceService;
    private final RagOrchestrator ragOrchestrator;

    public RagQueryExecutionService(
            RagQueryService ragQueryService,
            CurrentUserProvider currentUserProvider,
            RagPersistenceService ragPersistenceService,
            RagOrchestrator ragOrchestrator) {
        this.ragQueryService = ragQueryService;
        this.currentUserProvider = currentUserProvider;
        this.ragPersistenceService = ragPersistenceService;
        this.ragOrchestrator = ragOrchestrator;
    }

    public RagQueryResult execute(String question) {
        Optional<RagSearchPlan> planOptional = ragQueryService.prepareSearch(question);

        CurrentUserContext currentUser = currentUserProvider.getCurrentUser();
        RagQuestion ragQuestion =
                ragPersistenceService.persistQuestion(question, currentUser.appUserId());

        if (planOptional.isEmpty()) {
            return new RagQueryResult(false, null, List.of());
        }

        RagSearchPlan plan = planOptional.get();
        RagSearchOrchestrationResult searchResult = ragOrchestrator.search(
                question,
                plan.allowedDocumentVersionIds(),
                plan.providerName(),
                plan.modelName());

        List<RagSearchPersistenceItem> persistenceItems = searchResult.verifiedSearchResults().stream()
                .map(item -> new RagSearchPersistenceItem(
                        item.chunkId(),
                        item.documentVersionId(),
                        item.similarityScore()))
                .toList();
        RagSearch ragSearch = ragPersistenceService.persistSearch(
                ragQuestion, plan.aiModelId(), persistenceItems);

        if (!searchResult.hasSufficientEvidence()) {
            return new RagQueryResult(false, null, List.of());
        }

        RagGenerationOrchestrationResult generationResult =
                ragOrchestrator.generate(question, searchResult);
        ragPersistenceService.persistAnswer(
                ragSearch, generationResult.answer(), generationResult.validCitedChunkIds());

        return new RagQueryResult(
                true, generationResult.answer(), generationResult.validCitedChunkIds());
    }
}
