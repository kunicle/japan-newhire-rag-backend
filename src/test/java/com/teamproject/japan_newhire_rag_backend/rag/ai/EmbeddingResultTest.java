package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EmbeddingResultTest {

    @Test
    void createsResultWithValidValues() {
        EmbeddingResult result = new EmbeddingResult("qdrant-point-10", 1536);

        assertEquals("qdrant-point-10", result.vectorReference());
        assertEquals(1536, result.embeddingDimension());
    }

    @Test
    void rejectsNullOrBlankVectorReference() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingResult(null, 1536));
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingResult("", 1536));
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingResult("   ", 1536));
    }

    @Test
    void rejectsNonPositiveEmbeddingDimension() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingResult("qdrant-point-10", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingResult("qdrant-point-10", -1));
    }
}
