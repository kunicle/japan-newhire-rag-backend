package com.teamproject.japan_newhire_rag_backend.rag.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.teamproject.japan_newhire_rag_backend.rag.application.RagQuestionHistoryDetail;

public record RagQuestionHistoryDetailResponse(
        Long questionId,
        String question,
        String status,
        LocalDateTime askedAt,
        String answer,
        List<RagCitationResponse> citations) {

    public static RagQuestionHistoryDetailResponse from(RagQuestionHistoryDetail detail) {
        return new RagQuestionHistoryDetailResponse(
                detail.questionId(),
                detail.question(),
                detail.status(),
                detail.askedAt(),
                detail.answer(),
                detail.citations().stream()
                        .map(RagCitationResponse::from)
                        .toList());
    }
}
