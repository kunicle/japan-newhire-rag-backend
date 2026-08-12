package com.teamproject.japan_newhire_rag_backend.rag.ai;

public record AiRagSearchResultItem(
        Long documentVersionId,
        Long chunkId,
        String content,
        double similarityScore) {

    public AiRagSearchResultItem {
        if (documentVersionId == null) {
            throw new IllegalArgumentException("documentVersionId가 없습니다.");
        }
        if (chunkId == null) {
            throw new IllegalArgumentException("chunkId가 없습니다.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("검색 결과 내용이 비어 있습니다.");
        }
        if (similarityScore < 0.0 || similarityScore > 1.0) {
            throw new IllegalArgumentException("similarityScore는 0.0 이상 1.0 이하여야 합니다.");
        }
    }
}
