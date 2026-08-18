package com.teamproject.japan_newhire_rag_backend.document.version.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.entity.Document;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.document.version.repository.DocumentVersionRepository;

@Service
@Transactional
public class DocumentPublicationService {

    private static final String ACTIVE_DOCUMENT_STATUS = "ACTIVE";
    private static final String PUBLIC_PUBLICATION_STATUS = "PUBLIC";

    private final DocumentVersionRepository documentVersionRepository;

    public DocumentPublicationService(DocumentVersionRepository documentVersionRepository) {
        this.documentVersionRepository = documentVersionRepository;
    }

    public DocumentPublicationResult publish(
            Long documentId,
            Long documentVersionId,
            Long publishedByAppUserId) {
        validateArguments(documentId, documentVersionId, publishedByAppUserId);

        List<DocumentVersion> lockedVersions =
                documentVersionRepository.findForUpdateByDocument_DocumentId(documentId);
        DocumentVersion target = lockedVersions.stream()
                .filter(version -> Objects.equals(
                        version.getDocumentVersionId(), documentVersionId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "문서 버전을 찾을 수 없습니다."));

        validateDocument(target.getDocument());
        if (PUBLIC_PUBLICATION_STATUS.equals(target.getPublicationStatus())
                && target.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 공개된 버전입니다.");
        }

        LocalDateTime publishedAt = LocalDateTime.now();
        for (DocumentVersion version : lockedVersions) {
            if (!Objects.equals(version.getDocumentVersionId(), documentVersionId)
                    && version.isActive()) {
                version.deactivate();
            }
        }
        target.publish(publishedByAppUserId, publishedAt);

        return new DocumentPublicationResult(
                documentId,
                target.getDocumentVersionId(),
                target.getPublicationStatus(),
                target.isActive(),
                target.getPublishedAt(),
                target.getPublishedBy());
    }

    private void validateArguments(
            Long documentId,
            Long documentVersionId,
            Long publishedByAppUserId) {
        if (documentId == null) {
            throw new IllegalArgumentException("문서 ID가 없습니다.");
        }
        if (documentVersionId == null) {
            throw new IllegalArgumentException("문서 버전 ID가 없습니다.");
        }
        if (publishedByAppUserId == null) {
            throw new IllegalArgumentException("공개 처리자 ID가 없습니다.");
        }
    }

    private void validateDocument(Document document) {
        if (!ACTIVE_DOCUMENT_STATUS.equals(document.getDocumentStatus())
                || document.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "문서를 찾을 수 없습니다.");
        }
    }
}
