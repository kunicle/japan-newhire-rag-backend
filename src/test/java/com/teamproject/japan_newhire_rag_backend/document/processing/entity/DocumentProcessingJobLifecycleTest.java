package com.teamproject.japan_newhire_rag_backend.document.processing.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class DocumentProcessingJobLifecycleTest {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 8, 12, 9, 0);
    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 8, 12, 9, 10);

    @Test
    void createKeepsPendingDefaults() {
        DocumentProcessingJob job = DocumentProcessingJob.create(null, 10L);

        assertThat(job.getProcessingStatus()).isEqualTo("PENDING");
        assertThat(job.getTotalChunkCount()).isZero();
        assertThat(job.getCompletedChunkCount()).isZero();
        assertThat(job.getFailedChunkCount()).isZero();
        assertThat(job.getRetryCount()).isZero();
        assertThat(job.getFailureReason()).isNull();
        assertThat(job.getStartedAt()).isNull();
        assertThat(job.getCompletedAt()).isNull();
    }

    @Test
    void markProcessingChangesOnlyStatusAndStartedAt() {
        DocumentProcessingJob job = DocumentProcessingJob.create(null, 10L);
        job.recordTotalChunkCount(3);
        job.incrementCompletedChunkCount();
        job.incrementFailedChunkCount();
        job.incrementRetryCount();

        job.markProcessing(STARTED_AT);

        assertThat(job.getProcessingStatus()).isEqualTo("PROCESSING");
        assertThat(job.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(job.getTotalChunkCount()).isEqualTo(3);
        assertThat(job.getCompletedChunkCount()).isEqualTo(1);
        assertThat(job.getFailedChunkCount()).isEqualTo(1);
        assertThat(job.getRetryCount()).isEqualTo(1);
        assertThat(job.getFailureReason()).isNull();
        assertThat(job.getCompletedAt()).isNull();
    }

    @Test
    void recordsTotalAndIncrementsCounters() {
        DocumentProcessingJob job = DocumentProcessingJob.create(null, 10L);

        job.recordTotalChunkCount(4);
        job.incrementCompletedChunkCount();
        job.incrementCompletedChunkCount();
        job.incrementFailedChunkCount();
        job.incrementRetryCount();

        assertThat(job.getTotalChunkCount()).isEqualTo(4);
        assertThat(job.getCompletedChunkCount()).isEqualTo(2);
        assertThat(job.getFailedChunkCount()).isEqualTo(1);
        assertThat(job.getRetryCount()).isEqualTo(1);
        assertThat(job.getProcessingStatus()).isEqualTo("PENDING");
        assertThat(job.getStartedAt()).isNull();
        assertThat(job.getCompletedAt()).isNull();
    }

    @Test
    void markCompletedChangesOnlyStatusAndCompletedAt() {
        DocumentProcessingJob job = preparedJob();

        job.markCompleted(COMPLETED_AT);

        assertThat(job.getProcessingStatus()).isEqualTo("COMPLETED");
        assertThat(job.getCompletedAt()).isEqualTo(COMPLETED_AT);
        assertPreparedFieldsRemain(job);
        assertThat(job.getFailureReason()).isNull();
    }

    @Test
    void markFailedStoresReasonAndFinalFailureTimeWithoutResettingProgress() {
        DocumentProcessingJob job = preparedJob();

        job.markFailed("embedding API failure", COMPLETED_AT);

        assertThat(job.getProcessingStatus()).isEqualTo("FAILED");
        assertThat(job.getFailureReason()).isEqualTo("embedding API failure");
        assertThat(job.getCompletedAt()).isEqualTo(COMPLETED_AT);
        assertPreparedFieldsRemain(job);
    }

    private DocumentProcessingJob preparedJob() {
        DocumentProcessingJob job = DocumentProcessingJob.create(null, 10L);
        job.recordTotalChunkCount(3);
        job.incrementCompletedChunkCount();
        job.incrementFailedChunkCount();
        job.incrementRetryCount();
        job.markProcessing(STARTED_AT);
        return job;
    }

    private void assertPreparedFieldsRemain(DocumentProcessingJob job) {
        assertThat(job.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(job.getTotalChunkCount()).isEqualTo(3);
        assertThat(job.getCompletedChunkCount()).isEqualTo(1);
        assertThat(job.getFailedChunkCount()).isEqualTo(1);
        assertThat(job.getRetryCount()).isEqualTo(1);
    }
}
