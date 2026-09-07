package com.teamproject.japan_newhire_rag_backend.rag.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.document.chunk.repository.DocumentChunkRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResultItem;

@ExtendWith(MockitoExtension.class)
class SearchResultVerifierTest {

    @Mock DocumentChunkRepository documentChunkRepository;

    private SearchResultVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new SearchResultVerifier(documentChunkRepository);
    }

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
        stubChunks(
                createChunk(10L, 1L, "검색 결과 내용"),
                createChunk(20L, 2L, "검색 결과 내용"));

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
        stubChunks(
                createChunk(10L, 2L, "검색 결과 내용"),
                createChunk(30L, 2L, "검색 결과 내용"));

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
        stubChunks(
                createChunk(10L, 1L, "검색 결과 내용"),
                createChunk(20L, 2L, "검색 결과 내용"));

        List<AiRagSearchResultItem> result =
                verifier.filterByAllowedDocumentVersions(searchResults, Set.of(1L, 2L));
        result.remove(0);

        assertEquals(originalSnapshot, searchResults);
        assertEquals(1, result.size());
    }

    @Test
    void keepsResultWhenChunkExistsAndVersionMatches() {
        AiRagSearchResultItem searchResult = createSearchResult(1L, 10L);
        stubChunks(createChunk(10L, 1L, "검색 결과 내용"));

        List<AiRagSearchResultItem> result =
                verifier.filterByAllowedDocumentVersions(List.of(searchResult), Set.of(1L));

        assertEquals(List.of(searchResult), result);
    }

    @Test
    void dropsResultWhenClaimedVersionNotAllowedWithoutDbLookup() {
        List<AiRagSearchResultItem> result = verifier.filterByAllowedDocumentVersions(
                List.of(createSearchResult(2L, 10L)),
                Set.of(1L));

        assertTrue(result.isEmpty());
        verifyNoInteractions(documentChunkRepository);
    }

    @Test
    void dropsResultWhenChunkDoesNotExistInDatabase() {
        when(documentChunkRepository.findAllById(any())).thenReturn(List.of());

        List<AiRagSearchResultItem> result = verifier.filterByAllowedDocumentVersions(
                List.of(createSearchResult(1L, 10L)),
                Set.of(1L));

        assertTrue(result.isEmpty());
    }

    @Test
    void dropsResultWhenChunkBelongsToDifferentVersion() {
        stubChunks(createChunk(10L, 2L, "검색 결과 내용"));

        List<AiRagSearchResultItem> result = verifier.filterByAllowedDocumentVersions(
                List.of(createSearchResult(1L, 10L)),
                Set.of(1L));

        assertTrue(result.isEmpty());
    }

    @Test
    void keepsOnlyValidResultsFromMixedBatchInOriginalOrder() {
        AiRagSearchResultItem firstValid = createSearchResult(1L, 10L);
        AiRagSearchResultItem nonexistent = createSearchResult(1L, 20L);
        AiRagSearchResultItem mismatch = createSearchResult(1L, 30L);
        AiRagSearchResultItem disallowed = createSearchResult(2L, 40L);
        AiRagSearchResultItem secondValid = createSearchResult(1L, 50L);
        stubChunks(
                createChunk(50L, 1L, "검색 결과 내용"),
                createChunk(30L, 3L, "검색 결과 내용"),
                createChunk(10L, 1L, "검색 결과 내용"));

        List<AiRagSearchResultItem> result = verifier.filterByAllowedDocumentVersions(
                List.of(firstValid, nonexistent, mismatch, disallowed, secondValid),
                Set.of(1L));

        assertEquals(List.of(firstValid, secondValid), result);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Long>> chunkIdsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(documentChunkRepository).findAllById(chunkIdsCaptor.capture());
        List<Long> queriedChunkIds = new ArrayList<>();
        chunkIdsCaptor.getValue().forEach(queriedChunkIds::add);
        assertEquals(Set.of(10L, 20L, 30L, 50L), Set.copyOf(queriedChunkIds));
        assertTrue(!queriedChunkIds.contains(40L));
    }

    @Test
    void returnsEmptyListAndSkipsDbQueryForEmptyInput() {
        List<AiRagSearchResultItem> result =
                verifier.filterByAllowedDocumentVersions(List.of(), Set.of(1L));

        assertTrue(result.isEmpty());
        verify(documentChunkRepository, never()).findAllById(any());
    }

    @Test
    void preservesDuplicateValidChunkIdsAtBothPositions() {
        AiRagSearchResultItem first = new AiRagSearchResultItem(1L, 10L, "첫 번째", 0.9);
        AiRagSearchResultItem second = new AiRagSearchResultItem(1L, 10L, "두 번째", 0.8);
        stubChunks(createChunk(10L, 1L, "DB 내용"));

        List<AiRagSearchResultItem> result =
                verifier.filterByAllowedDocumentVersions(List.of(first, second), Set.of(1L));

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).chunkId());
        assertEquals(10L, result.get(1).chunkId());
        assertEquals(0.9, result.get(0).similarityScore());
        assertEquals(0.8, result.get(1).similarityScore());
    }

    @Test
    void replacesContentWithDatabaseAuthoritativeContent() {
        AiRagSearchResultItem staleResult =
                new AiRagSearchResultItem(1L, 10L, "STALE_CONTENT", 0.8);
        stubChunks(createChunk(10L, 1L, "AUTHORITATIVE_CONTENT"));

        List<AiRagSearchResultItem> result =
                verifier.filterByAllowedDocumentVersions(List.of(staleResult), Set.of(1L));

        assertEquals("AUTHORITATIVE_CONTENT", result.get(0).content());
    }

    @Test
    void preservesOriginalSimilarityScoreFromAiResult() {
        AiRagSearchResultItem searchResult =
                new AiRagSearchResultItem(1L, 10L, "검색 결과 내용", 0.876543);
        stubChunks(createChunk(10L, 1L, "DB 내용"));

        List<AiRagSearchResultItem> result =
                verifier.filterByAllowedDocumentVersions(List.of(searchResult), Set.of(1L));

        assertEquals(0.876543, result.get(0).similarityScore());
    }

    private AiRagSearchResultItem createSearchResult(Long documentVersionId, Long chunkId) {
        return new AiRagSearchResultItem(documentVersionId, chunkId, "검색 결과 내용", 0.8);
    }

    private DocumentChunk createChunk(Long chunkId, Long documentVersionId, String content) {
        DocumentVersion documentVersion = mock(DocumentVersion.class);
        when(documentVersion.getDocumentVersionId()).thenReturn(documentVersionId);
        DocumentChunk chunk = mock(DocumentChunk.class);
        when(chunk.getDocumentChunkId()).thenReturn(chunkId);
        when(chunk.getDocumentVersion()).thenReturn(documentVersion);
        lenient().when(chunk.getChunkContent()).thenReturn(content);
        return chunk;
    }

    private void stubChunks(DocumentChunk... chunks) {
        when(documentChunkRepository.findAllById(any())).thenReturn(List.of(chunks));
    }
}
