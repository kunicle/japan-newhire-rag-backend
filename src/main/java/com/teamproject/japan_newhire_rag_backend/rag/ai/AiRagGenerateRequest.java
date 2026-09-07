package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.util.List;

public record AiRagGenerateRequest(String question, List<AiRagSearchResultItem> evidence) {

    public AiRagGenerateRequest {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("질문이 비어 있습니다.");
        }
        if (evidence == null) {
            throw new IllegalArgumentException("evidence가 없습니다.");
        }
        if (evidence.isEmpty()) {
            throw new IllegalArgumentException("evidence 없이 답변을 생성할 수 없습니다.");
        }
    }
}
