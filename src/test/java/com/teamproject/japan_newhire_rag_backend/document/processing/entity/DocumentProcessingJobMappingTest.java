package com.teamproject.japan_newhire_rag_backend.document.processing.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

class DocumentProcessingJobMappingTest {

    @Test
    void mapsTableIdentityAndDocumentVersionRelation() throws NoSuchFieldException {
        Table table = DocumentProcessingJob.class.getAnnotation(Table.class);
        Field idField = DocumentProcessingJob.class
                .getDeclaredField("documentProcessingJobId");
        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
        Field versionField = DocumentProcessingJob.class.getDeclaredField("documentVersion");
        ManyToOne manyToOne = versionField.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = versionField.getAnnotation(JoinColumn.class);

        assertThat(table.name()).isEqualTo("document_processing_job");
        assertThat(idField.getAnnotation(Id.class)).isNotNull();
        assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
        assertThat(idField.getAnnotation(Column.class).name())
                .isEqualTo("document_processing_job_id");
        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(joinColumn.name()).isEqualTo("document_version_id");
        assertThat(joinColumn.nullable()).isFalse();
    }

    @Test
    void mapsStringsCountsExternalIdAndTimestamps() throws NoSuchFieldException {
        assertStringColumn("jobType", "job_type", false, 30);
        assertStringColumn("processingStatus", "processing_status", false, 20);
        assertColumn("totalChunkCount", "total_chunk_count", false, int.class);
        assertColumn("completedChunkCount", "completed_chunk_count", false, int.class);
        assertColumn("failedChunkCount", "failed_chunk_count", false, int.class);
        assertColumn("retryCount", "retry_count", false, int.class);
        assertStringColumn("failureReason", "failure_reason", true, 1000);
        assertColumn("startedAt", "started_at", true, LocalDateTime.class);
        assertColumn("completedAt", "completed_at", true, LocalDateTime.class);
        assertColumn("createdBy", "created_by", false, Long.class);

        assertThat(DocumentProcessingJob.class.getSuperclass()).isEqualTo(BaseEntity.class);
        Field createdAt = BaseEntity.class.getDeclaredField("createdAt");
        Field updatedAt = BaseEntity.class.getDeclaredField("updatedAt");
        assertThat(createdAt.getAnnotation(Column.class).name()).isEqualTo("created_at");
        assertThat(createdAt.getAnnotation(Column.class).nullable()).isFalse();
        assertThat(createdAt.getAnnotation(Column.class).updatable()).isFalse();
        assertThat(updatedAt.getAnnotation(Column.class).name()).isEqualTo("updated_at");
        assertThat(updatedAt.getAnnotation(Column.class).nullable()).isFalse();
    }

    @Test
    void factoryUsesDatabaseDefaultValues() {
        DocumentVersion version = DocumentVersion.create(
                null,
                "v1",
                LocalDate.of(2026, 1, 1),
                null,
                "rules.txt",
                "/documents/rules.txt",
                100L);

        DocumentProcessingJob job = DocumentProcessingJob.create(version, 10L);

        assertThat(job.getDocumentVersion()).isSameAs(version);
        assertThat(job.getJobType()).isEqualTo("FULL_PROCESSING");
        assertThat(job.getProcessingStatus()).isEqualTo("PENDING");
        assertThat(job.getTotalChunkCount()).isZero();
        assertThat(job.getCompletedChunkCount()).isZero();
        assertThat(job.getFailedChunkCount()).isZero();
        assertThat(job.getRetryCount()).isZero();
        assertThat(job.getFailureReason()).isNull();
        assertThat(job.getStartedAt()).isNull();
        assertThat(job.getCompletedAt()).isNull();
        assertThat(job.getCreatedBy()).isEqualTo(10L);
    }

    @Test
    void ddlContainsConfirmedConstraintsAndIndexes() throws IOException {
        String ddl = new ClassPathResource("db/ddl/b-domain-schema.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(ddl).contains(
                "CREATE TABLE document_processing_job",
                "DEFAULT 'FULL_PROCESSING'",
                "DEFAULT 'PENDING'",
                "CONSTRAINT ck_document_job_counts",
                "CONSTRAINT ck_document_job_retry",
                "ON DELETE CASCADE",
                "ON DELETE RESTRICT",
                "INDEX idx_document_job_version_time (document_version_id, created_at)",
                "INDEX idx_document_job_status (processing_status, created_at)");
    }

    @Test
    void repositoryIsJpaRepository() {
        assertThat(JpaRepository.class).isAssignableFrom(DocumentProcessingJobRepository.class);
    }

    private void assertColumn(
            String fieldName,
            String columnName,
            boolean nullable,
            Class<?> fieldType) throws NoSuchFieldException {
        Field field = DocumentProcessingJob.class.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);

        assertThat(field.getType()).isEqualTo(fieldType);
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
    }

    private void assertStringColumn(
            String fieldName,
            String columnName,
            boolean nullable,
            int length) throws NoSuchFieldException {
        assertColumn(fieldName, columnName, nullable, String.class);
        Column column = DocumentProcessingJob.class
                .getDeclaredField(fieldName)
                .getAnnotation(Column.class);

        assertThat(column.length()).isEqualTo(length);
    }
}
