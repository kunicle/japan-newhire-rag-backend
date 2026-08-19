package com.teamproject.japan_newhire_rag_backend.rag.ai;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PythonAiEmbeddingClient implements AiEmbeddingClient {

    private final RestClient restClient;
    private final AiHttpRetryExecutor retryExecutor;

    public PythonAiEmbeddingClient(RestClient restClient) {
        this(restClient, new AiHttpRetryExecutor());
    }

    PythonAiEmbeddingClient(RestClient restClient, AiHttpRetryExecutor retryExecutor) {
        this.restClient = restClient;
        this.retryExecutor = retryExecutor;
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        EmbedHttpResponse response = retryExecutor.execute(() -> restClient.post()
                .uri("/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(EmbedHttpRequest.from(request))
                .retrieve()
                .body(EmbedHttpResponse.class));

        if (response == null) {
            throw new IllegalStateException("Python AI embedding 응답이 없습니다.");
        }
        return response.toDomain();
    }

    private record EmbedHttpRequest(
            @JsonProperty("document_chunk_id") Long documentChunkId,
            @JsonProperty("document_version_id") Long documentVersionId,
            @JsonProperty("chunk_content") String chunkContent,
            @JsonProperty("provider_name") String providerName,
            @JsonProperty("model_name") String modelName) {

        private static EmbedHttpRequest from(EmbeddingRequest request) {
            return new EmbedHttpRequest(
                    request.documentChunkId(),
                    request.documentVersionId(),
                    request.chunkContent(),
                    request.providerName(),
                    request.modelName());
        }
    }

    private record EmbedHttpResponse(
            @JsonProperty("vector_reference") String vectorReference,
            @JsonProperty("embedding_dimension") int embeddingDimension) {

        private EmbeddingResult toDomain() {
            return new EmbeddingResult(vectorReference, embeddingDimension);
        }
    }
}
