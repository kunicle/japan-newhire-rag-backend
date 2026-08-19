package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingJobQueryServiceTest {

    @Mock DocumentProcessingJobRepository documentProcessingJobRepository;

    private DocumentProcessingJobQueryService service;

    @BeforeEach
    void setUp() {
        service = new DocumentProcessingJobQueryService(documentProcessingJobRepository);
    }

    @Test
    void returnsJobsInRepositoryOrder() {
        LocalDateTime newerAt = LocalDateTime.of(2026, 8, 19, 10, 0);
        LocalDateTime olderAt = LocalDateTime.of(2026, 8, 19, 9, 0);
        DocumentProcessingJob newer = job(
                301L, 201L, "FAILED", "Embedding processing failed", newerAt);
        DocumentProcessingJob older = job(302L, 205L, "COMPLETED", null, olderAt);
        when(documentProcessingJobRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(newer, older));

        List<DocumentProcessingJobStatus> result = service.getProcessingJobs();

        assertEquals(List.of(
                new DocumentProcessingJobStatus(
                        301L, 201L, "FAILED", "Embedding processing failed", newerAt),
                new DocumentProcessingJobStatus(
                        302L, 205L, "COMPLETED", null, olderAt)), result);
        verify(documentProcessingJobRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void completedJobHasNullFailureReason() {
        DocumentProcessingJob completed = job(
                302L, 205L, "COMPLETED", null, LocalDateTime.of(2026, 8, 19, 9, 0));
        when(documentProcessingJobRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(completed));

        DocumentProcessingJobStatus result = service.getProcessingJobs().get(0);

        assertEquals("COMPLETED", result.status());
        assertNull(result.failureReason());
    }

    @Test
    void failedJobExposesStoredFailureReason() {
        DocumentProcessingJob failed = job(
                301L,
                201L,
                "FAILED",
                "Embedding processing failed",
                LocalDateTime.of(2026, 8, 19, 10, 0));
        when(documentProcessingJobRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(failed));

        DocumentProcessingJobStatus result = service.getProcessingJobs().get(0);

        assertEquals("FAILED", result.status());
        assertEquals("Embedding processing failed", result.failureReason());
    }

    @Test
    void returnsEmptyListWhenNoJobsExist() {
        when(documentProcessingJobRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of());

        List<DocumentProcessingJobStatus> result = service.getProcessingJobs();

        assertTrue(result.isEmpty());
        verify(documentProcessingJobRepository).findAllByOrderByCreatedAtDesc();
    }

    private DocumentProcessingJob job(
            Long jobId,
            Long versionId,
            String status,
            String failureReason,
            LocalDateTime createdAt) {
        DocumentVersion documentVersion = mock(DocumentVersion.class);
        when(documentVersion.getDocumentVersionId()).thenReturn(versionId);
        DocumentProcessingJob job = mock(DocumentProcessingJob.class);
        when(job.getDocumentProcessingJobId()).thenReturn(jobId);
        when(job.getDocumentVersion()).thenReturn(documentVersion);
        when(job.getProcessingStatus()).thenReturn(status);
        if (failureReason != null) {
            when(job.getFailureReason()).thenReturn(failureReason);
        }
        when(job.getCreatedAt()).thenReturn(createdAt);
        return job;
    }
}
