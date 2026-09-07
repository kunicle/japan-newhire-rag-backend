package com.teamproject.japan_newhire_rag_backend.document.chunking;

public record DocumentChunk(int index, String content) {

    public DocumentChunk {
        if (index < 0) {
            throw new IllegalArgumentException("index는 0 이상이어야 합니다.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("chunk 내용이 비어 있습니다.");
        }

        content = content.trim();
    }
}
