package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.document.chunk.service.DocumentChunkPersistenceService;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJobDetail;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobDetailRepository;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

@Service
public class DocumentChunkingProcessingService {

    private final DocumentProcessingJobRepository documentProcessingJobRepository;
    private final DocumentProcessingJobDetailRepository documentProcessingJobDetailRepository;
    private final DocumentChunkPersistenceService documentChunkPersistenceService;

    public DocumentChunkingProcessingService(
            DocumentProcessingJobRepository documentProcessingJobRepository,
            DocumentProcessingJobDetailRepository documentProcessingJobDetailRepository,
            DocumentChunkPersistenceService documentChunkPersistenceService) {
        this.documentProcessingJobRepository = documentProcessingJobRepository;
        this.documentProcessingJobDetailRepository = documentProcessingJobDetailRepository;
        this.documentChunkPersistenceService = documentChunkPersistenceService;
    }

    @Transactional
    public DocumentProcessingJob processChunking(
            DocumentVersion documentVersion,
            String text,
            int maxChunkSize,
            int overlapSize,
            Long createdBy) {
        DocumentProcessingJob newJob = DocumentProcessingJob.create(documentVersion, createdBy);
        DocumentProcessingJob job = documentProcessingJobRepository.save(newJob);
        job.markProcessing(LocalDateTime.now());

        List<DocumentChunk> savedChunks = documentChunkPersistenceService.splitAndSave(
                documentVersion,
                text,
                maxChunkSize,
                overlapSize);
        job.recordTotalChunkCount(savedChunks.size());

        LocalDateTime processedAt = LocalDateTime.now();
        List<DocumentProcessingJobDetail> details = savedChunks.stream()
                .map(chunk -> completedChunkingDetail(job, chunk, processedAt))
                .toList();
        documentProcessingJobDetailRepository.saveAll(details);

        return job;
    }

    private DocumentProcessingJobDetail completedChunkingDetail(
            DocumentProcessingJob job,
            DocumentChunk chunk,
            LocalDateTime processedAt) {
        DocumentProcessingJobDetail detail = DocumentProcessingJobDetail.create(
                job,
                chunk,
                "CHUNKING");
        detail.markCompleted(processedAt);
        return detail;
    }
}
