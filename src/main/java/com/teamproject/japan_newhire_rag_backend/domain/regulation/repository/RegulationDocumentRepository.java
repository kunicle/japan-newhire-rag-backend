package com.teamproject.japan_newhire_rag_backend.domain.regulation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.regulation.entity.RegulationDocument;

public interface RegulationDocumentRepository
        extends JpaRepository<RegulationDocument, Long> {
}
