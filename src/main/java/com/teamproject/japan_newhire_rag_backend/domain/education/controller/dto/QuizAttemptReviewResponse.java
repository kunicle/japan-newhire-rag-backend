package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizAttempt;

public record QuizAttemptReviewResponse(
        Long attemptId,
        int attemptNumber,
        BigDecimal totalScore,
        boolean passed,
        Integer remainingAttemptCount,
        LocalDateTime submittedAt,
        List<QuizQuestionReviewResponse> questions
) {
    public static QuizAttemptReviewResponse from(
            QuizAttempt attempt,
            Integer remainingAttemptCount,
            List<QuizQuestionReviewResponse> questions
    ) {
        return new QuizAttemptReviewResponse(
                attempt.getQuizAttemptId(),
                attempt.getAttemptNumber(),
                attempt.getTotalScore(),
                Boolean.TRUE.equals(attempt.getPassed()),
                remainingAttemptCount,
                attempt.getSubmittedAt(),
                questions);
    }
}
