package com.teamproject.japan_newhire_rag_backend.domain.education.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizOption;

public interface QuizOptionRepository
        extends JpaRepository<QuizOption, Long> {

    @EntityGraph(attributePaths = "quizQuestion")
    List<QuizOption>
    findAllByQuizQuestion_QuizQuestionIdInOrderByQuizQuestion_QuestionOrderAscOptionOrderAsc(
            Collection<Long> questionIds);

    @EntityGraph(attributePaths = "quizQuestion")
    List<QuizOption> findAllByQuizOptionIdIn(
            Collection<Long> optionIds);
}