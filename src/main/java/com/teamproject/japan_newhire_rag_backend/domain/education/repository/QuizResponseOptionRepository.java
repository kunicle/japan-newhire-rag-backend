package com.teamproject.japan_newhire_rag_backend.domain.education.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizResponseOption;

public interface QuizResponseOptionRepository
        extends JpaRepository<QuizResponseOption, Long> {

    @EntityGraph(attributePaths = {"quizResponse", "quizOption"})
    List<QuizResponseOption>
    findAllByQuizResponse_QuizAttempt_QuizAttemptId(
            Long quizAttemptId);
}