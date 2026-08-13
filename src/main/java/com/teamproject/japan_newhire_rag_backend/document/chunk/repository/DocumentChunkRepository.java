package com.teamproject.japan_newhire_rag_backend.document.chunk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentVersion_DocumentVersionIdAndChunkStatusOrderByChunkSequenceAsc(
            Long documentVersionId,
            String chunkStatus);
}
