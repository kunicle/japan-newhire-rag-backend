package com.teamproject.japan_newhire_rag_backend.domain.education.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizQuestion;

public interface QuizQuestionRepository
        extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion>
    findAllByQuiz_QuizIdAndActiveTrueOrderByQuestionOrderAsc(
            Long quizId);
}