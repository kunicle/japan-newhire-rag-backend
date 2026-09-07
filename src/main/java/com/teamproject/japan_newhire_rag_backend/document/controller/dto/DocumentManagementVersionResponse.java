package com.teamproject.japan_newhire_rag_backend.document.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.document.access.controller.dto.DocumentAccessRuleReadResponse;

public record DocumentManagementVersionResponse(
        Long documentVersionId,
        String versionName,
        String publicationStatus,
        boolean isActive,
        String originalFileName,
        LocalDate effectiveDate,
        LocalDate expirationDate,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        DocumentAccessRuleReadResponse accessRule) {
}
