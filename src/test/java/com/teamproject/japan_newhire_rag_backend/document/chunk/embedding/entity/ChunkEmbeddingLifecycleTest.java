package com.teamproject.japan_newhire_rag_backend.document.chunk.embedding.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.rag.model.entity.AiModel;

class ChunkEmbeddingLifecycleTest {

    private static final LocalDateTime EMBEDDED_AT = LocalDateTime.of(2026, 8, 13, 10, 0);

    @Test
    void createKeepsPendingDefaults() {
        ChunkEmbedding embedding = embedding();

        assertThat(embedding.getEmbeddingStatus()).isEqualTo("PENDING");
        assertThat(embedding.getEmbeddedAt()).isNull();
    }

    @Test
    void markCompletedChangesOnlyStatusAndEmbeddedAt() {
        DocumentChunk chunk = DocumentChunk.create(null, 1, null, null, "규정 내용", null);
        AiModel model = AiModel.create("provider", "embedding-model", "EMBEDDING", 1536);
        ChunkEmbedding embedding = ChunkEmbedding.create(chunk, model, "chunk-1-vector", 1536);

        embedding.markCompleted(EMBEDDED_AT);

        assertThat(embedding.getEmbeddingStatus()).isEqualTo("COMPLETED");
        assertThat(embedding.getEmbeddedAt()).isEqualTo(EMBEDDED_AT);
        assertThat(embedding.getDocumentChunk()).isSameAs(chunk);
        assertThat(embedding.getAiModel()).isSameAs(model);
        assertThat(embedding.getVectorReference()).isEqualTo("chunk-1-vector");
        assertThat(embedding.getEmbeddingDimension()).isEqualTo(1536);
    }

    private ChunkEmbedding embedding() {
        return ChunkEmbedding.create(null, null, "chunk-1-vector", 1536);
    }
}
