package com.teamproject.japan_newhire_rag_backend.rag.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagQuestion;

public interface RagQuestionRepository extends JpaRepository<RagQuestion, Long> {

    List<RagQuestion> findByCreatedByOrderByCreatedAtDesc(Long createdBy);

    Optional<RagQuestion> findByRagQuestionIdAndCreatedBy(Long ragQuestionId, Long createdBy);
}
