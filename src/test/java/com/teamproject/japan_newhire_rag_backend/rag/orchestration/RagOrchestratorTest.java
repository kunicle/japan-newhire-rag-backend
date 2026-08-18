package com.teamproject.japan_newhire_rag_backend.rag.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagGenerateResponse;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResponse;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResultItem;
import com.teamproject.japan_newhire_rag_backend.rag.ai.FakeAiRagClient;
import com.teamproject.japan_newhire_rag_backend.rag.citation.CitationValidator;
import com.teamproject.japan_newhire_rag_backend.rag.evidence.EvidenceThresholdChecker;
import com.teamproject.japan_newhire_rag_backend.rag.search.SearchResultVerifier;

class RagOrchestratorTest {

    private static final double EVIDENCE_THRESHOLD = 0.7;
    private static final String QUESTION = "휴가 규정";
    private static final String PROVIDER_NAME = "provider-a";
    private static final String MODEL_NAME = "model-a";

    private final FakeAiRagClient aiRagClient = new FakeAiRagClient();
    private final RagOrchestrator orchestrator = new RagOrchestrator(
            aiRagClient,
            new SearchResultVerifier(),
            new EvidenceThresholdChecker(),
            new CitationValidator(),
            EVIDENCE_THRESHOLD);

    @Test
    void rejectsNullRequiredSearchArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.search(null, Set.of(1L), PROVIDER_NAME, MODEL_NAME));
        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.search(QUESTION, null, PROVIDER_NAME, MODEL_NAME));
        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.search(QUESTION, Set.of(1L), null, MODEL_NAME));
        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.search(QUESTION, Set.of(1L), PROVIDER_NAME, null));
    }

    @Test
    void rejectsNullRequiredGenerateArguments() {
        RagSearchOrchestrationResult searchResult =
                new RagSearchOrchestrationResult(true, List.of(createSearchResult(1L, 10L, 0.8)));

        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.generate(null, searchResult));
        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.generate(QUESTION, null));
    }

    @Test
    void rejectsInsufficientEvidenceSearchResultWithoutCallingGenerate() {
        RagSearchOrchestrationResult insufficientResult =
                new RagSearchOrchestrationResult(false, List.of(createSearchResult(1L, 10L, 0.8)));

        assertThrows(IllegalStateException.class,
                () -> orchestrator.generate(QUESTION, insufficientResult));

        assertEquals(0, aiRagClient.getGenerateCallCount());
    }

    @Test
    void rejectsForgedSufficientFlagWhenActualEvidenceBelowThreshold() {
        RagSearchOrchestrationResult forged =
                new RagSearchOrchestrationResult(true, List.of(createSearchResult(1L, 10L, 0.5)));

        assertThrows(IllegalStateException.class,
                () -> orchestrator.generate(QUESTION, forged));

        assertEquals(0, aiRagClient.getGenerateCallCount());
    }

    @Test
    void doesNotGenerateWhenSearchResultsAreEmpty() {
        aiRagClient.registerSearchResponse(QUESTION, new AiRagSearchResponse(List.of()));

        RagSearchOrchestrationResult result = search(Set.of(1L));

        assertFalse(result.hasSufficientEvidence());
        assertTrue(result.verifiedSearchResults().isEmpty());
        assertEquals(0, aiRagClient.getGenerateCallCount());
    }

    @Test
    void doesNotGenerateWhenEverySimilarityIsBelowThreshold() {
        List<AiRagSearchResultItem> searchResults = List.of(
                createSearchResult(1L, 10L, 0.6),
                createSearchResult(1L, 20L, 0.5));
        aiRagClient.registerSearchResponse(QUESTION, new AiRagSearchResponse(searchResults));

        RagSearchOrchestrationResult result = search(Set.of(1L));

        assertFalse(result.hasSufficientEvidence());
        assertEquals(searchResults, result.verifiedSearchResults());
        assertEquals(0, aiRagClient.getGenerateCallCount());
    }

    @Test
    void doesNotGenerateWhenEverySearchResultIsFilteredOut() {
        aiRagClient.registerSearchResponse(QUESTION, new AiRagSearchResponse(List.of(
                createSearchResult(2L, 10L, 0.9))));

        RagSearchOrchestrationResult result = search(Set.of(1L));

        assertFalse(result.hasSufficientEvidence());
        assertTrue(result.verifiedSearchResults().isEmpty());
        assertEquals(0, aiRagClient.getGenerateCallCount());
    }

    @Test
    void generatesWithFilteredEvidenceAndRemovesInvalidCitations() {
        AiRagSearchResultItem allowedResult = createSearchResult(1L, 10L, 0.8);
        AiRagSearchResultItem disallowedResult = createSearchResult(2L, 20L, 0.9);
        aiRagClient.registerSearchResponse(QUESTION, new AiRagSearchResponse(
                List.of(allowedResult, disallowedResult)));
        AiRagGenerateResponse generateResponse =
                new AiRagGenerateResponse("휴가는 연 15일입니다.", List.of(10L, 20L, 99L));
        aiRagClient.registerGenerateResponse(QUESTION, generateResponse);

        RagSearchOrchestrationResult searchResult = search(Set.of(1L));

        assertTrue(searchResult.hasSufficientEvidence());
        assertEquals(List.of(allowedResult), searchResult.verifiedSearchResults());
        assertEquals(PROVIDER_NAME, aiRagClient.getLastSearchRequest().providerName());
        assertEquals(MODEL_NAME, aiRagClient.getLastSearchRequest().modelName());

        RagGenerationOrchestrationResult generationResult =
                orchestrator.generate(QUESTION, searchResult);

        assertEquals(generateResponse.answer(), generationResult.answer());
        assertEquals(List.of(10L), generationResult.validCitedChunkIds());
        assertEquals(1, aiRagClient.getGenerateCallCount());
        assertEquals(List.of(allowedResult), aiRagClient.getLastGenerateRequest().evidence());
    }

    private RagSearchOrchestrationResult search(Set<Long> allowedDocumentVersionIds) {
        return orchestrator.search(
                QUESTION,
                allowedDocumentVersionIds,
                PROVIDER_NAME,
                MODEL_NAME);
    }

    private AiRagSearchResultItem createSearchResult(
            Long documentVersionId,
            Long chunkId,
            double similarityScore) {
        return new AiRagSearchResultItem(
                documentVersionId,
                chunkId,
                "검색 결과 내용",
                similarityScore);
    }
}
