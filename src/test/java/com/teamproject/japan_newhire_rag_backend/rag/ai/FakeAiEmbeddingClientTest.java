package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FakeAiEmbeddingClientTest {

    private final FakeAiEmbeddingClient client = new FakeAiEmbeddingClient();

    @Test
    void returnsRegisteredResultForEachDocumentChunkId() {
        EmbeddingResult firstResult = result("vector-10");
        EmbeddingResult secondResult = result("vector-20");
        client.registerResult(10L, firstResult);
        client.registerResult(20L, secondResult);

        assertSame(firstResult, client.embed(request(10L)));
        assertSame(secondResult, client.embed(request(20L)));
    }

    @Test
    void returnsDefaultResultWhenNoRegisteredResultExists() {
        EmbeddingResult defaultResult = result("default-vector");
        client.setDefaultResult(defaultResult);

        assertSame(defaultResult, client.embed(request(10L)));
    }

    @Test
    void throwsWhenRegisteredAndDefaultResultsDoNotExist() {
        assertThrows(IllegalStateException.class, () -> client.embed(request(10L)));
    }

    @Test
    void registeredResultTakesPriorityOverDefaultResult() {
        EmbeddingResult registeredResult = result("registered-vector");
        client.registerResult(10L, registeredResult);
        client.setDefaultResult(result("default-vector"));

        assertSame(registeredResult, client.embed(request(10L)));
    }

    @Test
    void countsCallsAndStoresMostRecentRequest() {
        client.setDefaultResult(result("default-vector"));
        EmbeddingRequest firstRequest = request(10L);
        EmbeddingRequest lastRequest = request(20L);

        client.embed(firstRequest);
        client.embed(lastRequest);

        assertEquals(2, client.getEmbedCallCount());
        assertSame(lastRequest, client.getLastRequest());
    }

    @Test
    void rejectsNullRequestWithoutCountingCall() {
        assertThrows(IllegalArgumentException.class, () -> client.embed(null));
        assertEquals(0, client.getEmbedCallCount());
    }

    private EmbeddingRequest request(Long documentChunkId) {
        return new EmbeddingRequest(documentChunkId, "규정 내용", "openai", "model");
    }

    private EmbeddingResult result(String vectorReference) {
        return new EmbeddingResult(vectorReference, 1536);
    }
}
