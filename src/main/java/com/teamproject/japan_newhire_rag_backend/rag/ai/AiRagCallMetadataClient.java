package com.teamproject.japan_newhire_rag_backend.rag.ai;

public interface AiRagCallMetadataClient extends AiRagClient {
    AiHttpExecution<AiRagSearchResponse> searchWithMetadata(AiRagSearchRequest request);
    AiHttpExecution<AiRagGenerateResponse> generateWithMetadata(AiRagGenerateRequest request);
}
