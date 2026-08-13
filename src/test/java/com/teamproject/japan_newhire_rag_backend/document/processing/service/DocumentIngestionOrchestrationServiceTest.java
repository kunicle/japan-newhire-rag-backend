package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;
import com.teamproject.japan_newhire_rag_backend.document.service.DocumentLifecycleService;
import com.teamproject.japan_newhire_rag_backend.document.validation.InvalidTxtDocumentException;
import com.teamproject.japan_newhire_rag_backend.document.validation.TxtDocumentValidator;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

class DocumentIngestionOrchestrationServiceTest {

    private final TxtDocumentValidator validator = mock(TxtDocumentValidator.class);
    private final DocumentLifecycleService lifecycleService = mock(DocumentLifecycleService.class);
    private final DocumentChunkingProcessingService chunkingService =
            mock(DocumentChunkingProcessingService.class);
    private final DocumentEmbeddingOrchestrationService embeddingService =
            mock(DocumentEmbeddingOrchestrationService.class);
    private final DocumentIngestionOrchestrationService service =
            new DocumentIngestionOrchestrationService(
                    validator,
                    lifecycleService,
                    chunkingService,
                    embeddingService);

    @Test
    void ingestsDocumentThroughChunkingAndEmbedding() {
        String original = "신입사원은 최초 로그인 후 비밀번호를 변경합니다.";
        byte[] content = original.getBytes(StandardCharsets.UTF_8);
        DocumentVersion documentVersion = mock(DocumentVersion.class);
        DocumentProcessingJob processingJob = mock(DocumentProcessingJob.class);
        DocumentProcessingJob finalJob = mock(DocumentProcessingJob.class);
        when(lifecycleService.createDocumentWithInitialVersion(
                10L,
                "신입사원 규정",
                "최초 로그인 안내",
                "2026-01",
                "신입사원.txt",
                "/documents/신입사원.txt",
                content.length,
                30L)).thenReturn(documentVersion);
        when(chunkingService.processChunking(documentVersion, original, 500, 50, 30L))
                .thenReturn(processingJob);
        when(embeddingService.processEmbeddings(processingJob, documentVersion))
                .thenReturn(finalJob);

        DocumentProcessingJob result = service.ingest(
                10L,
                "신입사원 규정",
                "최초 로그인 안내",
                "2026-01",
                "신입사원.txt",
                "/documents/신입사원.txt",
                content,
                500,
                50,
                30L);

        assertThat(result).isSameAs(finalJob);
        InOrder order = inOrder(validator, lifecycleService, chunkingService, embeddingService);
        order.verify(validator).validate("신입사원.txt", content);
        order.verify(lifecycleService).createDocumentWithInitialVersion(
                10L,
                "신입사원 규정",
                "최초 로그인 안내",
                "2026-01",
                "신입사원.txt",
                "/documents/신입사원.txt",
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

        verifyNoInteractions(lifecycleService, chunkingService, embeddingService);
    }

    @Test
    void doesNotChunkOrEmbedWhenDocumentLifecycleFails() {
        byte[] content = "내용".getBytes(StandardCharsets.UTF_8);
        IllegalArgumentException failure = new IllegalArgumentException("category failure");
        when(lifecycleService.createDocumentWithInitialVersion(
                10L, "규정", null, "v1", "규정.txt", "/rules.txt", content.length, 30L))
                .thenThrow(failure);

        assertThatThrownBy(() -> ingest("규정.txt", content))
                .isSameAs(failure);

        verifyNoInteractions(chunkingService, embeddingService);
    }

    @Test
    void doesNotEmbedWhenChunkingFails() {
        byte[] content = "내용".getBytes(StandardCharsets.UTF_8);
        String text = new String(content, StandardCharsets.UTF_8);
        DocumentVersion documentVersion = mock(DocumentVersion.class);
        IllegalStateException failure = new IllegalStateException("chunking failure");
        when(lifecycleService.createDocumentWithInitialVersion(
                10L, "규정", null, "v1", "규정.txt", "/rules.txt", content.length, 30L))
                .thenReturn(documentVersion);
        when(chunkingService.processChunking(documentVersion, text, 500, 50, 30L))
                .thenThrow(failure);

        assertThatThrownBy(() -> ingest("규정.txt", content))
                .isSameAs(failure);

        verifyNoInteractions(embeddingService);
    }

    @Test
    void returnsEmbeddingOrchestrationResultUnchanged() {
        byte[] content = "내용".getBytes(StandardCharsets.UTF_8);
        String text = new String(content, StandardCharsets.UTF_8);
        DocumentVersion documentVersion = mock(DocumentVersion.class);
        DocumentProcessingJob processingJob = mock(DocumentProcessingJob.class);
        DocumentProcessingJob failedJob = mock(DocumentProcessingJob.class);
        when(lifecycleService.createDocumentWithInitialVersion(
                10L, "규정", null, "v1", "규정.txt", "/rules.txt", content.length, 30L))
                .thenReturn(documentVersion);
        when(chunkingService.processChunking(documentVersion, text, 500, 50, 30L))
                .thenReturn(processingJob);
        when(embeddingService.processEmbeddings(processingJob, documentVersion))
                .thenReturn(failedJob);

        assertThat(ingest("규정.txt", content)).isSameAs(failedJob);
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
                        String.class,
                        byte[].class,
                        int.class,
                        int.class,
                        Long.class)
                .getAnnotation(Transactional.class)).isNull();
    }

    private DocumentProcessingJob ingest(String originalFileName, byte[] content) {
        return service.ingest(
                10L,
                "규정",
                null,
                "v1",
                originalFileName,
                "/rules.txt",
                content,
                500,
                50,
                30L);
    }
}
