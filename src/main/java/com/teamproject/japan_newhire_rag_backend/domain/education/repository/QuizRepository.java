package com.teamproject.japan_newhire_rag_backend.domain.education.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @EntityGraph(attributePaths = {"course", "courseModule"})
    Optional<Quiz> findByQuizIdAndActiveTrue(Long quizId);
}