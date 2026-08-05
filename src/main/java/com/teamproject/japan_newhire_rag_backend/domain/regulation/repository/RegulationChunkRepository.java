package com.teamproject.japan_newhire_rag_backend.domain.regulation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.regulation.entity.RegulationChunk;

public interface RegulationChunkRepository
        extends JpaRepository<RegulationChunk, Long> {

    List<RegulationChunk> findAllByDocument_IdOrderByChunkIndexAsc(
            Long documentId
    );
}
