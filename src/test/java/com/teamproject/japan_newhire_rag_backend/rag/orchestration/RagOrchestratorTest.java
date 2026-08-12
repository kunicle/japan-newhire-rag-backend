package com.teamproject.japan_newhire_rag_backend.rag.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private final FakeAiRagClient aiRagClient = new FakeAiRagClient();
    private final RagOrchestrator orchestrator = new RagOrchestrator(
            aiRagClient,
            new SearchResultVerifier(),
            new EvidenceThresholdChecker(),
            new CitationValidator(),
            EVIDENCE_THRESHOLD);

    @Test
    void rejectsNullRequiredArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.handle(null, Set.of(1L)));
        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.handle(QUESTION, null));
    }

    @Test
    void doesNotGenerateWhenSearchResultsAreEmpty() {
        aiRagClient.registerSearchResponse(QUESTION, new AiRagSearchResponse(List.of()));

        RagOrchestrationResult result = orchestrator.handle(QUESTION, Set.of(1L));

        assertInsufficientEvidenceWithoutGeneration(result);
    }

    @Test
    void doesNotGenerateWhenEverySimilarityIsBelowThreshold() {
        aiRagClient.registerSearchResponse(QUESTION, new AiRagSearchResponse(List.of(
                createSearchResult(1L, 10L, 0.6),
                createSearchResult(1L, 20L, 0.5))));

        RagOrchestrationResult result = orchestrator.handle(QUESTION, Set.of(1L));

        assertInsufficientEvidenceWithoutGeneration(result);
    }

    @Test
    void doesNotGenerateWhenEverySearchResultIsFilteredOut() {
        aiRagClient.registerSearchResponse(QUESTION, new AiRagSearchResponse(List.of(
                createSearchResult(2L, 10L, 0.9))));

        RagOrchestrationResult result = orchestrator.handle(QUESTION, Set.of(1L));

        assertInsufficientEvidenceWithoutGeneration(result);
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

        RagOrchestrationResult result = orchestrator.handle(QUESTION, Set.of(1L));

        assertTrue(result.hasSufficientEvidence());
        assertEquals(1, aiRagClient.getGenerateCallCount());
        assertEquals(generateResponse.answer(), result.answer());
        assertEquals(List.of(allowedResult), aiRagClient.getLastGenerateRequest().evidence());
        assertEquals(List.of(10L), result.validCitedChunkIds());
    }

    private void assertInsufficientEvidenceWithoutGeneration(RagOrchestrationResult result) {
        assertFalse(result.hasSufficientEvidence());
        assertNull(result.answer());
        assertTrue(result.validCitedChunkIds().isEmpty());
        assertEquals(0, aiRagClient.getGenerateCallCount());
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
