package com.teamproject.japan_newhire_rag_backend.domain.system.audit.api;

public interface AuditLogRecordService {

    void record(AuditLogRecordCommand command);
}
