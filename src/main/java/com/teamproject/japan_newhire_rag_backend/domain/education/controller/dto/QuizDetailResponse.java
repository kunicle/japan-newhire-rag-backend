package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.math.BigDecimal;
import java.util.List;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Quiz;

public record QuizDetailResponse(
        Long quizId,
        Long courseId,
        Long courseModuleId,
        String quizTitle,
        BigDecimal passingScore,
        Integer maxAttemptCount,
        int attemptsUsed,
        List<QuizQuestionResponse> questions
) {
    public static QuizDetailResponse from(
            Quiz quiz,
            int attemptsUsed,
            List<QuizQuestionResponse> questions
    ) {
        Long moduleId = quiz.getCourseModule() == null
                ? null
                : quiz.getCourseModule().getCourseModuleId();

        return new QuizDetailResponse(
                quiz.getQuizId(),
                quiz.getCourse().getCourseId(),
                moduleId,
                quiz.getQuizTitle(),
                quiz.getPassingScore(),
                quiz.getMaxAttemptCount(),
                attemptsUsed,
                questions);
    }
}