package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Quiz;

public record MyCourseQuizSummaryResponse(
        Long quizId,
        String quizTitle,
        boolean required
) {

    public MyCourseQuizSummaryResponse(
            Long quizId,
            String quizTitle
    ) {
        this(quizId, quizTitle, true);
    }

    public static MyCourseQuizSummaryResponse from(Quiz quiz) {
        return new MyCourseQuizSummaryResponse(
                quiz.getQuizId(),
                quiz.getQuizTitle(),
                quiz.isRequired());
    }
}
