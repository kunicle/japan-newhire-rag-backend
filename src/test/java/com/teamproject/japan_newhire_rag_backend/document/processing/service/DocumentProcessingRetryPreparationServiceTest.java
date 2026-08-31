package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.document.chunk.repository.DocumentChunkRepository;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;

import jakarta.persistence.LockModeType;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingRetryPreparationServiceTest {

    private static final Long JOB_ID = 10L;
    private static final Long VERSION_ID = 20L;

    @Mock DocumentProcessingJobRepository jobRepository;
    @Mock DocumentChunkRepository chunkRepository;
    @Mock CurrentUserProvider currentUserProvider;

    private DocumentProcessingRetryPreparationService service;

    @BeforeEach
    void setUp() {
        service = new DocumentProcessingRetryPreparationService(
                jobRepository,
                chunkRepository,
                currentUserProvider);
    }

    @Test
    void latestFailedJobWithActiveChunksCreatesNewProcessingJob() {
        DocumentVersion version = version();
        DocumentProcessingJob failedJob = latestJob(version, "FAILED");
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(failedJob));
        when(lockedJobs()).thenReturn(List.of(failedJob));
        when(chunkRepository
                .findByDocumentVersion_DocumentVersionIdAndChunkStatusOrderByChunkSequenceAsc(
                        VERSION_ID,
                        "ACTIVE"))
                .thenReturn(List.of(mock(DocumentChunk.class), mock(DocumentChunk.class)));
        when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUserContext(
                77L,
                999L,
                Set.of(RoleType.HR_MANAGER),
                null,
                null,
                null));
        when(jobRepository.save(any(DocumentProcessingJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentProcessingJob result = service.prepare(JOB_ID);

        assertEquals(version, result.getDocumentVersion());
        assertEquals(77L, result.getCreatedBy());
        assertEquals(2, result.getTotalChunkCount());
        assertEquals("PROCESSING", result.getProcessingStatus());
        assertTrue(result.getStartedAt() != null);
        verify(jobRepository).save(result);
    }

    @Test
    void jobNotFoundThrowsResourceNotFound() {
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.prepare(JOB_ID));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        verify(jobRepository, never()).save(any());
    }

    @Test
    void oldFailedJobWithNewerCompletedJobIsRejected() {
        DocumentVersion version = version();
        DocumentProcessingJob oldFailed = targetJob(version);
        DocumentProcessingJob newerCompleted = mock(DocumentProcessingJob.class);
        when(newerCompleted.getDocumentProcessingJobId()).thenReturn(11L);
        stubLockedJobs(oldFailed, List.of(newerCompleted, oldFailed));

        assertNotLatestConflict();
    }

    @Test
    void oldFailedJobWithNewerFailedJobIsRejected() {
        DocumentVersion version = version();
        DocumentProcessingJob oldFailed = targetJob(version);
        DocumentProcessingJob newerFailed = mock(DocumentProcessingJob.class);
        when(newerFailed.getDocumentProcessingJobId()).thenReturn(11L);
        stubLockedJobs(oldFailed, List.of(newerFailed, oldFailed));

        assertNotLatestConflict();
    }

    @Test
    void latestCompletedJobIsRejected() {
        assertLatestStatusConflict("COMPLETED");
    }

    @Test
    void latestProcessingJobIsRejected() {
        assertLatestStatusConflict("PROCESSING");
    }

    @Test
    void latestPendingJobIsRejected() {
        assertLatestStatusConflict("PENDING");
    }

    @Test
    void activeChunksAreRequired() {
        DocumentVersion version = version();
        DocumentProcessingJob failedJob = latestJob(version, "FAILED");
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(failedJob));
        when(lockedJobs()).thenReturn(List.of(failedJob));
        when(chunkRepository
                .findByDocumentVersion_DocumentVersionIdAndChunkStatusOrderByChunkSequenceAsc(
                        VERSION_ID,
                        "ACTIVE"))
                .thenReturn(List.of());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.prepare(JOB_ID));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        assertEquals("재처리할 청크가 없습니다.", exception.getMessage());
        verify(jobRepository, never()).save(any());
    }

    @Test
    void preparationMethodIsTransactional() throws NoSuchMethodException {
        Method method = DocumentProcessingRetryPreparationService.class
                .getMethod("prepare", Long.class);

        assertTrue(method.isAnnotationPresent(Transactional.class));
    }

    @Test
    void repositoryUsesPessimisticWriteLock() throws NoSuchMethodException {
        Method method = DocumentProcessingJobRepository.class.getMethod(
                "findForUpdateByDocumentVersion_DocumentVersionIdOrderByCreatedAtDescDocumentProcessingJobIdDesc",
                Long.class);
        Lock lock = method.getAnnotation(Lock.class);

        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
    }

    private void assertLatestStatusConflict(String status) {
        DocumentVersion version = version();
        DocumentProcessingJob latestJob = latestJob(version, status);
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(latestJob));
        when(lockedJobs()).thenReturn(List.of(latestJob));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.prepare(JOB_ID));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        assertEquals("실패한 작업만 재시도할 수 있습니다.", exception.getMessage());
        verify(jobRepository, never()).save(any());
    }

    private void assertNotLatestConflict() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.prepare(JOB_ID));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        assertEquals("최신 처리 작업만 재시도할 수 있습니다.", exception.getMessage());
        verify(jobRepository, never()).save(any());
    }

    private void stubLockedJobs(
            DocumentProcessingJob target,
            List<DocumentProcessingJob> jobs) {
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(target));
        when(lockedJobs()).thenReturn(jobs);
    }

    private List<DocumentProcessingJob> lockedJobs() {
        return jobRepository
                .findForUpdateByDocumentVersion_DocumentVersionIdOrderByCreatedAtDescDocumentProcessingJobIdDesc(
                        VERSION_ID);
    }

    private DocumentVersion version() {
        DocumentVersion version = mock(DocumentVersion.class);
        when(version.getDocumentVersionId()).thenReturn(VERSION_ID);
        return version;
    }

    private DocumentProcessingJob targetJob(DocumentVersion version) {
        DocumentProcessingJob job = mock(DocumentProcessingJob.class);
        when(job.getDocumentVersion()).thenReturn(version);
        return job;
    }

    private DocumentProcessingJob latestJob(DocumentVersion version, String status) {
        DocumentProcessingJob job = targetJob(version);
        when(job.getDocumentProcessingJobId()).thenReturn(JOB_ID);
        when(job.getProcessingStatus()).thenReturn(status);
        return job;
    }
}
