package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.util.List;

public record AiRagResponse(String answer, List<AiRagSearchResultItem> searchResults) {

    public AiRagResponse {
        if (searchResults == null) {
            throw new IllegalArgumentException("검색 결과 목록이 없습니다.");
        }
    }
}
