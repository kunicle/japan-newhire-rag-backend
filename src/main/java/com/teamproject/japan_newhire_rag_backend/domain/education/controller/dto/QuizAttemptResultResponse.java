package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizAttempt;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.QuizAttemptStatus;

public record QuizAttemptResultResponse(
        Long attemptId,
        int attemptNumber,
        BigDecimal totalScore,
        boolean passed,
        QuizAttemptStatus attemptStatus,
        Integer remainingAttemptCount,
        LocalDateTime submittedAt
) {
    public static QuizAttemptResultResponse from(
            QuizAttempt attempt,
            Integer remainingAttemptCount
    ) {
        return new QuizAttemptResultResponse(
                attempt.getQuizAttemptId(),
                attempt.getAttemptNumber(),
                attempt.getTotalScore(),
                Boolean.TRUE.equals(attempt.getPassed()),
                attempt.getAttemptStatus(),
                remainingAttemptCount,
                attempt.getSubmittedAt());
    }
}