package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class PythonAiEmbeddingClientTest {

    private MockRestServiceServer server;
    private PythonAiEmbeddingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://python-ai.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PythonAiEmbeddingClient(builder.build());
    }

    @Test
    void embedSerializesRequestAndDeserializesResponse() {
        server.expect(requestTo("http://python-ai.test/embed"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {
                          "document_chunk_id": 101,
                          "chunk_content": "example content",
                          "provider_name": "provider-a",
                          "model_name": "model-a"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "vector_reference": "chunk-101-vector",
                          "embedding_dimension": 3
                        }
                        """, MediaType.APPLICATION_JSON));

        EmbeddingResult result = client.embed(new EmbeddingRequest(
                101L,
                "example content",
                "provider-a",
                "model-a"));

        assertEquals("chunk-101-vector", result.vectorReference());
        assertEquals(3, result.embeddingDimension());
        server.verify();
    }

    @Test
    void rejectsNullResponseBody() {
        server.expect(requestTo("http://python-ai.test/embed"))
                .andExpect(method(POST))
                .andRespond(withSuccess());

        assertThrows(IllegalStateException.class, () -> client.embed(request()));
        server.verify();
    }

    @Test
    void propagatesNonSuccessfulHttpResponse() {
        server.expect(requestTo("http://python-ai.test/embed"))
                .andExpect(method(POST))
                .andRespond(withUnauthorizedRequest());

        assertThrows(RestClientResponseException.class, () -> client.embed(request()));
        server.verify();
    }

    private EmbeddingRequest request() {
        return new EmbeddingRequest(101L, "example content", "provider-a", "model-a");
    }
}
