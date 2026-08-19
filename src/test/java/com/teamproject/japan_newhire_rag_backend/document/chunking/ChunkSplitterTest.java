package com.teamproject.japan_newhire_rag_backend.document.chunking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class ChunkSplitterTest {

    private final ChunkSplitter splitter = new ChunkSplitter();

    @Test
    void rejectsNullText() {
        assertThrows(IllegalArgumentException.class, () -> splitter.split(null, 10, 0));
    }

    @Test
    void rejectsEmptyOrBlankText() {
        assertThrows(IllegalArgumentException.class, () -> splitter.split("", 10, 0));
        assertThrows(IllegalArgumentException.class, () -> splitter.split("   ", 10, 0));
    }

    @Test
    void rejectsNonPositiveMaxChunkSize() {
        assertThrows(IllegalArgumentException.class, () -> splitter.split("text", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> splitter.split("text", -1, 0));
    }

    @Test
    void rejectsNegativeOverlapSize() {
        assertThrows(IllegalArgumentException.class, () -> splitter.split("text", 10, -1));
    }

    @Test
    void rejectsOverlapSizeEqualToOrLargerThanMaxChunkSize() {
        assertThrows(IllegalArgumentException.class, () -> splitter.split("text", 4, 4));
        assertThrows(IllegalArgumentException.class, () -> splitter.split("text", 4, 5));
    }

    @Test
    void returnsOneTrimmedChunkWhenTextIsShorterThanMaxChunkSize() {
        List<DocumentChunk> chunks = splitter.split("  사내 규정  ", 20, 3);

        assertEquals(1, chunks.size());
        assertEquals(0, chunks.get(0).index());
        assertEquals("사내 규정", chunks.get(0).content());
    }

    @Test
    void splitsLongTextAndAssignsConsecutiveIndexes() {
        List<DocumentChunk> chunks = splitter.split("abcdefghijkl", 5, 1);

        assertEquals(3, chunks.size());
        assertEquals(List.of(0, 1, 2), chunks.stream().map(DocumentChunk::index).toList());
        assertEquals(List.of("abcde", "efghi", "ijkl"),
                chunks.stream().map(DocumentChunk::content).toList());
    }

    @Test
    void overlapsBoundaryTextBetweenAdjacentChunks() {
        List<DocumentChunk> chunks = splitter.split("abcdefghij", 6, 2);

        assertEquals("abcdef", chunks.get(0).content());
        assertEquals("efghij", chunks.get(1).content());
        assertEquals(chunks.get(0).content().substring(4),
                chunks.get(1).content().substring(0, 2));
    }

    @Test
    void includesLastChunkWhenItIsShorterThanMaxChunkSize() {
        List<DocumentChunk> chunks = splitter.split("abcdefghijk", 5, 1);

        assertEquals(3, chunks.size());
        assertEquals("ijk", chunks.get(2).content());
    }

    @Test
    void splitsInOrderWithoutOverlapWhenOverlapSizeIsZero() {
        List<DocumentChunk> chunks = splitter.split("abcdefghij", 4, 0);

        assertEquals(List.of("abcd", "efgh", "ij"),
                chunks.stream().map(DocumentChunk::content).toList());
    }

    @Test
    void skipsBlankChunksAndKeepsIndexesConsecutive() {
        List<DocumentChunk> chunks = splitter.split("abc   def", 3, 0);

        assertEquals(List.of(0, 1), chunks.stream().map(DocumentChunk::index).toList());
        assertEquals(List.of("abc", "def"),
                chunks.stream().map(DocumentChunk::content).toList());
    }

    @Test
    void rejectsInvalidDocumentChunkValues() {
        assertThrows(IllegalArgumentException.class, () -> new DocumentChunk(-1, "content"));
        assertThrows(IllegalArgumentException.class, () -> new DocumentChunk(0, null));
        assertThrows(IllegalArgumentException.class, () -> new DocumentChunk(0, "   "));
    }
}
