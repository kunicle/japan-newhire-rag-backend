package com.teamproject.japan_newhire_rag_backend.document.chunk.embedding.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.document.chunk.embedding.repository.ChunkEmbeddingRepository;
import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.rag.model.entity.AiModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

class ChunkEmbeddingMappingTest {

    @Test
    void mapsEntityTableAndIdentity() throws NoSuchFieldException {
        Field idField = ChunkEmbedding.class.getDeclaredField("chunkEmbeddingId");

        assertThat(ChunkEmbedding.class.getAnnotation(Entity.class)).isNotNull();
        assertThat(ChunkEmbedding.class.getAnnotation(Table.class).name())
                .isEqualTo("chunk_embedding");
        assertThat(idField.getType()).isEqualTo(Long.class);
        assertThat(idField.getAnnotation(Id.class)).isNotNull();
        assertThat(idField.getAnnotation(GeneratedValue.class).strategy())
                .isEqualTo(GenerationType.IDENTITY);
        assertThat(idField.getAnnotation(Column.class).name()).isEqualTo("chunk_embedding_id");
    }

    @Test
    void mapsBothParentsAsRequiredLazyManyToOneWithoutCascade() throws NoSuchFieldException {
        assertRelation("documentChunk", DocumentChunk.class, "document_chunk_id");
        assertRelation("aiModel", AiModel.class, "ai_model_id");

        assertThat(ChunkEmbedding.class.getDeclaredFields())
                .allSatisfy(field -> assertThat(field.getAnnotation(OneToOne.class)).isNull());
    }

    @Test
    void mapsEmbeddingFieldsAndBaseEntityTimestamps() throws NoSuchFieldException {
        assertStringColumn("vectorReference", "vector_reference", false, 500);
        assertColumn("embeddingDimension", "embedding_dimension", false, int.class);
        assertStringColumn("embeddingStatus", "embedding_status", false, 20);
        assertColumn("embeddedAt", "embedded_at", true, LocalDateTime.class);

        assertThat(ChunkEmbedding.class.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(BaseEntity.class.getDeclaredField("createdAt")
                .getAnnotation(Column.class).name()).isEqualTo("created_at");
        assertThat(BaseEntity.class.getDeclaredField("updatedAt")
                .getAnnotation(Column.class).name()).isEqualTo("updated_at");
    }

    @Test
    void factoryUsesPendingDefault() {
        DocumentChunk chunk = DocumentChunk.create(null, 1, null, null, "규정 내용", null);
        AiModel model = AiModel.create("provider", "embedding-model", "EMBEDDING", 1536);

        ChunkEmbedding embedding = ChunkEmbedding.create(chunk, model, "chunk-1", 1536);

        assertThat(embedding.getDocumentChunk()).isSameAs(chunk);
        assertThat(embedding.getAiModel()).isSameAs(model);
        assertThat(embedding.getVectorReference()).isEqualTo("chunk-1");
        assertThat(embedding.getEmbeddingDimension()).isEqualTo(1536);
        assertThat(embedding.getEmbeddingStatus()).isEqualTo("PENDING");
        assertThat(embedding.getEmbeddedAt()).isNull();
    }

    @Test
    void repositoryIsJpaRepository() {
        assertThat(JpaRepository.class).isAssignableFrom(ChunkEmbeddingRepository.class);
    }

    @Test
    void ddlContainsConfirmedCardinalityAndConstraints() throws IOException {
        String ddl = new ClassPathResource("db/ddl/b-domain-schema.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        int start = ddl.indexOf("CREATE TABLE chunk_embedding");
        int end = ddl.indexOf("CREATE TABLE document_processing_job", start);
        String embeddingDdl = ddl.substring(start, end);

        assertThat(embeddingDdl).contains(
                "CONSTRAINT fk_chunk_embedding_chunk",
                "ON DELETE CASCADE",
                "CONSTRAINT fk_chunk_embedding_model",
                "ON DELETE RESTRICT",
                "CONSTRAINT uk_chunk_embedding_model",
                "UNIQUE (document_chunk_id, ai_model_id)",
                "CONSTRAINT ck_chunk_embedding_dimension",
                "CHECK (embedding_dimension > 0)",
                "INDEX idx_chunk_embedding_status (embedding_status, created_at)",
                "INDEX idx_chunk_embedding_reference (vector_reference)");
        assertThat(embeddingDdl).doesNotContain(
                "UNIQUE (document_chunk_id)",
                "ck_chunk_embedding_status",
                "ON UPDATE CURRENT_TIMESTAMP");
    }

    private void assertRelation(
            String fieldName,
            Class<?> fieldType,
            String columnName) throws NoSuchFieldException {
        Field field = ChunkEmbedding.class.getDeclaredField(fieldName);
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);

        assertThat(field.getType()).isEqualTo(fieldType);
        assertThat(manyToOne).isNotNull();
        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(manyToOne.cascade()).isEmpty();
        assertThat(joinColumn.name()).isEqualTo(columnName);
        assertThat(joinColumn.nullable()).isFalse();
    }

    private void assertColumn(
            String fieldName,
            String columnName,
            boolean nullable,
            Class<?> fieldType) throws NoSuchFieldException {
        Field field = ChunkEmbedding.class.getDeclaredField(fieldName);
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
        assertThat(ChunkEmbedding.class.getDeclaredField(fieldName)
                .getAnnotation(Column.class).length()).isEqualTo(length);
    }
}
