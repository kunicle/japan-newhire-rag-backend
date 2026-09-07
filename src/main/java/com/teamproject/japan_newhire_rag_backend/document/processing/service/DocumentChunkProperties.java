package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "document.chunk")
public record DocumentChunkProperties(
        int maxChunkSize,
        int overlapSize) {

    public DocumentChunkProperties {
        if (maxChunkSize <= 0) {
            throw new IllegalArgumentException(
                    "document.chunk.max-chunk-size는 1 이상이어야 합니다.");
        }

        if (overlapSize < 0) {
            throw new IllegalArgumentException(
                    "document.chunk.overlap-size는 0 이상이어야 합니다.");
        }

        if (overlapSize >= maxChunkSize) {
            throw new IllegalArgumentException(
                    "document.chunk.overlap-size는 document.chunk.max-chunk-size보다 작아야 합니다.");
        }
    }
}
