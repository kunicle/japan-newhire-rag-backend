package com.teamproject.japan_newhire_rag_backend.document.controller.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record DocumentManagementListPageResponse(
        List<DocumentManagementListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static DocumentManagementListPageResponse from(Page<DocumentManagementListItemResponse> result) {
        return new DocumentManagementListPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
