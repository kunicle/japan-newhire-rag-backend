package com.teamproject.japan_newhire_rag_backend.rag.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagAnswer;

public interface RagAnswerRepository extends JpaRepository<RagAnswer, Long> {

    Optional<RagAnswer> findByRagSearch_RagSearchId(Long ragSearchId);
}
