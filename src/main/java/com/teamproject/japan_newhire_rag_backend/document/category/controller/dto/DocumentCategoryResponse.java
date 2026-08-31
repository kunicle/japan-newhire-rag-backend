package com.teamproject.japan_newhire_rag_backend.document.category.controller.dto;

import com.teamproject.japan_newhire_rag_backend.document.category.entity.DocumentCategory;

public record DocumentCategoryResponse(
        Long documentCategoryId,
        String categoryCode,
        String categoryName) {

    public static DocumentCategoryResponse from(DocumentCategory category) {
        return new DocumentCategoryResponse(
                category.getDocumentCategoryId(),
                category.getCategoryCode(),
                category.getCategoryName());
    }
}
