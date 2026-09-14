package com.teamproject.japan_newhire_rag_backend.domain.education.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.teamproject.japan_newhire_rag_backend.domain.education.enums.QuizAttemptStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "quiz_attempt", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_quiz_attempt_number",
                columnNames = {
                        "quiz_id",
                        "employee_id",
                        "course_enrollment_id",
                        "attempt_number"
                })
})
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_attempt_id")
    private Long quizAttemptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_enrollment_id", nullable = false)
    private CourseEnrollment courseEnrollment;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "total_score", precision = 7, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "is_passed")
    private Boolean passed;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_status", nullable = false, length = 20)
    private QuizAttemptStatus attemptStatus = QuizAttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static QuizAttempt start(
            Quiz quiz,
            Long employeeId,
            CourseEnrollment courseEnrollment,
            int attemptNumber,
            LocalDateTime startedAt
    ) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.quiz = quiz;
        attempt.employeeId = employeeId;
        attempt.courseEnrollment = courseEnrollment;
        attempt.attemptNumber = attemptNumber;
        attempt.totalScore = null;
        attempt.passed = null;
        attempt.attemptStatus = QuizAttemptStatus.IN_PROGRESS;
        attempt.startedAt = startedAt;
        attempt.submittedAt = null;
        return attempt;
    }

    public void grade(
            BigDecimal totalScore,
            boolean passed,
            LocalDateTime submittedAt
    ) {
        this.totalScore = totalScore;
        this.passed = passed;
        this.attemptStatus = QuizAttemptStatus.GRADED;
        this.submittedAt = submittedAt;
    }
}