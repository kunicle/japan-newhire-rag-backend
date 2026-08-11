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

    public RagOrchestrationResult handle(String question, Set<Long> allowedDocumentIds) {
        if (question == null || allowedDocumentIds == null) {
            throw new IllegalArgumentException("question과 allowedDocumentIds는 null일 수 없습니다.");
        }

        AiRagSearchRequest searchRequest =
                new AiRagSearchRequest(question, List.copyOf(allowedDocumentIds));
        AiRagSearchResponse searchResponse = aiRagClient.search(searchRequest);
        List<AiRagSearchResultItem> verifiedSearchResults =
                searchResultVerifier.filterByAllowedDocuments(
                        searchResponse.searchResults(),
                        allowedDocumentIds);

        List<Double> similarityScores = verifiedSearchResults.stream()
                .map(AiRagSearchResultItem::similarityScore)
                .toList();
        boolean hasSufficientEvidence =
                evidenceThresholdChecker.hasSufficientEvidence(
                        similarityScores,
                        evidenceThreshold);

        if (!hasSufficientEvidence) {
            return new RagOrchestrationResult(false, null, List.of());
        }

        AiRagGenerateRequest generateRequest =
                new AiRagGenerateRequest(question, verifiedSearchResults);
        AiRagGenerateResponse generateResponse = aiRagClient.generate(generateRequest);
        List<Long> validCitedChunkIds = citationValidator.filterValidCitations(
                generateResponse.citedChunkIds(),
                verifiedSearchResults);

        return new RagOrchestrationResult(
                true,
                generateResponse.answer(),
                validCitedChunkIds);
    }
}
