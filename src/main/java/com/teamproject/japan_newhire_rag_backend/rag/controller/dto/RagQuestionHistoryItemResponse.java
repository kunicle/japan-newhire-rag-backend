package com.teamproject.japan_newhire_rag_backend.rag.controller.dto;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.rag.application.RagQuestionHistoryItem;

public record RagQuestionHistoryItemResponse(
        Long questionId,
        String question,
        String status,
        LocalDateTime askedAt) {

    public static RagQuestionHistoryItemResponse from(RagQuestionHistoryItem item) {
        return new RagQuestionHistoryItemResponse(
                item.questionId(),
                item.question(),
                item.status(),
                item.askedAt());
    }
}
