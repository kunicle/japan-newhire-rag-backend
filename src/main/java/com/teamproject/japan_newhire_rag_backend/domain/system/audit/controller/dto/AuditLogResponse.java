package com.teamproject.japan_newhire_rag_backend.domain.system.audit.controller.dto;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.domain.system.audit.entity.AuditLog;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditTargetType;

public record AuditLogResponse(
        Long auditLogId,
        Long actorUserId,
        AuditActionType actionType,
        AuditTargetType targetType,
        Long targetId,
        String previousValue,
        String changedValue,
        String requestIp,
        String requestId,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getAuditLogId(),
                auditLog.getActorUserId(),
                auditLog.getActionType(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getPreviousValue(),
                auditLog.getChangedValue(),
                auditLog.getRequestIp(),
                auditLog.getRequestId(),
                auditLog.getCreatedAt());
    }
}
