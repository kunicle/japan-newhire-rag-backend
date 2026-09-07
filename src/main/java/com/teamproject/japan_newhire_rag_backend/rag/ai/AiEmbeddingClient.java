package com.teamproject.japan_newhire_rag_backend.rag.ai;

public interface AiEmbeddingClient {

    EmbeddingResult embed(EmbeddingRequest request);
}
