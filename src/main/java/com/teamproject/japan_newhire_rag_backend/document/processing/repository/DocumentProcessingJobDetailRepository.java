package com.teamproject.japan_newhire_rag_backend.document.processing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJobDetail;

public interface DocumentProcessingJobDetailRepository
        extends JpaRepository<DocumentProcessingJobDetail, Long> {

    boolean existsByDocumentProcessingJob_DocumentProcessingJobIdAndDocumentChunk_DocumentChunkIdAndProcessingStepAndProcessingStatus(
            Long documentProcessingJobId,
            Long documentChunkId,
            String processingStep,
            String processingStatus);
}
