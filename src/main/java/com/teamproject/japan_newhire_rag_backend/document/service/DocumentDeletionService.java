package com.teamproject.japan_newhire_rag_backend.document.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.entity.Document;
import com.teamproject.japan_newhire_rag_backend.document.repository.DocumentRepository;

@Service
@Transactional
public class DocumentDeletionService {

    private static final String ACTIVE_DOCUMENT_STATUS = "ACTIVE";

    private final DocumentRepository documentRepository;

    public DocumentDeletionService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void deleteDocument(Long documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("문서 ID가 없습니다.");
        }

        Document document = documentRepository.findById(documentId)
                .filter(value -> ACTIVE_DOCUMENT_STATUS.equals(value.getDocumentStatus()))
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "문서를 찾을 수 없습니다."));

        document.softDelete();
    }
}
