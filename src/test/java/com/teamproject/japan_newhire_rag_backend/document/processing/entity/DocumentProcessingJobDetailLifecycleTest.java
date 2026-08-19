package com.teamproject.japan_newhire_rag_backend.document.processing.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;

class DocumentProcessingJobDetailLifecycleTest {

    private static final LocalDateTime PROCESSED_AT = LocalDateTime.of(2026, 8, 12, 9, 10);
    private static final LocalDateTime PREVIOUS_PROCESSED_AT = LocalDateTime.of(2026, 8, 12, 9, 5);

    @Test
    void createKeepsPendingDefaults() {
        DocumentProcessingJobDetail detail = DocumentProcessingJobDetail.create(
                job(),
                chunk(),
                "EMBEDDING");

        assertThat(detail.getProcessingStatus()).isEqualTo("PENDING");
        assertThat(detail.getAttemptNumber()).isEqualTo(1);
        assertThat(detail.getFailureReason()).isNull();
        assertThat(detail.getProcessedAt()).isNull();
    }

    @Test
    void markProcessingChangesOnlyStatus() {
        DocumentProcessingJob job = job();
        DocumentChunk chunk = chunk();
        DocumentProcessingJobDetail detail = DocumentProcessingJobDetail.create(
                job,
                chunk,
                "EMBEDDING");
        detail.markFailed("previous failure", PREVIOUS_PROCESSED_AT);

        detail.markProcessing();

        assertThat(detail.getProcessingStatus()).isEqualTo("PROCESSING");
        assertThat(detail.getProcessedAt()).isEqualTo(PREVIOUS_PROCESSED_AT);
        assertThat(detail.getFailureReason()).isEqualTo("previous failure");
        assertUnrelatedFieldsRemain(detail, job, chunk, "EMBEDDING");
    }

    @Test
    void markCompletedStoresProvidedTimeWithoutResettingOtherFields() {
        DocumentProcessingJob job = job();
        DocumentChunk chunk = chunk();
        DocumentProcessingJobDetail detail = DocumentProcessingJobDetail.create(
                job,
                chunk,
                "CHUNKING");
        detail.markFailed("previous failure", PREVIOUS_PROCESSED_AT);

        detail.markCompleted(PROCESSED_AT);

        assertThat(detail.getProcessingStatus()).isEqualTo("COMPLETED");
        assertThat(detail.getProcessedAt()).isEqualTo(PROCESSED_AT);
        assertThat(detail.getFailureReason()).isEqualTo("previous failure");
        assertUnrelatedFieldsRemain(detail, job, chunk, "CHUNKING");
    }

    @Test
    void markFailedStoresReasonAndProvidedTimeWithoutResettingOtherFields() {
        DocumentProcessingJob job = job();
        DocumentChunk chunk = chunk();
        DocumentProcessingJobDetail detail = DocumentProcessingJobDetail.create(
                job,
                chunk,
                "EMBEDDING");

        detail.markFailed("embedding API failure", PROCESSED_AT);

        assertThat(detail.getProcessingStatus()).isEqualTo("FAILED");
        assertThat(detail.getFailureReason()).isEqualTo("embedding API failure");
        assertThat(detail.getProcessedAt()).isEqualTo(PROCESSED_AT);
        assertUnrelatedFieldsRemain(detail, job, chunk, "EMBEDDING");
    }

    @Test
    void lifecycleAllowsParsingDetailWithoutDocumentChunk() {
        DocumentProcessingJob job = job();
        DocumentProcessingJobDetail detail = DocumentProcessingJobDetail.create(
                job,
                null,
                "PARSING");

        detail.markProcessing();
        detail.markCompleted(PROCESSED_AT);

        assertThat(detail.getProcessingStatus()).isEqualTo("COMPLETED");
        assertThat(detail.getProcessedAt()).isEqualTo(PROCESSED_AT);
        assertThat(detail.getDocumentChunk()).isNull();
        assertUnrelatedFieldsRemain(detail, job, null, "PARSING");
    }

    private DocumentProcessingJob job() {
        return DocumentProcessingJob.create(null, 10L);
    }

    private DocumentChunk chunk() {
        return DocumentChunk.create(null, 1, null, null, "chunk content", null);
    }

    private void assertUnrelatedFieldsRemain(
            DocumentProcessingJobDetail detail,
            DocumentProcessingJob job,
            DocumentChunk chunk,
            String processingStep) {
        assertThat(detail.getDocumentProcessingJob()).isSameAs(job);
        assertThat(detail.getDocumentChunk()).isSameAs(chunk);
        assertThat(detail.getProcessingStep()).isEqualTo(processingStep);
        assertThat(detail.getAttemptNumber()).isEqualTo(1);
    }
}
