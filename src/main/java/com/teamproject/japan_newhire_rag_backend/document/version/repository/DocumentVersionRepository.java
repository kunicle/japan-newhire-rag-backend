package com.teamproject.japan_newhire_rag_backend.document.version.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    Optional<DocumentVersion> findByDocument_DocumentIdAndVersionName(
            Long documentId,
            String versionName);

    Optional<DocumentVersion> findByDocument_DocumentIdAndIsActiveTrue(Long documentId);

    List<DocumentVersion>
            findByDocument_DocumentStatusAndDocument_DeletedAtIsNullAndPublicationStatusAndIsActiveTrueAndEffectiveDateLessThanEqual(
                    String documentStatus,
                    String publicationStatus,
                    LocalDate today);
}
