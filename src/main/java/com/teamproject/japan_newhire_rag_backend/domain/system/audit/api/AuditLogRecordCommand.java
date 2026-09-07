package com.teamproject.japan_newhire_rag_backend.domain.system.audit.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;

public record AuditLogRecordCommand(
        Long actorUserId,
        AuditActionType actionType,
        Long targetId,
        Map<String, ?> previousValue,
        Map<String, ?> changedValue,
        String requestIp,
        String requestId
) {

    public AuditLogRecordCommand {
        previousValue = immutableCopy(previousValue);
        changedValue = immutableCopy(changedValue);
    }

    private static Map<String, ?> immutableCopy(Map<String, ?> value) {
        return value == null
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }
}
