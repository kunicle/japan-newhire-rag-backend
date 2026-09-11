package com.teamproject.japan_newhire_rag_backend.domain.education.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizAttempt;

public interface QuizAttemptRepository
        extends JpaRepository<QuizAttempt, Long> {

    @EntityGraph(attributePaths = {"quiz", "courseEnrollment"})
    Optional<QuizAttempt>
    findTopByQuiz_QuizIdAndEmployeeIdAndCourseEnrollment_CourseEnrollmentIdOrderByAttemptNumberDesc(
            Long quizId,
            Long employeeId,
            Long courseEnrollmentId);
}