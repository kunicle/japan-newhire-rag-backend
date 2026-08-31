package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingRetryServiceTest {

    @Mock DocumentProcessingRetryPreparationService preparationService;
    @Mock DocumentEmbeddingOrchestrationService embeddingOrchestrationService;

    private DocumentProcessingRetryService service;

    @BeforeEach
    void setUp() {
        service = new DocumentProcessingRetryService(
                preparationService,
                embeddingOrchestrationService);
    }

    @Test
    void preparesThenProcessesEmbeddingsExactlyOnce() {
        DocumentVersion version = mock(DocumentVersion.class);
        DocumentProcessingJob prepared = mock(DocumentProcessingJob.class);
        DocumentProcessingJob completed = mock(DocumentProcessingJob.class);
        when(prepared.getDocumentVersion()).thenReturn(version);
        when(preparationService.prepare(10L)).thenReturn(prepared);
        when(embeddingOrchestrationService.processEmbeddings(prepared, version))
                .thenReturn(completed);

        DocumentProcessingJob result = service.retry(10L);

        assertSame(completed, result);
        verify(preparationService).prepare(10L);
        verify(embeddingOrchestrationService).processEmbeddings(prepared, version);
    }

    @Test
    void returnsFailedEmbeddingResultWithoutThrowing() {
        DocumentVersion version = mock(DocumentVersion.class);
        DocumentProcessingJob prepared = mock(DocumentProcessingJob.class);
        DocumentProcessingJob failed = mock(DocumentProcessingJob.class);
        when(prepared.getDocumentVersion()).thenReturn(version);
        when(failed.getProcessingStatus()).thenReturn("FAILED");
        when(preparationService.prepare(10L)).thenReturn(prepared);
        when(embeddingOrchestrationService.processEmbeddings(prepared, version))
                .thenReturn(failed);

        DocumentProcessingJob result = service.retry(10L);

        assertSame(failed, result);
        assertEquals("FAILED", result.getProcessingStatus());
    }

    @Test
    void propagatesEmbeddingRuntimeException() {
        DocumentVersion version = mock(DocumentVersion.class);
        DocumentProcessingJob prepared = mock(DocumentProcessingJob.class);
        RuntimeException failure = new IllegalStateException("model unavailable");
        when(prepared.getDocumentVersion()).thenReturn(version);
        when(preparationService.prepare(10L)).thenReturn(prepared);
        when(embeddingOrchestrationService.processEmbeddings(prepared, version))
                .thenThrow(failure);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.retry(10L));

        assertSame(failure, thrown);
    }
}
