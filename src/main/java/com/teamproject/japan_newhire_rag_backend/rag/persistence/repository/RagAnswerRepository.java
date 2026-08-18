package com.teamproject.japan_newhire_rag_backend.rag.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagAnswer;

public interface RagAnswerRepository extends JpaRepository<RagAnswer, Long> {
}
