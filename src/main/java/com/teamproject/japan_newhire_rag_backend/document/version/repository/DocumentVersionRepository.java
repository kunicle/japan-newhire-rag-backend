package com.teamproject.japan_newhire_rag_backend.document.version.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    Optional<DocumentVersion> findByDocument_DocumentIdAndVersionName(
            Long documentId,
            String versionName);

    Optional<DocumentVersion> findByDocument_DocumentIdAndIsActiveTrue(Long documentId);
}
