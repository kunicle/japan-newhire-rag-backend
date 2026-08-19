package com.teamproject.japan_newhire_rag_backend.rag.ai;

public interface AiRagClient {

    AiRagSearchResponse search(AiRagSearchRequest request);

    AiRagGenerateResponse generate(AiRagGenerateRequest request);
}
