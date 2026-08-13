package com.teamproject.japan_newhire_rag_backend.domain.system.audit.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

class AuditLogSchemaConsistencyTest {

    private static final Path DDL = Path.of(
            "src/main/resources/db/ddl/system-operations-schema.sql");

    @Test
    void entityUsesOfficialTableAndColumnNames() throws Exception {
        assertEquals("audit_log", AuditLog.class.getAnnotation(Table.class).name());
        assertColumn("actorUserId", "actor_user_id", false, 255);
        assertColumn("actionType", "action_type", false, 50);
        assertColumn("targetType", "target_type", false, 50);
        assertColumn("targetId", "target_id", false, 255);
        assertColumn("previousValue", "previous_value", true, 255);
        assertColumn("changedValue", "changed_value", true, 255);
        assertColumn("requestIp", "request_ip", true, 45);
        assertColumn("requestId", "request_id", true, 100);
        assertColumn("createdAt", "created_at", false, 255);
    }

    @Test
    void ddlMatchesOfficialColumnsForeignKeyAndIndexes() throws Exception {
        String ddl = Files.readString(DDL).replaceAll("\\s+", " ");
        assertTrue(ddl.contains("audit_log_id BIGINT NOT NULL AUTO_INCREMENT"));
        assertTrue(ddl.contains("actor_user_id BIGINT NOT NULL"));
        assertTrue(ddl.contains("action_type VARCHAR(50) NOT NULL"));
        assertTrue(ddl.contains("target_type VARCHAR(50) NOT NULL"));
        assertTrue(ddl.contains("target_id BIGINT NOT NULL"));
        assertTrue(ddl.contains("previous_value TEXT NULL"));
        assertTrue(ddl.contains("changed_value TEXT NULL"));
        assertTrue(ddl.contains("request_ip VARCHAR(45) NULL"));
        assertTrue(ddl.contains("request_id VARCHAR(100) NULL"));
        assertTrue(ddl.contains("created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"));
        assertTrue(ddl.contains("FOREIGN KEY (actor_user_id) REFERENCES app_user (app_user_id) ON DELETE RESTRICT"));
        assertTrue(ddl.contains("INDEX idx_audit_log_action_time (action_type, created_at)"));
        assertTrue(ddl.contains("INDEX idx_audit_log_actor_time (actor_user_id, created_at)"));
        assertTrue(ddl.contains("INDEX idx_audit_log_target (target_type, target_id, created_at)"));
        assertTrue(ddl.contains("INDEX idx_audit_log_request (request_id)"));
        assertFalse(ddl.contains("FOREIGN KEY (target_id)"));
        assertFalse(ddl.contains("deleted_at"));
    }

    @Test
    void repositoryContractDoesNotExposeUpdateOrDeleteOperations() throws Exception {
        Class<?> repositoryType = Class.forName(
                "com.teamproject.japan_newhire_rag_backend.domain.system.audit.repository.AuditLogRepository");
        assertEquals(List.of("save"), Arrays.stream(repositoryType.getDeclaredMethods())
                .map(method -> method.getName())
                .sorted()
                .toList());
    }

    private void assertColumn(
            String fieldName,
            String columnName,
            boolean nullable,
            int length
    ) throws Exception {
        Field field = AuditLog.class.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);
        assertEquals(columnName, column.name());
        assertEquals(nullable, column.nullable());
        assertEquals(length, column.length());
    }
}
