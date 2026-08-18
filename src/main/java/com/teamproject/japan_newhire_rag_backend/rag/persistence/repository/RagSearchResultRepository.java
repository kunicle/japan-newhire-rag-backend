package com.teamproject.japan_newhire_rag_backend.rag.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagSearchResult;

public interface RagSearchResultRepository extends JpaRepository<RagSearchResult, Long> {
}
