package com.teamproject.japan_newhire_rag_backend.document.chunking;

import java.util.ArrayList;
import java.util.List;

public class ChunkSplitter {

    public List<DocumentChunk> split(String text, int maxChunkSize, int overlapSize) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("분할할 텍스트가 비어 있습니다.");
        }
        if (maxChunkSize <= 0) {
            throw new IllegalArgumentException("maxChunkSize는 1 이상이어야 합니다.");
        }
        if (overlapSize < 0) {
            throw new IllegalArgumentException("overlapSize는 0 이상이어야 합니다.");
        }
        if (overlapSize >= maxChunkSize) {
            throw new IllegalArgumentException("overlapSize는 maxChunkSize보다 작아야 합니다.");
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxChunkSize, text.length());
            String chunkContent = text.substring(start, end).trim();

            if (!chunkContent.isEmpty()) {
                chunks.add(new DocumentChunk(chunks.size(), chunkContent));
            }

            if (end == text.length()) {
                break;
            }
            start = end - overlapSize;
        }

        return chunks;
    }
}
