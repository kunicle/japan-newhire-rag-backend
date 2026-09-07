package com.teamproject.japan_newhire_rag_backend.domain.system.notification.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;

class NotificationSchemaConsistencyTest {

    @Test
    void entityUsesOfficialPhysicalColumnNames() throws ReflectiveOperationException {
        Field recipient = Notification.class.getDeclaredField("recipient");
        assertEquals("app_user_id", recipient.getAnnotation(JoinColumn.class).name());
        assertColumn("title", "notification_title");
        assertColumn("message", "notification_content");
        assertColumn("targetType", "reference_type");
        assertColumn("targetId", "reference_id");
    }

    @Test
    void ddlUsesOfficialPhysicalColumnNames() throws IOException {
        String ddl = Files.readString(Path.of(
                "src/main/resources/db/ddl/system-operations-schema.sql"));
        String notificationDdl = ddl.substring(ddl.indexOf("CREATE TABLE notification"));

        assertTrue(notificationDdl.contains("app_user_id BIGINT NOT NULL"));
        assertTrue(notificationDdl.contains("notification_title VARCHAR(200) NOT NULL"));
        assertTrue(notificationDdl.contains("notification_content TEXT NOT NULL"));
        assertTrue(notificationDdl.contains("reference_type VARCHAR(50) NULL"));
        assertTrue(notificationDdl.contains("reference_id BIGINT NULL"));
        assertTrue(notificationDdl.contains("FOREIGN KEY (app_user_id)"));
        assertFalse(notificationDdl.contains("recipient_app_user_id"));
    }

    private void assertColumn(String fieldName, String columnName)
            throws ReflectiveOperationException {
        Field field = Notification.class.getDeclaredField(fieldName);
        assertEquals(columnName, field.getAnnotation(Column.class).name());
    }
}
