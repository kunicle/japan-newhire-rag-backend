package com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.teamproject.japan_newhire_rag_backend.domain.system.audit.entity.AuditLog;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditTargetType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.repository.AuditLogRepository;

import tools.jackson.databind.json.JsonMapper;

class AuditLogRecordServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-12T03:00:00Z");

    private AuditLogRepository auditLogRepository;
    private AuditLogRecordService service;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        service = new AuditLogRecordService(
                auditLogRepository,
                JsonMapper.builder().build(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordsActorActionTargetTimeAndJsonSnapshots() {
        service.record(new AuditLogRecordCommand(
                1L,
                AuditActionType.ACCOUNT_DEACTIVATED,
                20L,
                Map.of("accountStatus", "ACTIVE"),
                Map.of("accountStatus", "INACTIVE"),
                " 127.0.0.1 ",
                " request-1 "));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(1L, saved.getActorUserId());
        assertEquals(AuditActionType.ACCOUNT_DEACTIVATED, saved.getActionType());
        assertEquals(AuditTargetType.APP_USER, saved.getTargetType());
        assertEquals(20L, saved.getTargetId());
        assertEquals("{\"accountStatus\":\"ACTIVE\"}", saved.getPreviousValue());
        assertEquals("{\"accountStatus\":\"INACTIVE\"}", saved.getChangedValue());
        assertEquals("127.0.0.1", saved.getRequestIp());
        assertEquals("request-1", saved.getRequestId());
        assertEquals(LocalDateTime.of(2026, 8, 12, 3, 0), saved.getCreatedAt());
    }

    @Test
    void supportsNullableSnapshotsAndRequestMetadata() {
        service.record(new AuditLogRecordCommand(
                1L,
                AuditActionType.USER_CREATED,
                20L,
                null,
                Map.of("accountStatus", "ACTIVE"),
                null,
                " "));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertNull(captor.getValue().getPreviousValue());
        assertNull(captor.getValue().getRequestIp());
        assertNull(captor.getValue().getRequestId());
    }

    @Test
    void recordsNullManagerAsExplicitJsonNull() {
        Map<String, Object> previous = new LinkedHashMap<>();
        previous.put("managerEmployeeId", null);

        service.record(new AuditLogRecordCommand(
                1L,
                AuditActionType.DIRECT_MANAGER_CHANGED,
                20L,
                previous,
                Map.of("managerEmployeeId", 30L),
                null,
                null));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("{\"managerEmployeeId\":null}", captor.getValue().getPreviousValue());
        assertEquals("{\"managerEmployeeId\":30}", captor.getValue().getChangedValue());
    }

    @Test
    void recordsEvaluationResultPublishedWithAllowedMetadataAndFeedbackIds() {
        Map<String, Object> changed = new LinkedHashMap<>();
        changed.put("cycleId", 10L);
        changed.put("targetEmployeeId", 20L);
        changed.put("selfEvaluationId", 101L);
        changed.put("managerEvaluationId", 102L);
        changed.put("visibleManagerFeedbackIds", List.of(201L, 202L));

        service.record(new AuditLogRecordCommand(
                1L,
                AuditActionType.EVALUATION_RESULT_PUBLISHED,
                101L,
                null,
                changed,
                null,
                null));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(AuditActionType.EVALUATION_RESULT_PUBLISHED, saved.getActionType());
        assertEquals(AuditTargetType.EVALUATION, saved.getTargetType());
        assertEquals(101L, saved.getTargetId());
        assertNull(saved.getPreviousValue());
        assertEquals(
                "{\"cycleId\":10,\"targetEmployeeId\":20,\"selfEvaluationId\":101,"
                        + "\"managerEvaluationId\":102,\"visibleManagerFeedbackIds\":[201,202]}",
                saved.getChangedValue());
    }

    @Test
    void rejectsUnexpectedEvaluationResultMetadata() {
        AuditLogRecordCommand command = new AuditLogRecordCommand(
                1L,
                AuditActionType.EVALUATION_RESULT_PUBLISHED,
                101L,
                null,
                Map.of("managerFeedbackText", "sensitive feedback"),
                null,
                null);

        assertThrows(IllegalArgumentException.class, () -> service.record(command));
        verify(auditLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsNonIdValuesInVisibleManagerFeedbackIds() {
        AuditLogRecordCommand command = new AuditLogRecordCommand(
                1L,
                AuditActionType.EVALUATION_RESULT_PUBLISHED,
                101L,
                null,
                Map.of("visibleManagerFeedbackIds", List.of("feedback text")),
                null,
                null);

        assertThrows(IllegalArgumentException.class, () -> service.record(command));
        verify(auditLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsSensitiveOrUnexpectedFieldsBeforeSaving() {
        AuditLogRecordCommand command = new AuditLogRecordCommand(
                1L,
                AuditActionType.USER_CREATED,
                20L,
                null,
                Map.of("passwordHash", "secret"),
                null,
                null);

        assertThrows(IllegalArgumentException.class, () -> service.record(command));
        verify(auditLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsWholeObjectsBeforeSaving() {
        AuditLogRecordCommand command = new AuditLogRecordCommand(
                1L,
                AuditActionType.ROLE_GRANTED,
                20L,
                null,
                Map.of("roleId", Map.of("nested", 10L)),
                null,
                null);

        assertThrows(IllegalArgumentException.class, () -> service.record(command));
        verify(auditLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void propagatesRepositoryFailure() {
        RuntimeException failure = new RuntimeException("database unavailable");
        when(auditLogRepository.save(org.mockito.ArgumentMatchers.any())).thenThrow(failure);
        AuditLogRecordCommand command = new AuditLogRecordCommand(
                1L,
                AuditActionType.ROLE_REVOKED,
                20L,
                Map.of("roleType", "EMPLOYEE"),
                null,
                null,
                null);

        assertEquals(failure, assertThrows(RuntimeException.class, () -> service.record(command)));
    }
}
