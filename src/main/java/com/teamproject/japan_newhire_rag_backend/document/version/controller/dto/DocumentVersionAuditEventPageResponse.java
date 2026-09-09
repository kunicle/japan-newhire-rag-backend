package com.teamproject.japan_newhire_rag_backend.document.version.controller.dto;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.domain.system.audit.controller.dto.AuditLogPageResponse;

public record DocumentVersionAuditEventPageResponse(
        List<DocumentVersionAuditEventResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static DocumentVersionAuditEventPageResponse from(AuditLogPageResponse auditLogs) {
        return new DocumentVersionAuditEventPageResponse(
                auditLogs.content().stream()
                        .map(DocumentVersionAuditEventResponse::from)
                        .toList(),
                auditLogs.page(),
                auditLogs.size(),
                auditLogs.totalElements(),
                auditLogs.totalPages());
    }
}
