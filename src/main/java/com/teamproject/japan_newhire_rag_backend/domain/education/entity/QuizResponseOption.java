package com.teamproject.japan_newhire_rag_backend.domain.education.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(name = "quiz_response_option", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_quiz_response_option",
                columnNames = {"quiz_response_id", "quiz_option_id"})
})
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizResponseOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_response_option_id")
    private Long quizResponseOptionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_response_id", nullable = false)
    private QuizResponse quizResponse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_option_id", nullable = false)
    private QuizOption quizOption;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static QuizResponseOption create(
            QuizResponse quizResponse,
            QuizOption quizOption
    ) {
        QuizResponseOption responseOption = new QuizResponseOption();
        responseOption.quizResponse = quizResponse;
        responseOption.quizOption = quizOption;
        return responseOption;
    }
}