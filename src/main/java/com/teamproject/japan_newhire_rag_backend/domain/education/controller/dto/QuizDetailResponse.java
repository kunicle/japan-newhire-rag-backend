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
        boolean required,
        int attemptsUsed,
        List<QuizQuestionResponse> questions,
        QuizAttemptReviewResponse latestAttemptReview
) {
    public QuizDetailResponse(
            Long quizId,
            Long courseId,
            Long courseModuleId,
            String quizTitle,
            BigDecimal passingScore,
            Integer maxAttemptCount,
            boolean required,
            int attemptsUsed,
            List<QuizQuestionResponse> questions
    ) {
        this(
                quizId,
                courseId,
                courseModuleId,
                quizTitle,
                passingScore,
                maxAttemptCount,
                required,
                attemptsUsed,
                questions,
                null);
    }

    public QuizDetailResponse(
            Long quizId,
            Long courseId,
            Long courseModuleId,
            String quizTitle,
            BigDecimal passingScore,
            Integer maxAttemptCount,
            int attemptsUsed,
            List<QuizQuestionResponse> questions
    ) {
        this(
                quizId,
                courseId,
                courseModuleId,
                quizTitle,
                passingScore,
                maxAttemptCount,
                true,
                attemptsUsed,
                questions,
                null);
    }

    public static QuizDetailResponse from(
            Quiz quiz,
            int attemptsUsed,
            List<QuizQuestionResponse> questions
    ) {
        return from(quiz, attemptsUsed, questions, null);
    }

    public static QuizDetailResponse from(
            Quiz quiz,
            int attemptsUsed,
            List<QuizQuestionResponse> questions,
            QuizAttemptReviewResponse latestAttemptReview
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
                quiz.isRequired(),
                attemptsUsed,
                questions,
                latestAttemptReview);
    }
}
