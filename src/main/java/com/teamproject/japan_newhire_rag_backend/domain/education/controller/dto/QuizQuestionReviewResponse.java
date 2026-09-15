package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.math.BigDecimal;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizOption;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizQuestion;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizResponse;

public record QuizQuestionReviewResponse(
        Long questionId,
        String questionContent,
        int questionOrder,
        BigDecimal score,
        Long selectedOptionId,
        String selectedOptionContent,
        Long correctOptionId,
        String correctOptionContent,
        boolean correct,
        BigDecimal earnedScore
) {
    public static QuizQuestionReviewResponse from(
            QuizResponse response,
            QuizOption selectedOption,
            QuizOption correctOption
    ) {
        QuizQuestion question = response.getQuizQuestion();

        return new QuizQuestionReviewResponse(
                question.getQuizQuestionId(),
                question.getQuestionContent(),
                question.getQuestionOrder(),
                question.getScore(),
                selectedOption.getQuizOptionId(),
                selectedOption.getOptionContent(),
                correctOption.getQuizOptionId(),
                correctOption.getOptionContent(),
                Boolean.TRUE.equals(response.getCorrect()),
                response.getEarnedScore());
    }
}
