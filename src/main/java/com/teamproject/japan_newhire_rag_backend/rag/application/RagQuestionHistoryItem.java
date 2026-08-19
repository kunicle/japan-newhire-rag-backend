package com.teamproject.japan_newhire_rag_backend.rag.application;

import java.time.LocalDateTime;

public record RagQuestionHistoryItem(
        Long questionId,
        String question,
        String status,
        LocalDateTime askedAt) {
}
