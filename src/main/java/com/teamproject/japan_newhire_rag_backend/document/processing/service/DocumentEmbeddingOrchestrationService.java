package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.teamproject.japan_newhire_rag_backend.document.chunk.embedding.repository.ChunkEmbeddingRepository;
import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.document.chunk.repository.DocumentChunkRepository;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJobDetail;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobDetailRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiEmbeddingClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.EmbeddingRequest;
import com.teamproject.japan_newhire_rag_backend.rag.ai.EmbeddingResult;
import com.teamproject.japan_newhire_rag_backend.rag.model.EmbeddingModelSelection;
import com.teamproject.japan_newhire_rag_backend.rag.model.service.EmbeddingModelSelectionService;

@Service
public class DocumentEmbeddingOrchestrationService {

    private static final String ACTIVE_CHUNK_STATUS = "ACTIVE";
    private static final String EMBEDDING_STEP = "EMBEDDING";
    private static final String COMPLETED_STATUS = "COMPLETED";

    private final DocumentChunkRepository documentChunkRepository;
    private final ChunkEmbeddingRepository chunkEmbeddingRepository;
    private final DocumentProcessingJobDetailRepository detailRepository;
    private final EmbeddingModelSelectionService modelSelectionService;
    private final AiEmbeddingClient aiEmbeddingClient;
    private final ChunkEmbeddingProcessingRecorder recorder;

    public DocumentEmbeddingOrchestrationService(
            DocumentChunkRepository documentChunkRepository,
            ChunkEmbeddingRepository chunkEmbeddingRepository,
            DocumentProcessingJobDetailRepository detailRepository,
            EmbeddingModelSelectionService modelSelectionService,
            AiEmbeddingClient aiEmbeddingClient,
            ChunkEmbeddingProcessingRecorder recorder) {
        this.documentChunkRepository = documentChunkRepository;
        this.chunkEmbeddingRepository = chunkEmbeddingRepository;
        this.detailRepository = detailRepository;
        this.modelSelectionService = modelSelectionService;
        this.aiEmbeddingClient = aiEmbeddingClient;
        this.recorder = recorder;
    }

    public DocumentProcessingJob processEmbeddings(
            DocumentProcessingJob job,
            DocumentVersion documentVersion) {
        EmbeddingModelSelection selection;
        try {
            selection = modelSelectionService.selectDefaultEmbeddingModel();
            validateEmbeddingDimension(selection.embeddingDimension());
        } catch (RuntimeException exception) {
            recorder.recordJobFailure(
                    job,
                    "Embedding model configuration failed: " + exception.getClass().getSimpleName(),
                    LocalDateTime.now());
            throw exception;
        }

        List<DocumentChunk> chunks = documentChunkRepository
                .findByDocumentVersion_DocumentVersionIdAndChunkStatusOrderByChunkSequenceAsc(
                        documentVersion.getDocumentVersionId(),
                        ACTIVE_CHUNK_STATUS);
        for (DocumentChunk chunk : chunks) {
            processChunk(job, documentVersion, chunk, selection);
        }
        return recorder.finalizeJob(job, LocalDateTime.now());
    }

    private void processChunk(
            DocumentProcessingJob job,
            DocumentVersion documentVersion,
            DocumentChunk chunk,
            EmbeddingModelSelection selection) {
        if (isAlreadyCompletedForCurrentJob(job, chunk)) {
            return;
        }
        if (chunkEmbeddingRepository
                .existsByDocumentChunk_DocumentChunkIdAndAiModel_AiModelId(
                        chunk.getDocumentChunkId(),
                        selection.aiModelId())) {
            recorder.recordExistingEmbeddingSuccess(job, chunk, LocalDateTime.now());
            return;
        }

        DocumentProcessingJobDetail detail = recorder.beginAttempt(job, chunk);
        try {
            EmbeddingResult result = aiEmbeddingClient.embed(new EmbeddingRequest(
                    chunk.getDocumentChunkId(),
                    documentVersion.getDocumentVersionId(),
                    chunk.getChunkContent(),
                    selection.providerName(),
                    selection.modelName()));
            validateResultDimension(selection.embeddingDimension(), result.embeddingDimension());
            recorder.recordSuccess(
                    detail,
                    job,
                    chunk,
                    selection,
                    result,
                    LocalDateTime.now());
        } catch (RuntimeException exception) {
            recorder.recordFailure(
                    detail,
                    job,
                    failureReason(exception),
                    LocalDateTime.now());
        }
    }

    private boolean isAlreadyCompletedForCurrentJob(
            DocumentProcessingJob job,
            DocumentChunk chunk) {
        return detailRepository
                .existsByDocumentProcessingJob_DocumentProcessingJobIdAndDocumentChunk_DocumentChunkIdAndProcessingStepAndProcessingStatus(
                        job.getDocumentProcessingJobId(),
                        chunk.getDocumentChunkId(),
                        EMBEDDING_STEP,
                        COMPLETED_STATUS);
    }

    private void validateEmbeddingDimension(Integer embeddingDimension) {
        if (embeddingDimension == null || embeddingDimension <= 0) {
            throw new IllegalStateException("Embedding model dimension must be positive");
        }
    }

    private void validateResultDimension(int expectedDimension, int actualDimension) {
        if (expectedDimension != actualDimension) {
            throw new IllegalStateException(
                    "Embedding dimension mismatch: expected="
                            + expectedDimension
                            + ", actual="
                            + actualDimension);
        }
    }

    private String failureReason(RuntimeException exception) {
        if (exception.getMessage() != null
                && exception.getMessage().startsWith("Embedding dimension mismatch:")) {
            return exception.getMessage();
        }
        return "Embedding request failed: " + exception.getClass().getSimpleName();
    }
}
