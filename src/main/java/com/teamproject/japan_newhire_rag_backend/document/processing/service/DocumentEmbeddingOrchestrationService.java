package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.teamproject.japan_newhire_rag_backend.document.chunk.embedding.repository.ChunkEmbeddingRepository;
import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.document.chunk.repository.DocumentChunkRepository;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJobDetail;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobDetailRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.api.SystemErrorRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.api.SystemErrorRecordService;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiEmbeddingCallMetadataClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiEmbeddingClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiHttpAttempt;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiHttpCallException;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiHttpExecution;
import com.teamproject.japan_newhire_rag_backend.rag.ai.EmbeddingRequest;
import com.teamproject.japan_newhire_rag_backend.rag.ai.EmbeddingResult;
import com.teamproject.japan_newhire_rag_backend.rag.model.EmbeddingModelSelection;
import com.teamproject.japan_newhire_rag_backend.rag.model.service.EmbeddingModelSelectionService;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.ExternalApiCallLogCommand;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.ExternalApiCallLogService;

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
    private final ExternalApiCallLogService externalApiCallLogService;
    private final SystemErrorRecordService systemErrorRecordService;

    @Autowired
    public DocumentEmbeddingOrchestrationService(
            DocumentChunkRepository documentChunkRepository,
            ChunkEmbeddingRepository chunkEmbeddingRepository,
            DocumentProcessingJobDetailRepository detailRepository,
            EmbeddingModelSelectionService modelSelectionService,
            AiEmbeddingClient aiEmbeddingClient,
            ChunkEmbeddingProcessingRecorder recorder,
            ExternalApiCallLogService externalApiCallLogService,
            SystemErrorRecordService systemErrorRecordService) {
        this.documentChunkRepository = documentChunkRepository;
        this.chunkEmbeddingRepository = chunkEmbeddingRepository;
        this.detailRepository = detailRepository;
        this.modelSelectionService = modelSelectionService;
        this.aiEmbeddingClient = aiEmbeddingClient;
        this.recorder = recorder;
        this.externalApiCallLogService = externalApiCallLogService;
        this.systemErrorRecordService = systemErrorRecordService;
    }

    public DocumentEmbeddingOrchestrationService(
            DocumentChunkRepository documentChunkRepository,
            ChunkEmbeddingRepository chunkEmbeddingRepository,
            DocumentProcessingJobDetailRepository detailRepository,
            EmbeddingModelSelectionService modelSelectionService,
            AiEmbeddingClient aiEmbeddingClient,
            ChunkEmbeddingProcessingRecorder recorder) {
        this(documentChunkRepository, chunkEmbeddingRepository, detailRepository, modelSelectionService,
                aiEmbeddingClient, recorder, null, null);
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
            EmbeddingRequest request = new EmbeddingRequest(
                    chunk.getDocumentChunkId(),
                    documentVersion.getDocumentVersionId(),
                    chunk.getChunkContent(),
                    selection.providerName(),
                    selection.modelName());
            EmbeddingResult result;
            List<AiHttpAttempt> attempts = List.of();
            if (aiEmbeddingClient instanceof AiEmbeddingCallMetadataClient metadataClient) {
                AiHttpExecution<EmbeddingResult> execution = metadataClient.embedWithMetadata(request);
                result = execution.result();
                attempts = execution.attempts();
            } else {
                result = aiEmbeddingClient.embed(request);
            }
            recordAttempts(job, selection.aiModelId(), attempts);
            validateResultDimension(selection.embeddingDimension(), result.embeddingDimension());
            recorder.recordSuccess(
                    detail,
                    job,
                    chunk,
                    selection,
                    result,
                    LocalDateTime.now());
        } catch (RuntimeException exception) {
            if (exception instanceof AiHttpCallException aiException) {
                Long failedLogId = recordAttempts(job, selection.aiModelId(), aiException.getAttempts());
                AiHttpAttempt failedAttempt = aiException.getAttempts().get(aiException.getAttempts().size() - 1);
                if (systemErrorRecordService != null) systemErrorRecordService.record(new SystemErrorRecordCommand(
                        job.getCreatedBy(), failedLogId, "EMBEDDING_API", failedAttempt.errorType(),
                        failedAttempt.errorCode(), failedAttempt.attemptNumber() - 1,
                        failedAttempt.errorMessage(), failedAttempt.completedAt()));
            }
            recorder.recordFailure(
                    detail,
                    job,
                    failureReason(exception),
                    LocalDateTime.now());
        }
    }

    private Long recordAttempts(
            DocumentProcessingJob job, Long aiModelId, List<AiHttpAttempt> attempts) {
        Long lastLogId = null;
        if (externalApiCallLogService == null) return null;
        for (AiHttpAttempt attempt : attempts) {
            lastLogId = externalApiCallLogService.record(new ExternalApiCallLogCommand(
                    aiModelId, null, job.getDocumentProcessingJobId(), "EMBEDDING", attempt.callStatus(),
                    attempt.attemptNumber(), attempt.httpStatusCode(), attempt.errorType(),
                    attempt.errorMessage(), attempt.durationMs(), attempt.requestedAt(), attempt.completedAt()));
        }
        return lastLogId;
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
