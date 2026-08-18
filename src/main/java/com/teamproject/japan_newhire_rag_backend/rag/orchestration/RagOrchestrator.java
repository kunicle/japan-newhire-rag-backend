package com.teamproject.japan_newhire_rag_backend.rag.orchestration;

import java.util.List;
import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagGenerateRequest;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagGenerateResponse;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchRequest;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResponse;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResultItem;
import com.teamproject.japan_newhire_rag_backend.rag.citation.CitationValidator;
import com.teamproject.japan_newhire_rag_backend.rag.evidence.EvidenceThresholdChecker;
import com.teamproject.japan_newhire_rag_backend.rag.search.SearchResultVerifier;

public class RagOrchestrator {

    private final AiRagClient aiRagClient;
    private final SearchResultVerifier searchResultVerifier;
    private final EvidenceThresholdChecker evidenceThresholdChecker;
    private final CitationValidator citationValidator;
    private final double evidenceThreshold;

    public RagOrchestrator(
            AiRagClient aiRagClient,
            SearchResultVerifier searchResultVerifier,
            EvidenceThresholdChecker evidenceThresholdChecker,
            CitationValidator citationValidator,
            double evidenceThreshold) {
        this.aiRagClient = aiRagClient;
        this.searchResultVerifier = searchResultVerifier;
        this.evidenceThresholdChecker = evidenceThresholdChecker;
        this.citationValidator = citationValidator;
        this.evidenceThreshold = evidenceThreshold;
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
        AiRagSearchResponse searchResponse = aiRagClient.search(searchRequest);
        List<AiRagSearchResultItem> verifiedSearchResults =
                searchResultVerifier.filterByAllowedDocumentVersions(
                        searchResponse.searchResults(),
                        allowedDocumentVersionIds);

        return new RagSearchOrchestrationResult(
                hasSufficientEvidence(verifiedSearchResults),
                verifiedSearchResults);
    }

    public RagGenerationOrchestrationResult generate(
            String question,
            RagSearchOrchestrationResult searchResult) {
        if (question == null || searchResult == null) {
            throw new IllegalArgumentException("RAG 생성 입력은 null일 수 없습니다.");
        }
        if (!searchResult.hasSufficientEvidence()
                || !hasSufficientEvidence(searchResult.verifiedSearchResults())) {
            throw new IllegalStateException("증거가 불충분한 상태에서는 답변을 생성할 수 없습니다.");
        }

        AiRagGenerateRequest generateRequest =
                new AiRagGenerateRequest(question, searchResult.verifiedSearchResults());
        AiRagGenerateResponse generateResponse = aiRagClient.generate(generateRequest);
        List<Long> validCitedChunkIds = citationValidator.filterValidCitations(
                generateResponse.citedChunkIds(),
                searchResult.verifiedSearchResults());

        return new RagGenerationOrchestrationResult(generateResponse.answer(), validCitedChunkIds);
    }

    private boolean hasSufficientEvidence(List<AiRagSearchResultItem> verifiedSearchResults) {
        List<Double> similarityScores = verifiedSearchResults.stream()
                .map(AiRagSearchResultItem::similarityScore)
                .toList();
        return evidenceThresholdChecker.hasSufficientEvidence(similarityScores, evidenceThreshold);
    }
}
