package com.teamproject.japan_newhire_rag_backend.document.version.controller.dto;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.domain.system.audit.controller.dto.AuditLogResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;

public record DocumentVersionAuditEventResponse(
        AuditActionType actionType,
        Long actorUserId,
        String previousValue,
        String changedValue,
        LocalDateTime createdAt) {

    public static DocumentVersionAuditEventResponse from(AuditLogResponse auditLog) {
        return new DocumentVersionAuditEventResponse(
                auditLog.actionType(),
                auditLog.actorUserId(),
                auditLog.previousValue(),
                auditLog.changedValue(),
                auditLog.createdAt());
    }
}
