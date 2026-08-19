package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class PythonAiRagClientTest {

    private MockRestServiceServer server;
    private PythonAiRagClient client;
    private RecordingSleeper sleeper;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        sleeper = new RecordingSleeper();
        client = new PythonAiRagClient(
                builder,
                "http://python-ai.test",
                new AiHttpRetryExecutor(sleeper));
    }

    @Test
    void searchSerializesRequestAndDeserializesResponse() {
        server.expect(requestTo("http://python-ai.test/rag/search"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {
                          "question": "육아휴직 규정을 알려주세요",
                          "allowed_document_version_ids": [101, 102],
                          "provider_name": "provider-a",
                          "model_name": "model-a"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "search_results": [
                            {
                              "document_version_id": 101,
                              "chunk_id": 5001,
                              "content": "예시 규정 텍스트",
                              "similarity_score": 0.87
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiRagSearchResponse response = client.search(
                new AiRagSearchRequest(
                        "육아휴직 규정을 알려주세요",
                        List.of(101L, 102L),
                        "provider-a",
                        "model-a"));

        assertEquals(1, response.searchResults().size());
        AiRagSearchResultItem item = response.searchResults().get(0);
        assertEquals(101L, item.documentVersionId());
        assertEquals(5001L, item.chunkId());
        assertEquals("예시 규정 텍스트", item.content());
        assertEquals(0.87, item.similarityScore());
        server.verify();
    }

    @Test
    void generateSerializesEvidenceAndDeserializesResponse() {
        server.expect(requestTo("http://python-ai.test/rag/generate"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {
                          "question": "육아휴직 규정을 알려주세요",
                          "evidence": [
                            {
                              "document_version_id": 101,
                              "chunk_id": 5001,
                              "content": "근거",
                              "similarity_score": 0.87
                            }
                          ]
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "answer": "예시 답변 텍스트",
                          "cited_chunk_ids": [5001]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiRagGenerateResponse response = client.generate(new AiRagGenerateRequest(
                "육아휴직 규정을 알려주세요",
                List.of(new AiRagSearchResultItem(101L, 5001L, "근거", 0.87))));

        assertEquals("예시 답변 텍스트", response.answer());
        assertEquals(List.of(5001L), response.citedChunkIds());
        server.verify();
    }

    @Test
    void propagatesNonSuccessfulHttpResponse() {
        server.expect(requestTo("http://python-ai.test/rag/search"))
                .andExpect(method(POST))
                .andRespond(withUnauthorizedRequest());

        assertThrows(
                RestClientResponseException.class,
                () -> client.search(new AiRagSearchRequest(
                        "질문", List.of(101L), "provider-a", "model-a")));
        assertEquals(List.of(), sleeper.durations);
        server.verify();
    }

    @Test
    void searchRetriesServerErrorsAndReturnsSuccessfulResponse() {
        server.expect(requestTo("http://python-ai.test/rag/search"))
                .andExpect(method(POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(requestTo("http://python-ai.test/rag/search"))
                .andExpect(method(POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(requestTo("http://python-ai.test/rag/search"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {
                          "search_results": [
                            {
                              "document_version_id": 101,
                              "chunk_id": 5001,
                              "content": "재시도 성공",
                              "similarity_score": 0.91
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiRagSearchResponse response = client.search(new AiRagSearchRequest(
                "질문", List.of(101L), "provider-a", "model-a"));

        assertEquals("재시도 성공", response.searchResults().get(0).content());
        assertEquals(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2)), sleeper.durations);
        server.verify();
    }

    @Test
    void generatePropagatesFailureAfterThreeAttempts() {
        server.expect(requestTo("http://python-ai.test/rag/generate"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://python-ai.test/rag/generate"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://python-ai.test/rag/generate"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(
                RestClientResponseException.class,
                () -> client.generate(new AiRagGenerateRequest(
                        "질문",
                        List.of(new AiRagSearchResultItem(101L, 5001L, "근거", 0.87)))));

        assertEquals(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2)), sleeper.durations);
        server.verify();
    }

    private static class RecordingSleeper implements RetrySleeper {

        private final List<Duration> durations = new ArrayList<>();

        @Override
        public void sleep(Duration duration) {
            durations.add(duration);
        }
    }
}
