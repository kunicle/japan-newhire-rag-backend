package com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.internal;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordService;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.entity.AuditLog;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.repository.AuditLogRepository;

import tools.jackson.databind.ObjectMapper;

@Service
public class AuditLogRecordServiceImpl implements AuditLogRecordService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditLogRecordServiceImpl(
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void record(AuditLogRecordCommand command) {
        validate(command);
        AuditActionType actionType = command.actionType();
        AuditLog auditLog = AuditLog.record(
                command.actorUserId(),
                actionType,
                actionType.targetType(),
                command.targetId(),
                serialize(command.previousValue()),
                serialize(command.changedValue()),
                normalizeNullable(command.requestIp()),
                normalizeNullable(command.requestId()),
                LocalDateTime.now(clock));
        auditLogRepository.save(auditLog);
    }

    private void validate(AuditLogRecordCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Audit log command is required");
        }
        if (command.actorUserId() == null || command.actorUserId() <= 0) {
            throw new IllegalArgumentException("actorUserId must be positive");
        }
        if (command.actionType() == null) {
            throw new IllegalArgumentException("actionType is required");
        }
        if (command.targetId() == null || command.targetId() <= 0) {
            throw new IllegalArgumentException("targetId must be positive");
        }
        validateLength(command.requestIp(), 45, "requestIp");
        validateLength(command.requestId(), 100, "requestId");
        validateValue(command.previousValue(), command.actionType());
        validateValue(command.changedValue(), command.actionType());
    }

    private void validateValue(Map<String, ?> value, AuditActionType actionType) {
        if (value == null) {
            return;
        }
        Set<String> allowedKeys = actionType.allowedValueKeys();
        if (!allowedKeys.containsAll(value.keySet())) {
            throw new IllegalArgumentException(
                    "Audit value contains fields not allowed for " + actionType.name());
        }
        if (value.entrySet().stream().anyMatch(entry ->
                isUnsupportedValue(actionType, entry.getKey(), entry.getValue()))) {
            throw new IllegalArgumentException("Audit values must be scalar values");
        }
    }

    private boolean isUnsupportedValue(AuditActionType actionType, String key, Object value) {
        if (actionType == AuditActionType.EVALUATION_RESULT_PUBLISHED
                && "visibleManagerFeedbackIds".equals(key)) {
            return !(value instanceof List<?> ids)
                    || ids.stream().anyMatch(this::isInvalidId);
        }
        return value != null
                && !(value instanceof String)
                && !(value instanceof Number)
                && !(value instanceof Boolean)
                && !(value instanceof Enum<?>);
    }

    private boolean isInvalidId(Object value) {
        return !(value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long)
                || ((Number) value).longValue() <= 0;
    }

    private String serialize(Map<String, ?> value) {
        if (value == null) {
            return null;
        }
        return objectMapper.writeValueAsString(value);
    }

    private void validateLength(String value, int maximumLength, String fieldName) {
        if (value != null && value.trim().length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " exceeds " + maximumLength);
        }
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
