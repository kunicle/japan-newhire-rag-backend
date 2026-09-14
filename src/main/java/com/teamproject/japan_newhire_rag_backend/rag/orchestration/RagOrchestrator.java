package com.teamproject.japan_newhire_rag_backend.rag.orchestration;

import java.util.List;
import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.rag.RagAnswerStatus;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagCallMetadataClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiHttpExecution;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagGenerateRequest;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagGenerateResponse;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchRequest;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResponse;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResultItem;
import com.teamproject.japan_newhire_rag_backend.rag.citation.CitationValidator;
import com.teamproject.japan_newhire_rag_backend.rag.search.SearchResultVerifier;

public class RagOrchestrator {

    private final AiRagClient aiRagClient;
    private final SearchResultVerifier searchResultVerifier;
    private final CitationValidator citationValidator;

    public RagOrchestrator(
            AiRagClient aiRagClient,
            SearchResultVerifier searchResultVerifier,
            CitationValidator citationValidator) {
        this.aiRagClient = aiRagClient;
        this.searchResultVerifier = searchResultVerifier;
        this.citationValidator = citationValidator;
    }

    public RagSearchOrchestrationResult search(
            String question,
            Set<Long> allowedDocumentVersionIds,
            String providerName,
            String modelName) {
        if (question == null
                || allowedDocumentVersionIds == null
                || providerName == null
                || modelName == null) {
            throw new IllegalArgumentException("RAG 검색 입력은 null일 수 없습니다.");
        }

        AiRagSearchRequest searchRequest =
                new AiRagSearchRequest(
                        question,
                        List.copyOf(allowedDocumentVersionIds),
                        providerName,
                        modelName);
        AiRagSearchResponse searchResponse;
        List<com.teamproject.japan_newhire_rag_backend.rag.ai.AiHttpAttempt> attempts = List.of();
        try {
            if (aiRagClient instanceof AiRagCallMetadataClient metadataClient) {
                AiHttpExecution<AiRagSearchResponse> execution = metadataClient.searchWithMetadata(searchRequest);
                searchResponse = execution.result();
                attempts = execution.attempts();
            } else {
                searchResponse = aiRagClient.search(searchRequest);
            }
        } catch (RuntimeException exception) {
            throw new ExternalAiCallException(exception);
        }
        List<AiRagSearchResultItem> verifiedSearchResults =
                searchResultVerifier.filterByAllowedDocumentVersions(
                        searchResponse.searchResults(),
                        allowedDocumentVersionIds);

        return new RagSearchOrchestrationResult(
                verifiedSearchResults,
                attempts);
    }

    public RagGenerationOrchestrationResult generate(
            String question,
            RagSearchOrchestrationResult searchResult) {
        if (question == null || searchResult == null) {
            throw new IllegalArgumentException("RAG 생성 입력은 null일 수 없습니다.");
        }
        if (searchResult.verifiedSearchResults().isEmpty()) {
            throw new IllegalStateException("증거가 불충분한 상태에서는 답변을 생성할 수 없습니다.");
        }

        AiRagGenerateRequest generateRequest =
                new AiRagGenerateRequest(question, searchResult.verifiedSearchResults());
        AiRagGenerateResponse generateResponse;
        List<com.teamproject.japan_newhire_rag_backend.rag.ai.AiHttpAttempt> attempts = List.of();
        try {
            if (aiRagClient instanceof AiRagCallMetadataClient metadataClient) {
                AiHttpExecution<AiRagGenerateResponse> execution = metadataClient.generateWithMetadata(generateRequest);
                generateResponse = execution.result();
                attempts = execution.attempts();
            } else {
                generateResponse = aiRagClient.generate(generateRequest);
            }
        } catch (RuntimeException exception) {
            throw new ExternalAiCallException(exception);
        }
        if (generateResponse.status() == RagAnswerStatus.INSUFFICIENT_EVIDENCE) {
            return new RagGenerationOrchestrationResult(
                    RagAnswerStatus.INSUFFICIENT_EVIDENCE, null, List.of(), attempts);
        }

        List<Long> validCitedChunkIds = citationValidator.filterValidCitations(
                generateResponse.citedChunkIds(),
                searchResult.verifiedSearchResults());
        if (validCitedChunkIds.isEmpty()) {
            return new RagGenerationOrchestrationResult(
                    RagAnswerStatus.INSUFFICIENT_EVIDENCE, null, List.of(), attempts);
        }

        return new RagGenerationOrchestrationResult(
                RagAnswerStatus.ANSWERED,
                generateResponse.answer(),
                validCitedChunkIds,
                attempts);
    }
}
