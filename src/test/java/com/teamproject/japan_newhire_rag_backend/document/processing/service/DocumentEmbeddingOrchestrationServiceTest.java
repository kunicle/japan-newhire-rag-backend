package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.chunk.embedding.entity.ChunkEmbedding;
import com.teamproject.japan_newhire_rag_backend.document.chunk.embedding.repository.ChunkEmbeddingRepository;
import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.document.chunk.repository.DocumentChunkRepository;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJobDetail;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobDetailRepository;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiEmbeddingClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.EmbeddingRequest;
import com.teamproject.japan_newhire_rag_backend.rag.ai.EmbeddingResult;
import com.teamproject.japan_newhire_rag_backend.rag.model.EmbeddingModelSelection;
import com.teamproject.japan_newhire_rag_backend.rag.model.entity.AiModel;
import com.teamproject.japan_newhire_rag_backend.rag.model.repository.AiModelRepository;
import com.teamproject.japan_newhire_rag_backend.rag.model.service.EmbeddingModelSelectionService;

class DocumentEmbeddingOrchestrationServiceTest {

    private final DocumentChunkRepository chunkRepository = mock(DocumentChunkRepository.class);
    private final ChunkEmbeddingRepository embeddingRepository = mock(ChunkEmbeddingRepository.class);
    private final DocumentProcessingJobDetailRepository detailRepository =
            mock(DocumentProcessingJobDetailRepository.class);
    private final DocumentProcessingJobRepository jobRepository =
            mock(DocumentProcessingJobRepository.class);
    private final AiModelRepository aiModelRepository = mock(AiModelRepository.class);
    private final EmbeddingModelSelectionService selectionService =
            mock(EmbeddingModelSelectionService.class);
    private final AiEmbeddingClient embeddingClient = mock(AiEmbeddingClient.class);
    private final ChunkEmbeddingProcessingRecorder recorder =
            new ChunkEmbeddingProcessingRecorder(
                    jobRepository,
                    detailRepository,
                    embeddingRepository,
                    aiModelRepository);
    private final DocumentEmbeddingOrchestrationService service =
            new DocumentEmbeddingOrchestrationService(
                    chunkRepository,
                    embeddingRepository,
                    detailRepository,
                    selectionService,
                    embeddingClient,
                    recorder);

    private DocumentVersion version;
    private DocumentProcessingJob job;

    @BeforeEach
    void setUp() {
        version = DocumentVersion.create(
                null, "v1", LocalDate.of(2026, 1, 1), null, "rules.txt", "/rules.txt", 1L);
        ReflectionTestUtils.setField(version, "documentVersionId", 100L);
        job = DocumentProcessingJob.create(version, 1L);
        ReflectionTestUtils.setField(job, "documentProcessingJobId", 200L);
        when(jobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(detailRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(embeddingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(selectionService.selectDefaultEmbeddingModel()).thenReturn(selection(1536));
    }

    @Test
    void processesChunksInSequenceWithExactRequestsAndPersistsCompletedEmbeddings() {
        DocumentChunk first = chunk(1L, 1, "first");
        DocumentChunk second = chunk(2L, 2, "second");
        job.recordTotalChunkCount(2);
        when(chunkRepository
                .findByDocumentVersion_DocumentVersionIdAndChunkStatusOrderByChunkSequenceAsc(
                        100L, "ACTIVE"))
                .thenReturn(List.of(first, second));
        when(embeddingClient.embed(any())).thenAnswer(invocation -> {
            EmbeddingRequest request = invocation.getArgument(0);
            return new EmbeddingResult("vector-" + request.documentChunkId(), 1536);
        });
        when(aiModelRepository.getReferenceById(10L)).thenReturn(mock(AiModel.class));

        DocumentProcessingJob result = service.processEmbeddings(job, version);

        verify(selectionService).selectDefaultEmbeddingModel();
        ArgumentCaptor<EmbeddingRequest> requests = ArgumentCaptor.forClass(EmbeddingRequest.class);
        verify(embeddingClient, times(2)).embed(requests.capture());
        assertThat(requests.getAllValues()).containsExactly(
                new EmbeddingRequest(1L, 100L, "first", "provider-a", "model-a"),
                new EmbeddingRequest(2L, 100L, "second", "provider-a", "model-a"));
        ArgumentCaptor<ChunkEmbedding> embeddings = ArgumentCaptor.forClass(ChunkEmbedding.class);
        verify(embeddingRepository, times(2)).save(embeddings.capture());
        assertThat(embeddings.getAllValues()).allSatisfy(embedding -> {
            assertThat(embedding.getEmbeddingStatus()).isEqualTo("COMPLETED");
            assertThat(embedding.getEmbeddedAt()).isNotNull();
            assertThat(embedding.getEmbeddingDimension()).isEqualTo(1536);
            assertThat(embedding.getVectorReference()).startsWith("vector-");
        });
        assertThat(result.getCompletedChunkCount()).isEqualTo(2);
        assertThat(result.getFailedChunkCount()).isZero();
        assertThat(result.getProcessingStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void continuesAfterClientFailureAndFinalizesParentAsFailed() {
        DocumentChunk first = chunk(1L, 1, "first");
        DocumentChunk second = chunk(2L, 2, "second");
        DocumentChunk third = chunk(3L, 3, "third");
        job.recordTotalChunkCount(3);
        when(chunkRepository
                .findByDocumentVersion_DocumentVersionIdAndChunkStatusOrderByChunkSequenceAsc(
                        100L, "ACTIVE"))
                .thenReturn(List.of(first, second, third));
        when(embeddingClient.embed(any()))
                .thenReturn(new EmbeddingResult("vector-1", 1536))
                .thenThrow(new IllegalStateException("remote failure"))
                .thenReturn(new EmbeddingResult("vector-3", 1536));
        when(aiModelRepository.getReferenceById(10L)).thenReturn(mock(AiModel.class));

        service.processEmbeddings(job, version);

        verify(embeddingClient, times(3)).embed(any());
        verify(embeddingRepository, times(2)).save(any());
        assertThat(job.getCompletedChunkCount()).isEqualTo(2);
        assertThat(job.getFailedChunkCount()).isEqualTo(1);
        assertThat(job.getProcessingStatus()).isEqualTo("FAILED");
        assertThat(job.getRetryCount()).isZero();
    }

    @Test
    void rejectsDimensionMismatchWithoutSavingEmbeddingAndContinues() {
        DocumentChunk first = chunk(1L, 1, "first");
        DocumentChunk second = chunk(2L, 2, "second");
        job.recordTotalChunkCount(2);
        when(chunkRepository
                .findByDocumentVersion_DocumentVersionIdAndChunkStatusOrderByChunkSequenceAsc(
                        100L, "ACTIVE"))
                .thenReturn(List.of(first, second));
        when(embeddingClient.embed(any()))
                .thenReturn(new EmbeddingResult("wrong", 3072))
                .thenReturn(new EmbeddingResult("right", 1536));
        when(aiModelRepository.getReferenceById(10L)).thenReturn(mock(AiModel.class));

        service.processEmbeddings(job, version);

        verify(embeddingClient, times(2)).embed(any());
        verify(embeddingRepository, times(1)).save(any());
        assertThat(job.getCompletedChunkCount()).isEqualTo(1);
        assertThat(job.getFailedChunkCount()).isEqualTo(1);
        assertThat(job.getProcessingStatus()).isEqualTo("FAILED");
    }

    @Test
    void failsParentBeforeChunkLookupWhenModelDimensionIsNull() {
        when(selectionService.selectDefaultEmbeddingModel()).thenReturn(selection(null));

        assertThatThrownBy(() -> service.processEmbeddings(job, version))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(embeddingClient);
        verify(embeddingRepository, never()).save(any());
        verify(chunkRepository, never())
                .findByDocumentVersion_DocumentVersionIdAndChunkStatusOrderByChunkSequenceAsc(
                        anyLong(), any());
        assertThat(job.getProcessingStatus()).isEqualTo("FAILED");
    }

    @Test
    void skipsChunkCompletelyWhenCurrentJobAlreadyCompletedIt() {
        DocumentChunk chunk = chunk(1L, 1, "content");
        job.recordTotalChunkCount(1);
        job.incrementCompletedChunkCount();
        when(chunkRepository
                .findByDocumentVersion_DocumentVersionIdAndChunkStatusOrderByChunkSequenceAsc(
                        100L, "ACTIVE"))
                .thenReturn(List.of(chunk));
        when(detailRepository
                .existsByDocumentProcessingJob_DocumentProcessingJobIdAndDocumentChunk_DocumentChunkIdAndProcessingStepAndProcessingStatus(
                        200L, 1L, "EMBEDDING", "COMPLETED"))
                .thenReturn(true);

        service.processEmbeddings(job, version);

        verifyNoInteractions(embeddingClient);
        verify(embeddingRepository, never()).save(any());
        verify(detailRepository, never()).save(any());
        assertThat(job.getCompletedChunkCount()).isEqualTo(1);
    }

    @Test
    void recordsCurrentJobSuccessWithoutHttpWhenEmbeddingAlreadyExists() {
        DocumentChunk chunk = chunk(1L, 1, "content");
        job.recordTotalChunkCount(1);
        when(chunkRepository
                .findByDocumentVersion_DocumentVersionIdAndChunkStatusOrderByChunkSequenceAsc(
                        100L, "ACTIVE"))
                .thenReturn(List.of(chunk));
        when(embeddingRepository
                .existsByDocumentChunk_DocumentChunkIdAndAiModel_AiModelId(1L, 10L))
                .thenReturn(true);

        service.processEmbeddings(job, version);

        verifyNoInteractions(embeddingClient);
        verify(embeddingRepository, never()).save(any());
        ArgumentCaptor<DocumentProcessingJobDetail> detail =
                ArgumentCaptor.forClass(DocumentProcessingJobDetail.class);
        verify(detailRepository).save(detail.capture());
        assertThat(detail.getValue().getProcessingStatus()).isEqualTo("COMPLETED");
        assertThat(job.getCompletedChunkCount()).isEqualTo(1);
        assertThat(job.getProcessingStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void keepsHttpOutsideTransactionAndRecorderMutationsTransactional() throws Exception {
        Method orchestration = DocumentEmbeddingOrchestrationService.class.getDeclaredMethod(
                "processEmbeddings", DocumentProcessingJob.class, DocumentVersion.class);
        assertThat(orchestration.getAnnotation(Transactional.class)).isNull();

        for (String methodName : List.of(
                "beginAttempt",
                "recordExistingEmbeddingSuccess",
                "recordSuccess",
                "recordFailure",
                "recordJobFailure",
                "finalizeJob")) {
            Method method = java.util.Arrays.stream(
                            ChunkEmbeddingProcessingRecorder.class.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(methodName))
                    .findFirst()
                    .orElseThrow();
            assertThat(method.getAnnotation(Transactional.class)).isNotNull();
        }
    }

    private DocumentChunk chunk(Long id, int sequence, String content) {
        DocumentChunk chunk = DocumentChunk.create(version, sequence, null, null, content, 1);
        ReflectionTestUtils.setField(chunk, "documentChunkId", id);
        return chunk;
    }

    private EmbeddingModelSelection selection(Integer dimension) {
        return new EmbeddingModelSelection(10L, "provider-a", "model-a", dimension);
    }
}
