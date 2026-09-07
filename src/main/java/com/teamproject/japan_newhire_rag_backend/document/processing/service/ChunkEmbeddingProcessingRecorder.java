package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.chunk.embedding.entity.ChunkEmbedding;
import com.teamproject.japan_newhire_rag_backend.document.chunk.embedding.repository.ChunkEmbeddingRepository;
import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJobDetail;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobDetailRepository;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobRepository;
import com.teamproject.japan_newhire_rag_backend.rag.ai.EmbeddingResult;
import com.teamproject.japan_newhire_rag_backend.rag.model.EmbeddingModelSelection;
import com.teamproject.japan_newhire_rag_backend.rag.model.entity.AiModel;
import com.teamproject.japan_newhire_rag_backend.rag.model.repository.AiModelRepository;

@Service
public class ChunkEmbeddingProcessingRecorder {

    private final DocumentProcessingJobRepository jobRepository;
    private final DocumentProcessingJobDetailRepository detailRepository;
    private final ChunkEmbeddingRepository chunkEmbeddingRepository;
    private final AiModelRepository aiModelRepository;

    public ChunkEmbeddingProcessingRecorder(
            DocumentProcessingJobRepository jobRepository,
            DocumentProcessingJobDetailRepository detailRepository,
            ChunkEmbeddingRepository chunkEmbeddingRepository,
            AiModelRepository aiModelRepository) {
        this.jobRepository = jobRepository;
        this.detailRepository = detailRepository;
        this.chunkEmbeddingRepository = chunkEmbeddingRepository;
        this.aiModelRepository = aiModelRepository;
    }

    @Transactional
    public DocumentProcessingJobDetail beginAttempt(
            DocumentProcessingJob job,
            DocumentChunk chunk) {
        DocumentProcessingJobDetail detail =
                DocumentProcessingJobDetail.create(job, chunk, "EMBEDDING");
        detail.markProcessing();
        return detailRepository.save(detail);
    }

    @Transactional
    public void recordExistingEmbeddingSuccess(
            DocumentProcessingJob job,
            DocumentChunk chunk,
            LocalDateTime completedAt) {
        DocumentProcessingJobDetail detail =
                DocumentProcessingJobDetail.create(job, chunk, "EMBEDDING");
        detail.markProcessing();
        detail.markCompleted(completedAt);
        detailRepository.save(detail);
        job.incrementCompletedChunkCount();
        jobRepository.save(job);
    }

    @Transactional
    public void recordSuccess(
            DocumentProcessingJobDetail detail,
            DocumentProcessingJob job,
            DocumentChunk chunk,
            EmbeddingModelSelection selection,
            EmbeddingResult result,
            LocalDateTime completedAt) {
        AiModel aiModel = aiModelRepository.getReferenceById(selection.aiModelId());
        ChunkEmbedding embedding = ChunkEmbedding.create(
                chunk,
                aiModel,
                result.vectorReference(),
                result.embeddingDimension());
        embedding.markCompleted(completedAt);
        chunkEmbeddingRepository.save(embedding);

        detail.markCompleted(completedAt);
        detailRepository.save(detail);
        job.incrementCompletedChunkCount();
        jobRepository.save(job);
    }

    @Transactional
    public void recordFailure(
            DocumentProcessingJobDetail detail,
            DocumentProcessingJob job,
            String reason,
            LocalDateTime failedAt) {
        detail.markFailed(reason, failedAt);
        detailRepository.save(detail);
        job.incrementFailedChunkCount();
        jobRepository.save(job);
    }

    @Transactional
    public void recordJobFailure(
            DocumentProcessingJob job,
            String reason,
            LocalDateTime failedAt) {
        job.markFailed(reason, failedAt);
        jobRepository.save(job);
    }

    @Transactional
    public DocumentProcessingJob finalizeJob(
            DocumentProcessingJob job,
            LocalDateTime completedAt) {
        if (job.getFailedChunkCount() == 0
                && job.getCompletedChunkCount() == job.getTotalChunkCount()) {
            job.markCompleted(completedAt);
        } else {
            job.markFailed("Embedding processing failed", completedAt);
        }
        return jobRepository.save(job);
    }
}
