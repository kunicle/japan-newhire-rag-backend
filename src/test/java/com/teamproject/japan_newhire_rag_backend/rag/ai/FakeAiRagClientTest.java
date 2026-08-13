package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class FakeAiRagClientTest {

    private final FakeAiRagClient client = new FakeAiRagClient();

    @Test
    void searchReturnsRegisteredResponse() {
        AiRagSearchResponse response = createSearchResponse(10L);
        client.registerSearchResponse("휴가 규정", response);

        assertSame(response, client.search(createSearchRequest("휴가 규정")));
    }

    @Test
    void searchOverwritesResponseForSameQuestion() {
        AiRagSearchResponse firstResponse = createSearchResponse(10L);
        AiRagSearchResponse secondResponse = createSearchResponse(20L);
        client.registerSearchResponse("휴가 규정", firstResponse);
        client.registerSearchResponse("휴가 규정", secondResponse);

        assertSame(secondResponse, client.search(createSearchRequest("휴가 규정")));
    }

    @Test
    void searchReturnsDefaultResponse() {
        AiRagSearchResponse response = createSearchResponse(10L);
        client.setDefaultSearchResponse(response);

        assertSame(response, client.search(createSearchRequest("미등록 질문")));
    }

    @Test
    void searchThrowsWhenNoResponseExists() {
        assertThrows(IllegalStateException.class,
                () -> client.search(createSearchRequest("미등록 질문")));
    }

    @Test
    void searchRejectsNullRequest() {
        assertThrows(IllegalArgumentException.class, () -> client.search(null));
    }

    @Test
    void registerSearchResponseRejectsNullValues() {
        AiRagSearchResponse response = createSearchResponse(10L);

        assertThrows(IllegalArgumentException.class,
                () -> client.registerSearchResponse(null, response));
        assertThrows(IllegalArgumentException.class,
                () -> client.registerSearchResponse("휴가 규정", null));
    }

    @Test
    void generateReturnsRegisteredResponse() {
        AiRagGenerateResponse response = new AiRagGenerateResponse("등록 답변", List.of(10L));
        client.registerGenerateResponse("휴가 규정", response);

        assertSame(response, client.generate(createGenerateRequest("휴가 규정")));
    }

    @Test
    void generateReturnsDefaultResponse() {
        AiRagGenerateResponse response = new AiRagGenerateResponse("기본 답변", List.of());
        client.setDefaultGenerateResponse(response);

        assertSame(response, client.generate(createGenerateRequest("미등록 질문")));
    }

    @Test
    void generateThrowsWhenNoResponseExists() {
        assertThrows(IllegalStateException.class,
                () -> client.generate(createGenerateRequest("미등록 질문")));
        assertEquals(1, client.getGenerateCallCount());
    }

    @Test
    void generateRejectsNullRequest() {
        assertThrows(IllegalArgumentException.class, () -> client.generate(null));
        assertEquals(0, client.getGenerateCallCount());
    }

    @Test
    void registerGenerateResponseRejectsNullValues() {
        AiRagGenerateResponse response = new AiRagGenerateResponse("답변", List.of());

        assertThrows(IllegalArgumentException.class,
                () -> client.registerGenerateResponse(null, response));
        assertThrows(IllegalArgumentException.class,
                () -> client.registerGenerateResponse("휴가 규정", null));
    }

    @Test
    void generateCountsEveryCall() {
        client.setDefaultGenerateResponse(new AiRagGenerateResponse("답변", List.of()));

        client.generate(createGenerateRequest("질문 1"));
        client.generate(createGenerateRequest("질문 2"));
        client.generate(createGenerateRequest("질문 3"));

        assertEquals(3, client.getGenerateCallCount());
    }

    @Test
    void searchDoesNotIncreaseGenerateCallCount() {
        client.setDefaultSearchResponse(createSearchResponse(10L));

        client.search(createSearchRequest("휴가 규정"));

        assertEquals(0, client.getGenerateCallCount());
    }

    @Test
    void generateStoresLastRequest() {
        client.setDefaultGenerateResponse(new AiRagGenerateResponse("답변", List.of()));
        AiRagGenerateRequest firstRequest = createGenerateRequest("질문 1");
        AiRagGenerateRequest lastRequest = createGenerateRequest("질문 2");

        client.generate(firstRequest);
        client.generate(lastRequest);

        assertSame(lastRequest, client.getLastGenerateRequest());
    }

    private AiRagSearchRequest createSearchRequest(String question) {
        return new AiRagSearchRequest(question, List.of(1L), "provider-a", "model-a");
    }

    private AiRagGenerateRequest createGenerateRequest(String question) {
        return new AiRagGenerateRequest(question, List.of(createSearchResult(1L, 10L)));
    }

    private AiRagSearchResponse createSearchResponse(Long chunkId) {
        return new AiRagSearchResponse(List.of(createSearchResult(1L, chunkId)));
    }

    private AiRagSearchResultItem createSearchResult(Long documentVersionId, Long chunkId) {
        return new AiRagSearchResultItem(documentVersionId, chunkId, "검색 결과", 0.9);
    }
}
