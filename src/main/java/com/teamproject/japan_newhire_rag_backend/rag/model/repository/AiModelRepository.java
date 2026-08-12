package com.teamproject.japan_newhire_rag_backend.rag.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.rag.model.entity.AiModel;

public interface AiModelRepository extends JpaRepository<AiModel, Long> {
}
