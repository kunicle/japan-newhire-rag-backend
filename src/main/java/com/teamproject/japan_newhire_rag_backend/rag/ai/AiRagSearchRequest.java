package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.util.List;

public record AiRagSearchRequest(String question, List<Long> allowedDocumentIds) {

    public AiRagSearchRequest {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("질문이 비어 있습니다.");
        }
        if (allowedDocumentIds == null) {
            throw new IllegalArgumentException("허용된 문서 목록이 없습니다.");
        }
        if (allowedDocumentIds.isEmpty()) {
            throw new IllegalArgumentException("허용된 문서가 없습니다.");
        }
    }
}
