package com.teamproject.japan_newhire_rag_backend.rag.ai;

public interface AiEmbeddingCallMetadataClient extends AiEmbeddingClient {
    AiHttpExecution<EmbeddingResult> embedWithMetadata(EmbeddingRequest request);
}
