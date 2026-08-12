package com.teamproject.japan_newhire_rag_backend.rag.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.rag.model.repository.AiModelRepository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

class AiModelMappingTest {

    @Test
    void mapsEntityTableAndIdentity() throws NoSuchFieldException {
        Field idField = AiModel.class.getDeclaredField("aiModelId");

        assertThat(AiModel.class.getAnnotation(Entity.class)).isNotNull();
        assertThat(AiModel.class.getAnnotation(Table.class).name()).isEqualTo("ai_model");
        assertThat(idField.getType()).isEqualTo(Long.class);
        assertThat(idField.getAnnotation(Id.class)).isNotNull();
        assertThat(idField.getAnnotation(GeneratedValue.class).strategy())
                .isEqualTo(GenerationType.IDENTITY);
        assertThat(idField.getAnnotation(Column.class).name()).isEqualTo("ai_model_id");
    }

    @Test
    void mapsModelFieldsAndBaseEntityTimestamps() throws NoSuchFieldException {
        assertStringColumn("providerName", "provider_name", false, 100);
        assertStringColumn("modelName", "model_name", false, 150);
        assertStringColumn("modelType", "model_type", false, 20);
        assertColumn("embeddingDimension", "embedding_dimension", true, Integer.class);
        assertStringColumn("modelStatus", "model_status", false, 20);
        assertColumn("isDefault", "is_default", false, boolean.class);

        assertThat(AiModel.class.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(BaseEntity.class.getDeclaredField("createdAt")
                .getAnnotation(Column.class).name()).isEqualTo("created_at");
        assertThat(BaseEntity.class.getDeclaredField("updatedAt")
                .getAnnotation(Column.class).name()).isEqualTo("updated_at");
    }

    @Test
    void factoryUsesDefaultsAndAllowsNullEmbeddingDimension() {
        AiModel model = AiModel.create("example-provider", "example-llm", "LLM", null);

        assertThat(model.getProviderName()).isEqualTo("example-provider");
        assertThat(model.getModelName()).isEqualTo("example-llm");
        assertThat(model.getModelType()).isEqualTo("LLM");
        assertThat(model.getEmbeddingDimension()).isNull();
        assertThat(model.getModelStatus()).isEqualTo("ACTIVE");
        assertThat(model.isDefault()).isFalse();
    }

    @Test
    void repositoryIsJpaRepository() {
        assertThat(JpaRepository.class).isAssignableFrom(AiModelRepository.class);
    }

    @Test
    void ddlContainsOnlyConfirmedConstraintsAndIndex() throws IOException {
        String ddl = new ClassPathResource("db/ddl/b-domain-schema.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        int start = ddl.indexOf("CREATE TABLE ai_model");
        int end = ddl.indexOf("CREATE TABLE document_chunk", start);
        String aiModelDdl = ddl.substring(start, end);

        assertThat(aiModelDdl).contains(
                "CONSTRAINT uk_ai_model_name",
                "UNIQUE (provider_name, model_name, model_type)",
                "CONSTRAINT ck_ai_model_dimension",
                "CHECK (embedding_dimension IS NULL OR embedding_dimension > 0)",
                "INDEX idx_ai_model_type_status (model_type, model_status, is_default)",
                "model_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'",
                "is_default BOOLEAN NOT NULL DEFAULT FALSE");
        assertThat(aiModelDdl).doesNotContain(
                "ck_ai_model_type",
                "ck_ai_model_status",
                "FOREIGN KEY",
                "ON UPDATE CURRENT_TIMESTAMP");
    }

    private void assertColumn(
            String fieldName,
            String columnName,
            boolean nullable,
            Class<?> fieldType) throws NoSuchFieldException {
        Field field = AiModel.class.getDeclaredField(fieldName);
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
        assertThat(AiModel.class.getDeclaredField(fieldName)
                .getAnnotation(Column.class).length()).isEqualTo(length);
    }
}
