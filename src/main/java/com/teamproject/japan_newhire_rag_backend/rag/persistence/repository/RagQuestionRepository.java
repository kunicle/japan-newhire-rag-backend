package com.teamproject.japan_newhire_rag_backend.rag.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagQuestion;

public interface RagQuestionRepository extends JpaRepository<RagQuestion, Long> {

    List<RagQuestion> findByCreatedByOrderByCreatedAtDesc(Long createdBy);

    Optional<RagQuestion> findByRagQuestionIdAndCreatedBy(Long ragQuestionId, Long createdBy);

    @Query("""
            SELECT q FROM RagQuestion q
            WHERE q.createdBy = :createdBy
              AND (:keyword IS NULL OR q.questionText LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<RagQuestion> findAllByCreatedByAndKeyword(
            @Param("createdBy") Long createdBy,
            @Param("keyword") String keyword,
            Pageable pageable);
}
