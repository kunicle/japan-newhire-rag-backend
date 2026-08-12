package com.teamproject.japan_newhire_rag_backend.domain.system.audit.repository;

import org.springframework.data.repository.Repository;

import com.teamproject.japan_newhire_rag_backend.domain.system.audit.entity.AuditLog;

public interface AuditLogRepository extends Repository<AuditLog, Long> {

    <S extends AuditLog> S save(S auditLog);
}
