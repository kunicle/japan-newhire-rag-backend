package com.teamproject.japan_newhire_rag_backend.rag.controller.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record RagQuestionHistoryPageResponse(
        List<RagQuestionHistoryItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static RagQuestionHistoryPageResponse from(Page<RagQuestionHistoryItemResponse> result) {
        return new RagQuestionHistoryPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
