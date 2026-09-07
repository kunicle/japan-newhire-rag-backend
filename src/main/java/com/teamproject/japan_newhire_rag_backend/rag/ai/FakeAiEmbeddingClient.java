package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.util.HashMap;
import java.util.Map;

public class FakeAiEmbeddingClient implements AiEmbeddingClient {

    private final Map<Long, EmbeddingResult> resultsByDocumentChunkId = new HashMap<>();
    private EmbeddingResult defaultResult;
    private int embedCallCount;
    private EmbeddingRequest lastRequest;

    public void registerResult(Long documentChunkId, EmbeddingResult result) {
        if (documentChunkId == null || result == null) {
            throw new IllegalArgumentException("documentChunkId와 result는 null일 수 없습니다.");
        }
        resultsByDocumentChunkId.put(documentChunkId, result);
    }

    public void setDefaultResult(EmbeddingResult result) {
        defaultResult = result;
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request는 null일 수 없습니다.");
        }

        embedCallCount++;
        lastRequest = request;

        EmbeddingResult registeredResult = resultsByDocumentChunkId.get(request.documentChunkId());
        if (registeredResult != null) {
            return registeredResult;
        }
        if (defaultResult != null) {
            return defaultResult;
        }
        throw new IllegalStateException(
                "등록된 embedding 결과가 없습니다: " + request.documentChunkId());
    }

    public int getEmbedCallCount() {
        return embedCallCount;
    }

    public EmbeddingRequest getLastRequest() {
        return lastRequest;
    }
}
