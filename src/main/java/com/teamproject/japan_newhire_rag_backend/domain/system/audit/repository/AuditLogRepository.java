package com.teamproject.japan_newhire_rag_backend.domain.system.audit.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.teamproject.japan_newhire_rag_backend.domain.system.audit.entity.AuditLog;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditTargetType;

public interface AuditLogRepository extends Repository<AuditLog, Long> {

    <S extends AuditLog> S save(S auditLog);

    @Query("""
            SELECT auditLog FROM AuditLog auditLog
            WHERE (:actionType IS NULL OR auditLog.actionType = :actionType)
              AND (:actorUserId IS NULL OR auditLog.actorUserId = :actorUserId)
              AND (:targetType IS NULL OR auditLog.targetType = :targetType)
              AND (:targetId IS NULL OR auditLog.targetId = :targetId)
              AND (:from IS NULL OR auditLog.createdAt >= :from)
              AND (:to IS NULL OR auditLog.createdAt <= :to)
            """)
    Page<AuditLog> findAllByFilters(
            @Param("actionType") AuditActionType actionType,
            @Param("actorUserId") Long actorUserId,
            @Param("targetType") AuditTargetType targetType,
            @Param("targetId") Long targetId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
