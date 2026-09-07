package com.teamproject.japan_newhire_rag_backend.document.chunk.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.document.chunk.repository.DocumentChunkRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

class DocumentChunkMappingTest {

    @Test
    void mapsEntityTableIdentityAndDocumentVersionRelation() throws NoSuchFieldException {
        Entity entity = DocumentChunk.class.getAnnotation(Entity.class);
        Table table = DocumentChunk.class.getAnnotation(Table.class);
        Field idField = DocumentChunk.class.getDeclaredField("documentChunkId");
        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
        Field versionField = DocumentChunk.class.getDeclaredField("documentVersion");
        ManyToOne manyToOne = versionField.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = versionField.getAnnotation(JoinColumn.class);

        assertThat(entity).isNotNull();
        assertThat(table.name()).isEqualTo("document_chunk");
        assertThat(idField.getAnnotation(Id.class)).isNotNull();
        assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
        assertThat(idField.getAnnotation(Column.class).name()).isEqualTo("document_chunk_id");
        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(joinColumn.name()).isEqualTo("document_version_id");
        assertThat(joinColumn.nullable()).isFalse();
    }

    @Test
    void mapsChunkFieldsAndTimestamps() throws NoSuchFieldException {
        assertColumn("chunkSequence", "chunk_sequence", false, int.class);
        assertStringColumn("articleNumber", "article_number", true, 50);
        assertStringColumn("articleTitle", "article_title", true, 200);
        assertColumn("chunkContent", "chunk_content", false, String.class);
        assertThat(DocumentChunk.class.getDeclaredField("chunkContent")
                .getAnnotation(Column.class).columnDefinition()).isEqualTo("TEXT");
        assertColumn("tokenCount", "token_count", true, Integer.class);
        assertStringColumn("chunkStatus", "chunk_status", false, 20);

        assertThat(DocumentChunk.class.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(BaseEntity.class.getDeclaredField("createdAt")
                .getAnnotation(Column.class).name()).isEqualTo("created_at");
        assertThat(BaseEntity.class.getDeclaredField("updatedAt")
                .getAnnotation(Column.class).name()).isEqualTo("updated_at");
    }

    @Test
    void factoryUsesActiveDefaultAndKeepsNullableValues() {
        DocumentVersion version = DocumentVersion.create(
                null,
                "v1",
                LocalDate.of(2026, 1, 1),
                null,
                "rules.txt",
                "/documents/rules.txt",
                100L);

        DocumentChunk chunk = DocumentChunk.create(version, 1, null, null, "규정 내용", null);

        assertThat(chunk.getDocumentVersion()).isSameAs(version);
        assertThat(chunk.getChunkSequence()).isEqualTo(1);
        assertThat(chunk.getArticleNumber()).isNull();
        assertThat(chunk.getArticleTitle()).isNull();
        assertThat(chunk.getChunkContent()).isEqualTo("규정 내용");
        assertThat(chunk.getTokenCount()).isNull();
        assertThat(chunk.getChunkStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void coexistsWithPureChunkingDocumentChunk() {
        assertThat(DocumentChunk.class.getPackageName())
                .isEqualTo("com.teamproject.japan_newhire_rag_backend.document.chunk.entity");
        assertThat(com.teamproject.japan_newhire_rag_backend.document.chunking.DocumentChunk.class)
                .isRecord();
    }

    @Test
    void ddlContainsConfirmedConstraintsAndIndexes() throws IOException {
        String ddl = new ClassPathResource("db/ddl/b-domain-schema.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(ddl).contains(
                "CREATE TABLE document_chunk",
                "DEFAULT 'ACTIVE'",
                "CONSTRAINT uk_document_chunk_sequence",
                "UNIQUE (document_version_id, chunk_sequence)",
                "CONSTRAINT ck_document_chunk_sequence",
                "CHECK (chunk_sequence > 0)",
                "CONSTRAINT fk_document_chunk_version",
                "ON DELETE CASCADE",
                "INDEX idx_document_chunk_version_status (document_version_id, chunk_status)",
                "INDEX idx_document_chunk_article (document_version_id, article_number)");
    }

    @Test
    void repositoryIsJpaRepository() {
        assertThat(JpaRepository.class).isAssignableFrom(DocumentChunkRepository.class);
    }

    private void assertColumn(
            String fieldName,
            String columnName,
            boolean nullable,
            Class<?> fieldType) throws NoSuchFieldException {
        Field field = DocumentChunk.class.getDeclaredField(fieldName);
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
        assertThat(DocumentChunk.class.getDeclaredField(fieldName)
                .getAnnotation(Column.class).length()).isEqualTo(length);
    }
}
