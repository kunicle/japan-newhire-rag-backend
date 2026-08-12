package com.teamproject.japan_newhire_rag_backend.rag.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResultItem;

class SearchResultVerifierTest {

    private final SearchResultVerifier verifier = new SearchResultVerifier();

    @Test
    void rejectsNullSearchResults() {
        assertThrows(IllegalArgumentException.class,
                () -> verifier.filterByAllowedDocumentVersions(null, Set.of(1L)));
    }

    @Test
    void rejectsNullAllowedDocumentVersionIds() {
        assertThrows(IllegalArgumentException.class,
                () -> verifier.filterByAllowedDocumentVersions(List.of(), null));
    }

    @Test
    void returnsAllResultsWhenEveryDocumentIsAllowed() {
        List<AiRagSearchResultItem> searchResults = List.of(
                createSearchResult(1L, 10L),
                createSearchResult(2L, 20L));

        List<AiRagSearchResultItem> result =
                verifier.filterByAllowedDocumentVersions(searchResults, Set.of(1L, 2L));

        assertEquals(searchResults, result);
        assertNotSame(searchResults, result);
    }

    @Test
    void returnsOnlyAllowedResultsInOriginalOrder() {
        AiRagSearchResultItem firstAllowed = createSearchResult(2L, 10L);
        AiRagSearchResultItem disallowed = createSearchResult(1L, 20L);
        AiRagSearchResultItem secondAllowed = createSearchResult(2L, 30L);
        List<AiRagSearchResultItem> searchResults =
                List.of(firstAllowed, disallowed, secondAllowed);

        List<AiRagSearchResultItem> result =
                verifier.filterByAllowedDocumentVersions(searchResults, Set.of(2L));

        assertEquals(List.of(firstAllowed, secondAllowed), result);
    }

    @Test
    void returnsEmptyListWhenNoDocumentIsAllowed() {
        List<AiRagSearchResultItem> searchResults = List.of(
                createSearchResult(1L, 10L),
                createSearchResult(2L, 20L));

        List<AiRagSearchResultItem> result =
                verifier.filterByAllowedDocumentVersions(searchResults, Set.of(3L));

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyListForEmptySearchResults() {
        List<AiRagSearchResultItem> result =
                verifier.filterByAllowedDocumentVersions(List.of(), Set.of(1L));

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyListForEmptyAllowedDocumentVersionIds() {
        List<AiRagSearchResultItem> searchResults =
                List.of(createSearchResult(1L, 10L));

        List<AiRagSearchResultItem> result =
                verifier.filterByAllowedDocumentVersions(searchResults, Set.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void modifyingReturnedListDoesNotChangeOriginalList() {
        List<AiRagSearchResultItem> searchResults = List.of(
                createSearchResult(1L, 10L),
                createSearchResult(2L, 20L));
        List<AiRagSearchResultItem> originalSnapshot = List.copyOf(searchResults);

        List<AiRagSearchResultItem> result =
                verifier.filterByAllowedDocumentVersions(searchResults, Set.of(1L, 2L));
        result.remove(0);

        assertEquals(originalSnapshot, searchResults);
        assertEquals(1, result.size());
    }

    private AiRagSearchResultItem createSearchResult(Long documentVersionId, Long chunkId) {
        return new AiRagSearchResultItem(documentVersionId, chunkId, "검색 결과 내용", 0.8);
    }
}
