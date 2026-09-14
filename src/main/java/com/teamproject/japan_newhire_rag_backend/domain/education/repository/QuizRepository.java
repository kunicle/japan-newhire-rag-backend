package com.teamproject.japan_newhire_rag_backend.domain.education.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @EntityGraph(attributePaths = {"course", "courseModule"})
    Optional<Quiz> findByQuizIdAndActiveTrue(Long quizId);

    @EntityGraph(attributePaths = {"course", "courseModule"})
    List<Quiz> findAllByCourse_CourseIdOrderByCreatedAtDescQuizIdDesc(
            Long courseId);

    @EntityGraph(attributePaths = {"course", "courseModule"})
    Optional<Quiz> findByQuizIdAndCourse_CourseId(
            Long quizId,
            Long courseId);

    List<Quiz> findAllByCourse_CourseIdAndActiveTrueOrderByQuizIdAsc(
            Long courseId);
}