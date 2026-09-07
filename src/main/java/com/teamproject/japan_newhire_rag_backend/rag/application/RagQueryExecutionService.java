package com.teamproject.japan_newhire_rag_backend.rag.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.ExternalAiCallException;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagGenerationOrchestrationResult;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagOrchestrator;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagSearchOrchestrationResult;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagQuestion;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagSearch;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagCitationSnapshot;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagPersistenceService;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagSearchPersistenceItem;

@Service
public class RagQueryExecutionService {

    private static final String FAILURE_TYPE_NO_ACCESSIBLE_DOCUMENT =
            "NO_ACCESSIBLE_DOCUMENT";
    private static final String FAILURE_TYPE_LOW_SIMILARITY = "LOW_SIMILARITY";
    private static final String FAILURE_TYPE_API_ERROR = "API_ERROR";

    private static final String FAILURE_REASON_NO_ACCESSIBLE_DOCUMENT =
            "접근 가능한 규정 문서가 없습니다.";
    private static final String FAILURE_REASON_LOW_SIMILARITY =
            "답변을 생성할 충분한 근거를 찾지 못했습니다.";
    private static final String FAILURE_REASON_API_ERROR =
            "외부 AI 서비스 호출에 실패했습니다.";

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
            ragPersistenceService.markQuestionRejected(
                    ragQuestion,
                    FAILURE_TYPE_NO_ACCESSIBLE_DOCUMENT,
                    FAILURE_REASON_NO_ACCESSIBLE_DOCUMENT);
            return new RagQueryResult(false, null, List.of(), List.of());
        }

        RagSearchPlan plan = planOptional.get();
        ragPersistenceService.markQuestionProcessing(ragQuestion);
        RagSearchOrchestrationResult searchResult;
        try {
            searchResult = ragOrchestrator.search(
                    question,
                    plan.allowedDocumentVersionIds(),
                    plan.providerName(),
                    plan.modelName());
        } catch (ExternalAiCallException exception) {
            ragPersistenceService.markQuestionFailed(
                    ragQuestion,
                    FAILURE_TYPE_API_ERROR,
                    FAILURE_REASON_API_ERROR);
            throw exception.getOriginalFailure();
        }

        List<RagSearchPersistenceItem> persistenceItems = searchResult.verifiedSearchResults().stream()
                .map(item -> new RagSearchPersistenceItem(
                        item.chunkId(),
                        item.documentVersionId(),
                        item.similarityScore()))
                .toList();
        RagSearch ragSearch = ragPersistenceService.persistSearch(
                ragQuestion, plan.aiModelId(), persistenceItems);

        if (!searchResult.hasSufficientEvidence()) {
            ragPersistenceService.markQuestionRejected(
                    ragQuestion,
                    FAILURE_TYPE_LOW_SIMILARITY,
                    FAILURE_REASON_LOW_SIMILARITY);
            return new RagQueryResult(false, null, List.of(), List.of());
        }

        RagGenerationOrchestrationResult generationResult;
        try {
            generationResult = ragOrchestrator.generate(question, searchResult);
        } catch (ExternalAiCallException exception) {
            ragPersistenceService.markQuestionFailed(
                    ragQuestion,
                    FAILURE_TYPE_API_ERROR,
                    FAILURE_REASON_API_ERROR);
            throw exception.getOriginalFailure();
        }
        List<RagCitationSnapshot> citations = ragPersistenceService.persistAnswer(
                ragSearch, generationResult.answer(), generationResult.validCitedChunkIds());
        ragPersistenceService.markQuestionAnswered(ragQuestion);

        return new RagQueryResult(
                true,
                generationResult.answer(),
                generationResult.validCitedChunkIds(),
                citations);
    }
}
