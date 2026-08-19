package com.teamproject.japan_newhire_rag_backend.rag.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagCitation;

public interface RagCitationRepository extends JpaRepository<RagCitation, Long> {

    List<RagCitation> findByRagAnswer_RagAnswerIdOrderByPositionAsc(Long ragAnswerId);
}
