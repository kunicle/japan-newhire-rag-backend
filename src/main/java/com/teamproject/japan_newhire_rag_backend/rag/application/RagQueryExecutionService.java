package com.teamproject.japan_newhire_rag_backend.rag.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.api.SystemErrorRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.api.SystemErrorRecordService;
import com.teamproject.japan_newhire_rag_backend.rag.RagAnswerStatus;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiHttpAttempt;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.ExternalAiCallException;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagGenerationOrchestrationResult;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagOrchestrator;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagSearchOrchestrationResult;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagQuestion;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagSearch;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagCitationSnapshot;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagPersistenceService;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagSearchPersistenceItem;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.ExternalApiCallLogCommand;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.ExternalApiCallLogService;

@Service
public class RagQueryExecutionService {

    private static final String FAILURE_TYPE_NO_ACCESSIBLE_DOCUMENT =
            "NO_ACCESSIBLE_DOCUMENT";
    private static final String FAILURE_TYPE_INSUFFICIENT_EVIDENCE =
            "INSUFFICIENT_EVIDENCE";
    private static final String FAILURE_TYPE_API_ERROR = "API_ERROR";

    private static final String FAILURE_REASON_NO_ACCESSIBLE_DOCUMENT =
            "접근 가능한 규정 문서가 없습니다.";
    private static final String FAILURE_REASON_INSUFFICIENT_EVIDENCE =
            "답변을 생성할 충분한 근거를 찾지 못했습니다.";
    private static final String FAILURE_REASON_API_ERROR =
            "외부 AI 서비스 호출에 실패했습니다.";

    private final RagQueryService ragQueryService;
    private final CurrentUserProvider currentUserProvider;
    private final RagPersistenceService ragPersistenceService;
    private final RagOrchestrator ragOrchestrator;
    private final ExternalApiCallLogService externalApiCallLogService;
    private final SystemErrorRecordService systemErrorRecordService;

    @Autowired
    public RagQueryExecutionService(
            RagQueryService ragQueryService,
            CurrentUserProvider currentUserProvider,
            RagPersistenceService ragPersistenceService,
            RagOrchestrator ragOrchestrator,
            ExternalApiCallLogService externalApiCallLogService,
            SystemErrorRecordService systemErrorRecordService) {
        this.ragQueryService = ragQueryService;
        this.currentUserProvider = currentUserProvider;
        this.ragPersistenceService = ragPersistenceService;
        this.ragOrchestrator = ragOrchestrator;
        this.externalApiCallLogService = externalApiCallLogService;
        this.systemErrorRecordService = systemErrorRecordService;
    }

    public RagQueryExecutionService(
            RagQueryService ragQueryService,
            CurrentUserProvider currentUserProvider,
            RagPersistenceService ragPersistenceService,
            RagOrchestrator ragOrchestrator) {
        this(ragQueryService, currentUserProvider, ragPersistenceService, ragOrchestrator, null, null);
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
            return insufficientEvidenceResult();
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
            recordFailure(ragQuestion, plan.aiModelId(), "RAG_SEARCH", exception);
            ragPersistenceService.markQuestionFailed(
                    ragQuestion,
                    FAILURE_TYPE_API_ERROR,
                    FAILURE_REASON_API_ERROR);
            throw exception.getOriginalFailure();
        }
        recordAttempts(ragQuestion, plan.aiModelId(), "RAG_SEARCH", searchResult.apiAttempts());

        List<RagSearchPersistenceItem> persistenceItems = searchResult.verifiedSearchResults().stream()
                .map(item -> new RagSearchPersistenceItem(
                        item.chunkId(),
                        item.documentVersionId(),
                        item.similarityScore()))
                .toList();
        RagSearch ragSearch = ragPersistenceService.persistSearch(
                ragQuestion, plan.aiModelId(), persistenceItems);

        if (searchResult.verifiedSearchResults().isEmpty()) {
            ragPersistenceService.markQuestionRejected(
                    ragQuestion,
                    FAILURE_TYPE_INSUFFICIENT_EVIDENCE,
                    FAILURE_REASON_INSUFFICIENT_EVIDENCE);
            return insufficientEvidenceResult();
        }

        RagGenerationOrchestrationResult generationResult;
        try {
            generationResult = ragOrchestrator.generate(question, searchResult);
        } catch (ExternalAiCallException exception) {
            recordFailure(ragQuestion, plan.aiModelId(), "RAG_GENERATE", exception);
            ragPersistenceService.markQuestionFailed(
                    ragQuestion,
                    FAILURE_TYPE_API_ERROR,
                    FAILURE_REASON_API_ERROR);
            throw exception.getOriginalFailure();
        }
        recordAttempts(ragQuestion, plan.aiModelId(), "RAG_GENERATE", generationResult.apiAttempts());
        if (generationResult.status() == RagAnswerStatus.INSUFFICIENT_EVIDENCE) {
            ragPersistenceService.markQuestionRejected(
                    ragQuestion,
                    FAILURE_TYPE_INSUFFICIENT_EVIDENCE,
                    FAILURE_REASON_INSUFFICIENT_EVIDENCE);
            return insufficientEvidenceResult();
        }
        List<RagCitationSnapshot> citations = ragPersistenceService.persistAnswer(
                ragSearch, generationResult.answer(), generationResult.validCitedChunkIds());
        ragPersistenceService.markQuestionAnswered(ragQuestion);

        return new RagQueryResult(
                RagAnswerStatus.ANSWERED,
                generationResult.answer(),
                generationResult.validCitedChunkIds(),
                citations);
    }

    private RagQueryResult insufficientEvidenceResult() {
        return new RagQueryResult(
                RagAnswerStatus.INSUFFICIENT_EVIDENCE,
                null,
                List.of(),
                List.of());
    }

    private void recordFailure(
            RagQuestion question, Long aiModelId, String apiType, ExternalAiCallException exception) {
        Long failedLogId = recordAttempts(question, aiModelId, apiType, exception.getAttempts());
        AiHttpAttempt failedAttempt = exception.getAttempts().isEmpty() ? null
                : exception.getAttempts().get(exception.getAttempts().size() - 1);
        if (systemErrorRecordService != null) systemErrorRecordService.record(new SystemErrorRecordCommand(
                question.getCreatedBy(), failedLogId, "LLM_API",
                failedAttempt == null ? "UNEXPECTED_ERROR" : failedAttempt.errorType(),
                failedAttempt == null ? null : failedAttempt.errorCode(),
                failedAttempt == null ? 0 : failedAttempt.attemptNumber() - 1,
                failedAttempt == null ? exception.getOriginalFailure().getClass().getSimpleName()
                        : failedAttempt.errorMessage(),
                failedAttempt == null ? question.getCreatedAt() : failedAttempt.completedAt()));
    }

    private Long recordAttempts(
            RagQuestion question, Long aiModelId, String apiType, List<AiHttpAttempt> attempts) {
        Long lastLogId = null;
        if (externalApiCallLogService == null) return null;
        for (AiHttpAttempt attempt : attempts) {
            lastLogId = externalApiCallLogService.record(new ExternalApiCallLogCommand(
                    aiModelId, question.getRagQuestionId(), null, apiType, attempt.callStatus(),
                    attempt.attemptNumber(), attempt.httpStatusCode(), attempt.errorType(),
                    attempt.errorMessage(), attempt.durationMs(), attempt.requestedAt(), attempt.completedAt()));
        }
        return lastLogId;
    }
}
