package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EmbeddingRequestTest {

    @Test
    void createsRequestWithValidValues() {
        EmbeddingRequest request = new EmbeddingRequest(
                10L,
                "규정 내용",
                "openai",
                "text-embedding-model");

        assertEquals(10L, request.documentChunkId());
        assertEquals("규정 내용", request.chunkContent());
        assertEquals("openai", request.providerName());
        assertEquals("text-embedding-model", request.modelName());
    }

    @Test
    void rejectsNullDocumentChunkId() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingRequest(null, "규정 내용", "openai", "model"));
    }

    @Test
    void rejectsNullOrBlankChunkContent() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingRequest(10L, null, "openai", "model"));
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingRequest(10L, "", "openai", "model"));
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingRequest(10L, "   ", "openai", "model"));
    }

    @Test
    void rejectsNullOrBlankProviderName() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingRequest(10L, "규정 내용", null, "model"));
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingRequest(10L, "규정 내용", "", "model"));
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingRequest(10L, "규정 내용", "   ", "model"));
    }

    @Test
    void rejectsNullOrBlankModelName() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingRequest(10L, "규정 내용", "openai", null));
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingRequest(10L, "규정 내용", "openai", ""));
        assertThrows(IllegalArgumentException.class,
                () -> new EmbeddingRequest(10L, "규정 내용", "openai", "   "));
    }
}
