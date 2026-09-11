package com.teamproject.japan_newhire_rag_backend.domain.education.entity;

import java.math.BigDecimal;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "quiz_response", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_quiz_response_question",
                columnNames = {"quiz_attempt_id", "quiz_question_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizResponse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_response_id")
    private Long quizResponseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_attempt_id", nullable = false)
    private QuizAttempt quizAttempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_question_id", nullable = false)
    private QuizQuestion quizQuestion;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "earned_score", precision = 5, scale = 2)
    private BigDecimal earnedScore;

    public static QuizResponse createGraded(
            QuizAttempt quizAttempt,
            QuizQuestion quizQuestion,
            boolean correct,
            BigDecimal earnedScore
    ) {
        QuizResponse response = new QuizResponse();
        response.quizAttempt = quizAttempt;
        response.quizQuestion = quizQuestion;
        response.correct = correct;
        response.earnedScore = earnedScore;
        return response;
    }
}