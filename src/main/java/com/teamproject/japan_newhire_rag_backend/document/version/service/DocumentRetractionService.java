package com.teamproject.japan_newhire_rag_backend.document.version.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.entity.Document;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.document.version.repository.DocumentVersionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordService;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;

@Service
@Transactional
public class DocumentRetractionService {

    private static final String ACTIVE_DOCUMENT_STATUS = "ACTIVE";
    private static final String PUBLIC_PUBLICATION_STATUS = "PUBLIC";
    private static final String RETRACTED_PUBLICATION_STATUS = "RETRACTED";

    private final DocumentVersionRepository documentVersionRepository;
    private final AuditLogRecordService auditLogRecordService;

    public DocumentRetractionService(
            DocumentVersionRepository documentVersionRepository,
            AuditLogRecordService auditLogRecordService) {
        this.documentVersionRepository = documentVersionRepository;
        this.auditLogRecordService = auditLogRecordService;
    }

    public DocumentRetractionResult retract(
            Long documentId,
            Long documentVersionId,
            Long retractedByAppUserId) {
        validateArguments(documentId, documentVersionId, retractedByAppUserId);

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
        validateTransition(target);

        String previousPublicationStatus = target.getPublicationStatus();
        boolean previousActive = target.isActive();
        LocalDateTime retractedAt = LocalDateTime.now();
        target.retract();

        auditLogRecordService.record(new AuditLogRecordCommand(
                retractedByAppUserId,
                AuditActionType.DOCUMENT_VERSION_RETRACTED,
                target.getDocumentVersionId(),
                Map.of(
                        "publicationStatus", previousPublicationStatus,
                        "isActive", previousActive),
                Map.of(
                        "publicationStatus", target.getPublicationStatus(),
                        "isActive", target.isActive()),
                null,
                null));

        return new DocumentRetractionResult(
                documentId,
                target.getDocumentVersionId(),
                target.getPublicationStatus(),
                target.isActive(),
                retractedAt,
                retractedByAppUserId);
    }

    private void validateTransition(DocumentVersion target) {
        if (RETRACTED_PUBLICATION_STATUS.equals(target.getPublicationStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 철회된 버전입니다.");
        }
        if (!PUBLIC_PUBLICATION_STATUS.equals(target.getPublicationStatus())
                || !target.isActive()) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "현재 공개 중인 버전만 철회할 수 있습니다.");
        }
    }

    private void validateDocument(Document document) {
        if (!ACTIVE_DOCUMENT_STATUS.equals(document.getDocumentStatus())
                || document.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "문서를 찾을 수 없습니다.");
        }
    }

    private void validateArguments(
            Long documentId,
            Long documentVersionId,
            Long retractedByAppUserId) {
        if (documentId == null) {
            throw new IllegalArgumentException("문서 ID가 없습니다.");
        }
        if (documentVersionId == null) {
            throw new IllegalArgumentException("문서 버전 ID가 없습니다.");
        }
        if (retractedByAppUserId == null) {
            throw new IllegalArgumentException("철회 처리자 ID가 없습니다.");
        }
    }
}
