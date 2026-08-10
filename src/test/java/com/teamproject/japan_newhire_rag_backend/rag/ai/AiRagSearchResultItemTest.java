package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AiRagSearchResultItemTest {

    @Test
    void createsSearchResultItemWithValidValues() {
        AiRagSearchResultItem item = new AiRagSearchResultItem(1L, 10L, "휴가 규정 내용", 0.85);

        assertEquals(1L, item.documentId());
        assertEquals(10L, item.chunkId());
        assertEquals("휴가 규정 내용", item.content());
        assertEquals(0.85, item.similarityScore());
    }

    @Test
    void rejectsNullDocumentId() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchResultItem(null, 10L, "휴가 규정", 0.8));
    }

    @Test
    void rejectsNullChunkId() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchResultItem(1L, null, "휴가 규정", 0.8));
    }

    @Test
    void rejectsNullOrBlankContent() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchResultItem(1L, 10L, null, 0.8));
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchResultItem(1L, 10L, "", 0.8));
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchResultItem(1L, 10L, "   ", 0.8));
    }

    @Test
    void rejectsSimilarityScoreOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchResultItem(1L, 10L, "휴가 규정", -0.01));
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchResultItem(1L, 10L, "휴가 규정", 1.01));
    }
}
