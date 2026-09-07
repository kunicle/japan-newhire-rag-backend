package com.teamproject.japan_newhire_rag_backend.rag.application;

import java.time.LocalDateTime;
import java.util.List;

import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagCitationSnapshot;

public record RagQuestionHistoryDetail(
        Long questionId,
        String question,
        String status,
        LocalDateTime askedAt,
        String answer,
        List<RagCitationSnapshot> citations) {
}
