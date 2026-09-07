package com.teamproject.japan_newhire_rag_backend.document.controller.dto;

import java.time.LocalDateTime;

public record DocumentManagementListItemResponse(
        Long documentId,
        String documentName,
        Long documentCategoryId,
        String categoryCode,
        String categoryName,
        String documentStatus,
        Long latestVersionId,
        String latestVersionName,
        String latestVersionPublicationStatus,
        boolean latestVersionIsActive,
        LocalDateTime createdAt) {
}
