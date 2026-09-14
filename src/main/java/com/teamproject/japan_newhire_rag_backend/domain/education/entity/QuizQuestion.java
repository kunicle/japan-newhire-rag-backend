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
@Table(name = "quiz_question", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_quiz_question_order",
                columnNames = {"quiz_id", "question_order"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_question_id")
    private Long quizQuestionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_content", nullable = false, columnDefinition = "TEXT")
    private String questionContent;

    @Column(name = "question_order", nullable = false)
    private int questionOrder;

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public static QuizQuestion create(
            Quiz quiz,
            String questionContent,
            int questionOrder,
            BigDecimal score
    ) {
        QuizQuestion question = new QuizQuestion();
        question.quiz = quiz;
        question.questionContent = questionContent;
        question.questionOrder = questionOrder;
        question.score = score;
        question.active = true;
        return question;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }
}