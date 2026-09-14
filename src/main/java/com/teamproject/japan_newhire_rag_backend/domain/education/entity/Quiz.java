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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "quiz")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quiz extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Long quizId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_module_id")
    private CourseModule courseModule;

    @Column(name = "quiz_title", nullable = false, length = 200)
    private String quizTitle;

    @Column(name = "passing_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal passingScore;

    @Column(name = "max_attempt_count")
    private Integer maxAttemptCount;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    public static Quiz create(
            Course course,
            CourseModule courseModule,
            String quizTitle,
            BigDecimal passingScore,
            Integer maxAttemptCount,
            Long createdBy
    ) {
        Quiz quiz = new Quiz();
        quiz.course = course;
        quiz.courseModule = courseModule;
        quiz.quizTitle = quizTitle;
        quiz.passingScore = passingScore;
        quiz.maxAttemptCount = maxAttemptCount;
        quiz.active = true;
        quiz.createdBy = createdBy;
        return quiz;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }
}