package com.teamproject.japan_newhire_rag_backend.document.chunk.embedding.entity;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.rag.model.entity.AiModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chunk_embedding")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChunkEmbedding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chunk_embedding_id")
    private Long chunkEmbeddingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_chunk_id", nullable = false)
    private DocumentChunk documentChunk;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_model_id", nullable = false)
    private AiModel aiModel;

    @Column(name = "vector_reference", nullable = false, length = 500)
    private String vectorReference;

    @Column(name = "embedding_dimension", nullable = false)
    private int embeddingDimension;

    @Column(name = "embedding_status", nullable = false, length = 20)
    private String embeddingStatus;

    @Column(name = "embedded_at")
    private LocalDateTime embeddedAt;

    private ChunkEmbedding(
            DocumentChunk documentChunk,
            AiModel aiModel,
            String vectorReference,
            int embeddingDimension) {
        this.documentChunk = documentChunk;
        this.aiModel = aiModel;
        this.vectorReference = vectorReference;
        this.embeddingDimension = embeddingDimension;
        this.embeddingStatus = "PENDING";
        this.embeddedAt = null;
    }

    public static ChunkEmbedding create(
            DocumentChunk documentChunk,
            AiModel aiModel,
            String vectorReference,
            int embeddingDimension) {
        return new ChunkEmbedding(
                documentChunk,
                aiModel,
                vectorReference,
                embeddingDimension);
    }
}
