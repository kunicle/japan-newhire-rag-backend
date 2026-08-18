package com.teamproject.japan_newhire_rag_backend.rag.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagSearch;

public interface RagSearchRepository extends JpaRepository<RagSearch, Long> {
}
