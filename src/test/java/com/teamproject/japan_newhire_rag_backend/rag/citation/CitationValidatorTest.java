package com.teamproject.japan_newhire_rag_backend.rag.citation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResultItem;

class CitationValidatorTest {

    private final CitationValidator validator = new CitationValidator();

    @Test
    void rejectsNullCitedChunkIds() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.filterValidCitations(null, List.of()));
    }

    @Test
    void rejectsNullVerifiedSearchResults() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.filterValidCitations(List.of(), null));
    }

    @Test
    void returnsAllCitationsWhenEveryChunkIsVerified() {
        List<Long> citedChunkIds = List.of(10L, 20L);
        List<AiRagSearchResultItem> verifiedSearchResults = List.of(
                createSearchResult(10L),
                createSearchResult(20L));

        List<Long> result =
                validator.filterValidCitations(citedChunkIds, verifiedSearchResults);

        assertEquals(citedChunkIds, result);
        assertNotSame(citedChunkIds, result);
    }

    @Test
    void returnsOnlyVerifiedCitationsInOriginalOrder() {
        List<Long> citedChunkIds = List.of(30L, 10L, 20L);
        List<AiRagSearchResultItem> verifiedSearchResults = List.of(
                createSearchResult(10L),
                createSearchResult(30L));

        List<Long> result =
                validator.filterValidCitations(citedChunkIds, verifiedSearchResults);

        assertEquals(List.of(30L, 10L), result);
    }

    @Test
    void returnsEmptyListWhenNoCitationIsVerified() {
        List<AiRagSearchResultItem> verifiedSearchResults =
                List.of(createSearchResult(10L));

        List<Long> result =
                validator.filterValidCitations(List.of(20L, 30L), verifiedSearchResults);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyListForEmptyCitedChunkIds() {
        List<AiRagSearchResultItem> verifiedSearchResults =
                List.of(createSearchResult(10L));

        List<Long> result =
                validator.filterValidCitations(List.of(), verifiedSearchResults);

        assertTrue(result.isEmpty());
    }

    @Test
    void preservesDuplicateVerifiedCitations() {
        List<Long> citedChunkIds = List.of(10L, 10L, 20L, 10L);
        List<AiRagSearchResultItem> verifiedSearchResults =
                List.of(createSearchResult(10L));

        List<Long> result =
                validator.filterValidCitations(citedChunkIds, verifiedSearchResults);

        assertEquals(List.of(10L, 10L, 10L), result);
    }

    @Test
    void modifyingReturnedListDoesNotChangeOriginalCitations() {
        List<Long> citedChunkIds = List.of(10L, 20L);
        List<Long> originalSnapshot = List.copyOf(citedChunkIds);
        List<AiRagSearchResultItem> verifiedSearchResults = List.of(
                createSearchResult(10L),
                createSearchResult(20L));

        List<Long> result =
                validator.filterValidCitations(citedChunkIds, verifiedSearchResults);
        result.remove(0);

        assertEquals(originalSnapshot, citedChunkIds);
        assertEquals(List.of(20L), result);
    }

    private AiRagSearchResultItem createSearchResult(Long chunkId) {
        return new AiRagSearchResultItem(1L, chunkId, "검증된 검색 결과", 0.8);
    }
}
