package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.util.List;

public record AiRagSearchRequest(
        String question,
        List<Long> allowedDocumentVersionIds,
        String providerName,
        String modelName) {

    public AiRagSearchRequest {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("질문이 비어 있습니다.");
        }
        if (allowedDocumentVersionIds == null) {
            throw new IllegalArgumentException("허용된 문서 버전 목록이 없습니다.");
        }
        if (allowedDocumentVersionIds.isEmpty()) {
            throw new IllegalArgumentException("허용된 문서 버전이 없습니다.");
        }
        if (providerName == null || providerName.trim().isEmpty()) {
            throw new IllegalArgumentException("provider 이름이 비어 있습니다.");
        }
        if (modelName == null || modelName.trim().isEmpty()) {
            throw new IllegalArgumentException("모델 이름이 비어 있습니다.");
        }
    }
}
