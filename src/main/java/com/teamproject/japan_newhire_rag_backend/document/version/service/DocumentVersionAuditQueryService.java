package com.teamproject.japan_newhire_rag_backend.document.version.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.version.controller.dto.DocumentVersionAuditEventPageResponse;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.document.version.repository.DocumentVersionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditTargetType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.AuditLogQueryService;

@Service
@Transactional(readOnly = true)
public class DocumentVersionAuditQueryService {

    private final DocumentVersionRepository documentVersionRepository;
    private final AuditLogQueryService auditLogQueryService;

    public DocumentVersionAuditQueryService(
            DocumentVersionRepository documentVersionRepository,
            AuditLogQueryService auditLogQueryService) {
        this.documentVersionRepository = documentVersionRepository;
        this.auditLogQueryService = auditLogQueryService;
    }

    public DocumentVersionAuditEventPageResponse findAll(
            Long documentId,
            Long documentVersionId,
            int page,
            int size) {
        validateArguments(documentId, documentVersionId);
        DocumentVersion version = documentVersionRepository.findById(documentVersionId)
                .filter(value -> Objects.equals(
                        value.getDocument().getDocumentId(), documentId))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "문서 버전을 찾을 수 없습니다."));

        return DocumentVersionAuditEventPageResponse.from(auditLogQueryService.findAll(
                AuditActionType.DOCUMENT_VERSION_RETRACTED,
                null,
                AuditTargetType.DOCUMENT_VERSION,
                version.getDocumentVersionId(),
                null,
                null,
                page,
                size));
    }

    private void validateArguments(Long documentId, Long documentVersionId) {
        if (documentId == null) {
            throw new IllegalArgumentException("문서 ID가 없습니다.");
        }
        if (documentVersionId == null) {
            throw new IllegalArgumentException("문서 버전 ID가 없습니다.");
        }
    }
}
