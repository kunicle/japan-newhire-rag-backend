package com.teamproject.japan_newhire_rag_backend.domain.education.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizResponse;

public interface QuizResponseRepository
        extends JpaRepository<QuizResponse, Long> {

    @EntityGraph(attributePaths = "quizQuestion")
    List<QuizResponse>
    findAllByQuizAttempt_QuizAttemptIdOrderByQuizQuestion_QuestionOrderAsc(
            Long quizAttemptId);
}