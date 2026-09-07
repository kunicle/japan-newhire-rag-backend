package com.teamproject.japan_newhire_rag_backend.document.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentManagementDetailResponse(
        Long documentId,
        String documentName,
        String documentDescription,
        Long documentCategoryId,
        String categoryCode,
        String categoryName,
        String documentStatus,
        LocalDateTime createdAt,
        List<DocumentManagementVersionResponse> versions) {
}
