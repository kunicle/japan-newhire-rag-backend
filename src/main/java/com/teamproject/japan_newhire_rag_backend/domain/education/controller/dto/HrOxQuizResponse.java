package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.math.BigDecimal;
import java.util.List;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Quiz;

public record HrOxQuizResponse(
        Long quizId,
        Long courseId,
        String quizTitle,
        BigDecimal passingScore,
        Integer maxAttemptCount,
        boolean active,
        Long createdBy,
        List<Question> questions
) {

    public static HrOxQuizResponse from(
            Quiz quiz,
            List<Question> questions
    ) {
        return new HrOxQuizResponse(
                quiz.getQuizId(),
                quiz.getCourse().getCourseId(),
                quiz.getQuizTitle(),
                quiz.getPassingScore(),
                quiz.getMaxAttemptCount(),
                quiz.isActive(),
                quiz.getCreatedBy(),
                questions);
    }

    public record Question(
            Long questionId,
            String questionContent,
            int questionOrder,
            BigDecimal score,
            String correctAnswer
    ) {
    }
}