package com.teamproject.japan_newhire_rag_backend.domain.system.audit.controller.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record AuditLogPageResponse(
        List<AuditLogResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AuditLogPageResponse from(Page<AuditLogResponse> result) {
        return new AuditLogPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
