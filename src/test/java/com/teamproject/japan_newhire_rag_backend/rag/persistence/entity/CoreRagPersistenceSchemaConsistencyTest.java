package com.teamproject.japan_newhire_rag_backend.rag.persistence.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

class CoreRagPersistenceSchemaConsistencyTest {

    private static final Path DDL = Path.of(
            "src/main/resources/db/ddl/b-domain-schema.sql");

    @Test
    void entitiesUseOfficialTableAndPrimaryKeyColumnNames() throws Exception {
        assertTableAndPrimaryKey(RagQuestion.class, "rag_question", "ragQuestionId", "rag_question_id");
        assertTableAndPrimaryKey(RagSearch.class, "rag_search", "ragSearchId", "rag_search_id");
        assertTableAndPrimaryKey(
                RagSearchResult.class,
                "rag_search_result",
                "ragSearchResultId",
                "rag_search_result_id");
        assertTableAndPrimaryKey(RagAnswer.class, "rag_answer", "ragAnswerId", "rag_answer_id");
        assertTableAndPrimaryKey(RagCitation.class, "rag_citation", "ragCitationId", "rag_citation_id");
    }

    @Test
    void entityColumnsAndUniqueConstraintsMatchOfficialSchema() throws Exception {
        assertColumn(RagQuestion.class, "questionText", "question_text", false);
        assertColumn(RagQuestion.class, "processingStatus", "processing_status", false, 20);
        assertColumn(RagQuestion.class, "failureType", "failure_type", true, 50);
        assertColumn(RagQuestion.class, "failureReason", "failure_reason", true, 1000);
        assertColumn(RagAnswer.class, "answerText", "answer_text", false);
        assertColumn(RagSearchResult.class, "similarityScore", "similarity_score", false);
        assertColumn(RagSearchResult.class, "resultRank", "result_rank", false);
        assertColumn(
                RagCitation.class,
                "documentNameSnapshot",
                "document_name_snapshot",
                false,
                200);
        assertColumn(
                RagCitation.class,
                "versionNameSnapshot",
                "version_name_snapshot",
                false,
                20);
        assertColumn(
                RagCitation.class,
                "articleNumberSnapshot",
                "article_number_snapshot",
                true,
                50);
        assertColumn(RagCitation.class, "citedText", "cited_text", false);

        JoinColumn answerSearch = RagAnswer.class.getDeclaredField("ragSearch")
                .getAnnotation(JoinColumn.class);
        assertEquals("rag_search_id", answerSearch.name());
        assertFalse(answerSearch.nullable());
        assertTrue(answerSearch.unique());

        assertUniqueConstraint(
                RagSearchResult.class,
                "uk_rag_search_result_rank",
                List.of("rag_search_id", "result_rank"));
        assertUniqueConstraint(
                RagCitation.class,
                "uk_rag_citation_position",
                List.of("rag_answer_id", "position"));
    }

    @Test
    void ddlMatchesTextColumnsAndUniqueConstraints() throws Exception {
        String ddl = Files.readString(DDL).replaceAll("\\s+", " ");

        assertTrue(ddl.contains("question_text TEXT NOT NULL"));
        assertTrue(ddl.contains(
                "processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'"));
        assertTrue(ddl.contains("failure_type VARCHAR(50) NULL"));
        assertTrue(ddl.contains("failure_reason VARCHAR(1000) NULL"));
        assertTrue(ddl.contains("answer_text TEXT NOT NULL"));
        assertTrue(ddl.contains("document_name_snapshot VARCHAR(200) NOT NULL"));
        assertTrue(ddl.contains("version_name_snapshot VARCHAR(20) NOT NULL"));
        assertTrue(ddl.contains("article_number_snapshot VARCHAR(50) NULL"));
        assertTrue(ddl.contains("cited_text TEXT NOT NULL"));
        assertTrue(ddl.contains("similarity_score DOUBLE NOT NULL"));
        assertTrue(ddl.contains("result_rank INT NOT NULL"));
        assertTrue(ddl.contains("UNIQUE (rag_search_id, result_rank)"));
        assertTrue(ddl.contains("UNIQUE (rag_search_id)"));
        assertTrue(ddl.contains("UNIQUE (rag_answer_id, position)"));
    }

    @Test
    void repositoriesDeclareExpectedMethods() {
        assertEquals(2, com.teamproject.japan_newhire_rag_backend.rag.persistence.repository
                .RagQuestionRepository.class.getDeclaredMethods().length);
        assertEquals(1, com.teamproject.japan_newhire_rag_backend.rag.persistence.repository
                .RagSearchRepository.class.getDeclaredMethods().length);
        assertEquals(0, com.teamproject.japan_newhire_rag_backend.rag.persistence.repository
                .RagSearchResultRepository.class.getDeclaredMethods().length);
        assertEquals(1, com.teamproject.japan_newhire_rag_backend.rag.persistence.repository
                .RagAnswerRepository.class.getDeclaredMethods().length);
        assertEquals(1, com.teamproject.japan_newhire_rag_backend.rag.persistence.repository
                .RagCitationRepository.class.getDeclaredMethods().length);
    }

    private void assertTableAndPrimaryKey(
            Class<?> entityType,
            String tableName,
            String fieldName,
            String columnName) throws Exception {
        assertEquals(tableName, entityType.getAnnotation(Table.class).name());
        assertEquals(
                columnName,
                entityType.getDeclaredField(fieldName).getAnnotation(Column.class).name());
    }

    private void assertColumn(
            Class<?> entityType,
            String fieldName,
            String columnName,
            boolean nullable) throws Exception {
        Field field = entityType.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);
        assertEquals(columnName, column.name());
        assertEquals(nullable, column.nullable());
    }

    private void assertColumn(
            Class<?> entityType,
            String fieldName,
            String columnName,
            boolean nullable,
            int length) throws Exception {
        Field field = entityType.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);
        assertEquals(columnName, column.name());
        assertEquals(nullable, column.nullable());
        assertEquals(length, column.length());
    }

    private void assertUniqueConstraint(
            Class<?> entityType,
            String constraintName,
            List<String> columnNames) {
        UniqueConstraint constraint = entityType.getAnnotation(Table.class).uniqueConstraints()[0];
        assertEquals(constraintName, constraint.name());
        assertEquals(columnNames, List.of(constraint.columnNames()));
    }
}
