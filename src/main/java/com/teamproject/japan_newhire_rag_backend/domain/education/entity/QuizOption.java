package com.teamproject.japan_newhire_rag_backend.domain.education.entity;

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
@Table(name = "quiz_option", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_quiz_option_order",
                columnNames = {"quiz_question_id", "option_order"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_option_id")
    private Long quizOptionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_question_id", nullable = false)
    private QuizQuestion quizQuestion;

    @Column(name = "option_content", nullable = false, length = 1000)
    private String optionContent;

    @Column(name = "option_order", nullable = false)
    private int optionOrder;

    @Column(name = "is_correct", nullable = false)
    private boolean correct = false;

    public static QuizOption create(
            QuizQuestion quizQuestion,
            String optionContent,
            int optionOrder,
            boolean correct
    ) {
        QuizOption option = new QuizOption();
        option.quizQuestion = quizQuestion;
        option.optionContent = optionContent;
        option.optionOrder = optionOrder;
        option.correct = correct;
        return option;
    }
}