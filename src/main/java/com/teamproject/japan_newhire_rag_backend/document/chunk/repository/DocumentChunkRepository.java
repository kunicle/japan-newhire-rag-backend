package com.teamproject.japan_newhire_rag_backend.document.chunk.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentVersion_DocumentVersionIdAndChunkStatusOrderByChunkSequenceAsc(
            Long documentVersionId,
            String chunkStatus);

    @Query("""
            SELECT c
            FROM DocumentChunk c
            JOIN FETCH c.documentVersion v
            JOIN FETCH v.document d
            WHERE c.documentChunkId IN :documentChunkIds
            """)
    List<DocumentChunk> findAllWithDocumentAndVersionByDocumentChunkIdIn(
            @Param("documentChunkIds") Collection<Long> documentChunkIds);
}
