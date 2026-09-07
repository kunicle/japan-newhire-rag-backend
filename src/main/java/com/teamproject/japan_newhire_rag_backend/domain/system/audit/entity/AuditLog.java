package com.teamproject.japan_newhire_rag_backend.domain.system.audit.entity;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditTargetType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "audit_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long auditLogId;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private AuditActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private AuditTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "previous_value", columnDefinition = "TEXT")
    private String previousValue;

    @Column(name = "changed_value", columnDefinition = "TEXT")
    private String changedValue;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static AuditLog record(
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
        AuditLog auditLog = new AuditLog();
        auditLog.actorUserId = actorUserId;
        auditLog.actionType = actionType;
        auditLog.targetType = targetType;
        auditLog.targetId = targetId;
        auditLog.previousValue = previousValue;
        auditLog.changedValue = changedValue;
        auditLog.requestIp = requestIp;
        auditLog.requestId = requestId;
        auditLog.createdAt = createdAt;
        return auditLog;
    }
}
