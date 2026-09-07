package com.teamproject.japan_newhire_rag_backend.document.version.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.document.version.repository.DocumentVersionRepository;

@Service
@Transactional(readOnly = true)
public class DocumentVersionCandidateService {

    private static final String ACTIVE_DOCUMENT_STATUS = "ACTIVE";
    private static final String PUBLIC_PUBLICATION_STATUS = "PUBLIC";

    private final DocumentVersionRepository documentVersionRepository;

    public DocumentVersionCandidateService(DocumentVersionRepository documentVersionRepository) {
        this.documentVersionRepository = documentVersionRepository;
    }

    public Set<Long> findCandidateDocumentVersionIds() {
        LocalDate today = LocalDate.now();

        List<DocumentVersion> documentVersions = documentVersionRepository
                .findByDocument_DocumentStatusAndDocument_DeletedAtIsNullAndPublicationStatusAndIsActiveTrueAndEffectiveDateLessThanEqual(
                        ACTIVE_DOCUMENT_STATUS,
                        PUBLIC_PUBLICATION_STATUS,
                        today);

        return documentVersions.stream()
                .filter(documentVersion -> isNotExpired(documentVersion, today))
                .map(DocumentVersion::getDocumentVersionId)
                .collect(Collectors.toSet());
    }

    private boolean isNotExpired(DocumentVersion documentVersion, LocalDate today) {
        LocalDate expirationDate = documentVersion.getExpirationDate();
        return expirationDate == null || !expirationDate.isBefore(today);
    }
}
