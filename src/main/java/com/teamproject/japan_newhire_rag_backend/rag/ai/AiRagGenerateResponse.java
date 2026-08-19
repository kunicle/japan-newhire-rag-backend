package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.util.List;

public record AiRagGenerateResponse(String answer, List<Long> citedChunkIds) {

    public AiRagGenerateResponse {
        if (citedChunkIds == null) {
            throw new IllegalArgumentException("인용된 chunk 목록이 없습니다.");
        }
    }
}
