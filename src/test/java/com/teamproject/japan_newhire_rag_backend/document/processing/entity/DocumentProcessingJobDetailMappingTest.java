package com.teamproject.japan_newhire_rag_backend.document.processing.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobDetailRepository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

class DocumentProcessingJobDetailMappingTest {

    @Test
    void mapsEntityTableAndIdentity() throws NoSuchFieldException {
        Field idField = DocumentProcessingJobDetail.class
                .getDeclaredField("processingJobDetailId");

        assertThat(DocumentProcessingJobDetail.class.getAnnotation(Entity.class)).isNotNull();
        assertThat(DocumentProcessingJobDetail.class.getAnnotation(Table.class).name())
                .isEqualTo("document_processing_job_detail");
        assertThat(idField.getType()).isEqualTo(Long.class);
        assertThat(idField.getAnnotation(Id.class)).isNotNull();
        assertThat(idField.getAnnotation(GeneratedValue.class).strategy())
                .isEqualTo(GenerationType.IDENTITY);
        assertThat(idField.getAnnotation(Column.class).name())
                .isEqualTo("processing_job_detail_id");
    }

    @Test
    void mapsRequiredJobAndNullableChunkRelations() throws NoSuchFieldException {
        assertRelation(
                "documentProcessingJob",
                DocumentProcessingJob.class,
                false,
                "document_processing_job_id",
                false);
        assertRelation(
                "documentChunk",
                DocumentChunk.class,
                true,
                "document_chunk_id",
                true);
    }

    @Test
    void mapsFieldsAndCreatedAtWithoutBaseEntity() throws NoSuchFieldException {
        assertStringColumn("processingStep", "processing_step", false, 30);
        assertStringColumn("processingStatus", "processing_status", false, 20);
        assertColumn("attemptNumber", "attempt_number", false, int.class);
        assertStringColumn("failureReason", "failure_reason", true, 1000);
        assertColumn("processedAt", "processed_at", true, LocalDateTime.class);
        assertColumn("createdAt", "created_at", false, LocalDateTime.class);

        Field createdAt = DocumentProcessingJobDetail.class.getDeclaredField("createdAt");
        assertThat(createdAt.getAnnotation(CreatedDate.class)).isNotNull();
        assertThat(createdAt.getAnnotation(Column.class).updatable()).isFalse();
        assertThat(DocumentProcessingJobDetail.class.getSuperclass()).isEqualTo(Object.class);
        assertThat(DocumentProcessingJobDetail.class.getSuperclass()).isNotEqualTo(BaseEntity.class);
        assertThat(DocumentProcessingJobDetail.class.getDeclaredFields())
                .extracting(Field::getName)
                .doesNotContain("updatedAt");

        EntityListeners listeners = DocumentProcessingJobDetail.class
                .getAnnotation(EntityListeners.class);
        assertThat(listeners.value()).containsExactly(AuditingEntityListener.class);
    }

    @Test
    void factoryUsesDefaultsAndAllowsMissingChunk() {
        DocumentProcessingJob job = DocumentProcessingJob.create(null, 10L);

        DocumentProcessingJobDetail detail = DocumentProcessingJobDetail.create(
                job,
                null,
                "PARSING");

        assertThat(detail.getDocumentProcessingJob()).isSameAs(job);
        assertThat(detail.getDocumentChunk()).isNull();
        assertThat(detail.getProcessingStep()).isEqualTo("PARSING");
        assertThat(detail.getProcessingStatus()).isEqualTo("PENDING");
        assertThat(detail.getAttemptNumber()).isEqualTo(1);
        assertThat(detail.getFailureReason()).isNull();
        assertThat(detail.getProcessedAt()).isNull();
    }

    @Test
    void repositoryIsJpaRepository() {
        assertThat(JpaRepository.class)
                .isAssignableFrom(DocumentProcessingJobDetailRepository.class);
    }

    @Test
    void ddlContainsConfirmedRelationsConstraintAndIndexes() throws IOException {
        String ddl = new ClassPathResource("db/ddl/b-domain-schema.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        String detailDdl = ddl.substring(ddl.indexOf("CREATE TABLE document_processing_job_detail"));

        assertThat(detailDdl).contains(
                "DEFAULT 'PENDING'",
                "attempt_number INT NOT NULL DEFAULT 1",
                "CONSTRAINT fk_processing_job_detail_job",
                "ON DELETE CASCADE",
                "CONSTRAINT fk_processing_job_detail_chunk",
                "ON DELETE SET NULL",
                "CONSTRAINT ck_processing_attempt",
                "CHECK (attempt_number > 0)",
                "INDEX idx_job_detail_job_step",
                "INDEX idx_job_detail_chunk (document_chunk_id)");
        assertThat(detailDdl).doesNotContain("updated_at");
    }

    private void assertRelation(
            String fieldName,
            Class<?> fieldType,
            boolean optional,
            String columnName,
            boolean nullable) throws NoSuchFieldException {
        Field field = DocumentProcessingJobDetail.class.getDeclaredField(fieldName);
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);

        assertThat(field.getType()).isEqualTo(fieldType);
        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isEqualTo(optional);
        assertThat(joinColumn.name()).isEqualTo(columnName);
        assertThat(joinColumn.nullable()).isEqualTo(nullable);
        assertThat(manyToOne.cascade()).isEmpty();
    }

    private void assertColumn(
            String fieldName,
            String columnName,
            boolean nullable,
            Class<?> fieldType) throws NoSuchFieldException {
        Field field = DocumentProcessingJobDetail.class.getDeclaredField(fieldName);
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
        assertThat(DocumentProcessingJobDetail.class.getDeclaredField(fieldName)
                .getAnnotation(Column.class).length()).isEqualTo(length);
    }
}
