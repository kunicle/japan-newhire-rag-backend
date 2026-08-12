package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PythonAiRagClient implements AiRagClient {

    private final RestClient restClient;

    public PythonAiRagClient(RestClient.Builder restClientBuilder, String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public AiRagSearchResponse search(AiRagSearchRequest request) {
        SearchHttpResponse response = restClient.post()
                .uri("/rag/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(SearchHttpRequest.from(request))
                .retrieve()
                .body(SearchHttpResponse.class);

        if (response == null) {
            throw new IllegalStateException("Python AI 검색 응답이 없습니다.");
        }
        return response.toDomain();
    }

    @Override
    public AiRagGenerateResponse generate(AiRagGenerateRequest request) {
        GenerateHttpResponse response = restClient.post()
                .uri("/rag/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(GenerateHttpRequest.from(request))
                .retrieve()
                .body(GenerateHttpResponse.class);

        if (response == null) {
            throw new IllegalStateException("Python AI 생성 응답이 없습니다.");
        }
        return response.toDomain();
    }

    private record SearchHttpRequest(
            String question,
            @JsonProperty("allowed_document_version_ids") List<Long> allowedDocumentVersionIds) {

        private static SearchHttpRequest from(AiRagSearchRequest request) {
            return new SearchHttpRequest(request.question(), request.allowedDocumentVersionIds());
        }
    }

    private record SearchHttpResponse(
            @JsonProperty("search_results") List<SearchResultHttpItem> searchResults) {

        private AiRagSearchResponse toDomain() {
            List<AiRagSearchResultItem> items = searchResults.stream()
                    .map(SearchResultHttpItem::toDomain)
                    .toList();
            return new AiRagSearchResponse(items);
        }
    }

    private record SearchResultHttpItem(
            @JsonProperty("document_version_id") Long documentVersionId,
            @JsonProperty("chunk_id") Long chunkId,
            String content,
            @JsonProperty("similarity_score") double similarityScore) {

        private static SearchResultHttpItem from(AiRagSearchResultItem item) {
            return new SearchResultHttpItem(
                    item.documentVersionId(),
                    item.chunkId(),
                    item.content(),
                    item.similarityScore());
        }

        private AiRagSearchResultItem toDomain() {
            return new AiRagSearchResultItem(
                    documentVersionId,
                    chunkId,
                    content,
                    similarityScore);
        }
    }

    private record GenerateHttpRequest(
            String question,
            List<SearchResultHttpItem> evidence) {

        private static GenerateHttpRequest from(AiRagGenerateRequest request) {
            List<SearchResultHttpItem> evidence = request.evidence().stream()
                    .map(SearchResultHttpItem::from)
                    .toList();
            return new GenerateHttpRequest(request.question(), evidence);
        }
    }

    private record GenerateHttpResponse(
            String answer,
            @JsonProperty("cited_chunk_ids") List<Long> citedChunkIds) {

        private AiRagGenerateResponse toDomain() {
            return new AiRagGenerateResponse(answer, citedChunkIds);
        }
    }
}
