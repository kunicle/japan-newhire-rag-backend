package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Quiz;

public record MyCourseQuizSummaryResponse(
        Long quizId,
        String quizTitle
) {

    public static MyCourseQuizSummaryResponse from(Quiz quiz) {
        return new MyCourseQuizSummaryResponse(
                quiz.getQuizId(),
                quiz.getQuizTitle());
    }
}