package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.entity.Document;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;
import com.teamproject.japan_newhire_rag_backend.document.service.DocumentLifecycleService;
import com.teamproject.japan_newhire_rag_backend.document.storage.DocumentStorageService;
import com.teamproject.japan_newhire_rag_backend.document.validation.InvalidTxtDocumentException;
import com.teamproject.japan_newhire_rag_backend.document.validation.TxtDocumentValidator;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

class DocumentIngestionOrchestrationServiceTest {

    private final TxtDocumentValidator validator = mock(TxtDocumentValidator.class);
    private final DocumentStorageService storageService = mock(DocumentStorageService.class);
    private final DocumentLifecycleService lifecycleService = mock(DocumentLifecycleService.class);
    private final DocumentChunkingProcessingService chunkingService =
            mock(DocumentChunkingProcessingService.class);
    private final DocumentEmbeddingOrchestrationService embeddingService =
            mock(DocumentEmbeddingOrchestrationService.class);
    private final DocumentIngestionOrchestrationService service =
            new DocumentIngestionOrchestrationService(
                    validator,
                    storageService,
                    lifecycleService,
                    chunkingService,
                    embeddingService);

    @Test
    void ingestsStoredDocumentThroughChunkingAndEmbedding() {
        String original = "신입사원은 최초 로그인 후 비밀번호를 변경합니다.";
        byte[] content = original.getBytes(StandardCharsets.UTF_8);
        Document document = mock(Document.class);
        DocumentVersion documentVersion = mock(DocumentVersion.class);
        DocumentProcessingJob processingJob = mock(DocumentProcessingJob.class);
        DocumentProcessingJob finalJob = mock(DocumentProcessingJob.class);
        when(document.getDocumentId()).thenReturn(99L);
        when(documentVersion.getDocument()).thenReturn(document);
        when(documentVersion.getDocumentVersionId()).thenReturn(88L);
        when(finalJob.getDocumentProcessingJobId()).thenReturn(77L);
        when(finalJob.getProcessingStatus()).thenReturn("COMPLETED");
        when(storageService.store("신입사원.txt", content)).thenReturn("stored-uuid.txt");
        when(lifecycleService.createDocumentWithInitialVersion(
                10L,
                "신입사원 규정",
                "최초 로그인 안내",
                "2026-01",
                "신입사원.txt",
                "stored-uuid.txt",
                content.length,
                30L)).thenReturn(documentVersion);
        when(chunkingService.processChunking(documentVersion, original, 500, 50, 30L))
                .thenReturn(processingJob);
        when(embeddingService.processEmbeddings(processingJob, documentVersion))
                .thenReturn(finalJob);

        DocumentIngestionResult result = service.ingest(
                10L,
                "신입사원 규정",
                "최초 로그인 안내",
                "2026-01",
                "신입사원.txt",
                content,
                500,
                50,
                30L);

        assertThat(result.documentId()).isEqualTo(99L);
        assertThat(result.documentVersionId()).isEqualTo(88L);
        assertThat(result.documentProcessingJobId()).isEqualTo(77L);
        assertThat(result.processingStatus()).isEqualTo("COMPLETED");
        InOrder order = inOrder(
                validator, storageService, lifecycleService, chunkingService, embeddingService);
        order.verify(validator).validate("신입사원.txt", content);
        order.verify(storageService).store("신입사원.txt", content);
        order.verify(lifecycleService).createDocumentWithInitialVersion(
                10L,
                "신입사원 규정",
                "최초 로그인 안내",
                "2026-01",
                "신입사원.txt",
                "stored-uuid.txt",
                content.length,
                30L);
        order.verify(chunkingService).processChunking(documentVersion, original, 500, 50, 30L);
        order.verify(embeddingService).processEmbeddings(processingJob, documentVersion);
    }

    @Test
    void doesNotPersistOrProcessWhenTxtValidationFails() {
        byte[] content = "내용".getBytes(StandardCharsets.UTF_8);
        InvalidTxtDocumentException failure =
                new InvalidTxtDocumentException("TXT 파일만 업로드할 수 있습니다.");
        doThrow(failure).when(validator).validate("규정.pdf", content);

        assertThatThrownBy(() -> ingest("규정.pdf", content))
                .isSameAs(failure);

        verifyNoInteractions(storageService);
        verifyNoInteractions(lifecycleService, chunkingService, embeddingService);
    }

    @Test
    void doesNotPersistOrProcessWhenStorageFails() {
        byte[] content = "내용".getBytes(StandardCharsets.UTF_8);
        IllegalStateException failure = new IllegalStateException("storage failure");
        when(storageService.store("규정.txt", content)).thenThrow(failure);

        assertThatThrownBy(() -> ingest("규정.txt", content))
                .isSameAs(failure);

        verify(storageService, never()).delete(anyString());
        verifyNoInteractions(lifecycleService, chunkingService, embeddingService);
    }

    @Test
    void deletesStoredFileWhenDocumentLifecycleFails() {
        byte[] content = "내용".getBytes(StandardCharsets.UTF_8);
        IllegalArgumentException failure = new IllegalArgumentException("category failure");
        when(storageService.store("규정.txt", content)).thenReturn("stored-uuid.txt");
        when(lifecycleService.createDocumentWithInitialVersion(
                10L, "규정", null, "v1", "규정.txt", "stored-uuid.txt", content.length, 30L))
                .thenThrow(failure);

        assertThatThrownBy(() -> ingest("규정.txt", content))
                .isSameAs(failure);

        verify(storageService).delete("stored-uuid.txt");
        verifyNoInteractions(chunkingService, embeddingService);
    }

    @Test
    void preservesLifecycleFailureWhenCleanupAlsoFails() {
        byte[] content = "내용".getBytes(StandardCharsets.UTF_8);
        IllegalArgumentException lifecycleFailure = new IllegalArgumentException("category failure");
        IllegalStateException cleanupFailure = new IllegalStateException("cleanup failure");
        when(storageService.store("규정.txt", content)).thenReturn("stored-uuid.txt");
        when(lifecycleService.createDocumentWithInitialVersion(
                10L, "규정", null, "v1", "규정.txt", "stored-uuid.txt", content.length, 30L))
                .thenThrow(lifecycleFailure);
        doThrow(cleanupFailure).when(storageService).delete("stored-uuid.txt");

        assertThatThrownBy(() -> ingest("규정.txt", content))
                .isSameAs(lifecycleFailure);

        assertThat(lifecycleFailure.getSuppressed()).containsExactly(cleanupFailure);
        verify(storageService).delete("stored-uuid.txt");
        verifyNoInteractions(chunkingService, embeddingService);
    }

    @Test
    void doesNotDeleteStoredFileOrEmbedWhenChunkingFails() {
        byte[] content = "내용".getBytes(StandardCharsets.UTF_8);
        String text = new String(content, StandardCharsets.UTF_8);
        DocumentVersion documentVersion = mock(DocumentVersion.class);
        IllegalStateException failure = new IllegalStateException("chunking failure");
        when(storageService.store("규정.txt", content)).thenReturn("stored-uuid.txt");
        when(lifecycleService.createDocumentWithInitialVersion(
                10L, "규정", null, "v1", "규정.txt", "stored-uuid.txt", content.length, 30L))
                .thenReturn(documentVersion);
        when(chunkingService.processChunking(documentVersion, text, 500, 50, 30L))
                .thenThrow(failure);

        assertThatThrownBy(() -> ingest("규정.txt", content))
                .isSameAs(failure);

        verify(storageService, never()).delete(anyString());
        verifyNoInteractions(embeddingService);
    }

    @Test
    void doesNotDeleteStoredFileWhenEmbeddingFails() {
        byte[] content = "내용".getBytes(StandardCharsets.UTF_8);
        String text = new String(content, StandardCharsets.UTF_8);
        DocumentVersion documentVersion = mock(DocumentVersion.class);
        DocumentProcessingJob processingJob = mock(DocumentProcessingJob.class);
        IllegalStateException failure = new IllegalStateException("embedding failure");
        when(storageService.store("규정.txt", content)).thenReturn("stored-uuid.txt");
        when(lifecycleService.createDocumentWithInitialVersion(
                10L, "규정", null, "v1", "규정.txt", "stored-uuid.txt", content.length, 30L))
                .thenReturn(documentVersion);
        when(chunkingService.processChunking(documentVersion, text, 500, 50, 30L))
                .thenReturn(processingJob);
        when(embeddingService.processEmbeddings(processingJob, documentVersion))
                .thenThrow(failure);

        assertThatThrownBy(() -> ingest("규정.txt", content))
                .isSameAs(failure);

        verify(storageService, never()).delete(anyString());
    }

    @Test
    void resultReflectsFinalJobIdAndStatus() {
        byte[] content = "내용".getBytes(StandardCharsets.UTF_8);
        String text = new String(content, StandardCharsets.UTF_8);
        Document document = mock(Document.class);
        DocumentVersion documentVersion = mock(DocumentVersion.class);
        DocumentProcessingJob processingJob = mock(DocumentProcessingJob.class);
        DocumentProcessingJob failedJob = mock(DocumentProcessingJob.class);
        when(document.getDocumentId()).thenReturn(101L);
        when(documentVersion.getDocument()).thenReturn(document);
        when(documentVersion.getDocumentVersionId()).thenReturn(202L);
        when(failedJob.getDocumentProcessingJobId()).thenReturn(303L);
        when(failedJob.getProcessingStatus()).thenReturn("FAILED");
        when(storageService.store("규정.txt", content)).thenReturn("stored-uuid.txt");
        when(lifecycleService.createDocumentWithInitialVersion(
                10L, "규정", null, "v1", "규정.txt", "stored-uuid.txt", content.length, 30L))
                .thenReturn(documentVersion);
        when(chunkingService.processChunking(documentVersion, text, 500, 50, 30L))
                .thenReturn(processingJob);
        when(embeddingService.processEmbeddings(processingJob, documentVersion))
                .thenReturn(failedJob);

        DocumentIngestionResult result = ingest("규정.txt", content);

        assertThat(result.documentProcessingJobId()).isEqualTo(303L);
        assertThat(result.processingStatus()).isEqualTo("FAILED");
    }

    @Test
    void orchestrationIsNotTransactional() throws NoSuchMethodException {
        assertThat(DocumentIngestionOrchestrationService.class
                .getAnnotation(Transactional.class)).isNull();
        assertThat(DocumentIngestionOrchestrationService.class
                .getDeclaredMethod(
                        "ingest",
                        Long.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        byte[].class,
                        int.class,
                        int.class,
                        Long.class)
                .getAnnotation(Transactional.class)).isNull();
    }

    private DocumentIngestionResult ingest(String originalFileName, byte[] content) {
        return service.ingest(
                10L,
                "규정",
                null,
                "v1",
                originalFileName,
                content,
                500,
                50,
                30L);
    }
}
