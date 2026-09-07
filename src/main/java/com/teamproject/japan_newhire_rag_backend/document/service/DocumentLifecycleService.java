package com.teamproject.japan_newhire_rag_backend.document.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.category.entity.DocumentCategory;
import com.teamproject.japan_newhire_rag_backend.document.category.repository.DocumentCategoryRepository;
import com.teamproject.japan_newhire_rag_backend.document.entity.Document;
import com.teamproject.japan_newhire_rag_backend.document.repository.DocumentRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.document.version.repository.DocumentVersionRepository;

@Service
@Transactional
public class DocumentLifecycleService {

    private final DocumentCategoryRepository documentCategoryRepository;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;

    public DocumentLifecycleService(
            DocumentCategoryRepository documentCategoryRepository,
            DocumentRepository documentRepository,
            DocumentVersionRepository documentVersionRepository) {
        this.documentCategoryRepository = documentCategoryRepository;
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
    }

    public DocumentVersion createDocumentWithInitialVersion(
            Long documentCategoryId,
            String documentName,
            String documentDescription,
            String versionName,
            String originalFileName,
            String storedFilePath,
            long fileSize,
            Long createdByAppUserId) {
        if (documentCategoryId == null) {
            throw new IllegalArgumentException("문서 카테고리 ID가 없습니다.");
        }

        DocumentCategory category = documentCategoryRepository.findById(documentCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("문서 카테고리가 없습니다."));
        if (!category.isActive()) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "비활성 문서 카테고리는 사용할 수 없습니다.");
        }

        Document document = Document.create(
                category,
                documentName,
                documentDescription,
                createdByAppUserId);
        Document savedDocument = documentRepository.save(document);
        DocumentVersion version = DocumentVersion.create(
                savedDocument,
                versionName,
                LocalDate.now(),
                null,
                originalFileName,
                storedFilePath,
                fileSize);
        return documentVersionRepository.save(version);
    }
}
