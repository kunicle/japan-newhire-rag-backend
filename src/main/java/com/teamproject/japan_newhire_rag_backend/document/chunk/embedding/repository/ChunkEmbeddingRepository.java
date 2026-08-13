package com.teamproject.japan_newhire_rag_backend.document.chunk.embedding.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.document.chunk.embedding.entity.ChunkEmbedding;

public interface ChunkEmbeddingRepository extends JpaRepository<ChunkEmbedding, Long> {

    boolean existsByDocumentChunk_DocumentChunkIdAndAiModel_AiModelId(
            Long documentChunkId,
            Long aiModelId);
}
