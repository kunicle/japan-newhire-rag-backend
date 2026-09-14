package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.math.BigDecimal;
import java.util.List;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizOption;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizQuestion;

public record QuizQuestionResponse(
        Long questionId,
        String questionContent,
        int questionOrder,
        BigDecimal score,
        List<QuizOptionResponse> options
) {
    public static QuizQuestionResponse from(
            QuizQuestion question,
            List<QuizOption> options
    ) {
        return new QuizQuestionResponse(
                question.getQuizQuestionId(),
                question.getQuestionContent(),
                question.getQuestionOrder(),
                question.getScore(),
                options.stream()
                        .map(QuizOptionResponse::from)
                        .toList());
    }
}