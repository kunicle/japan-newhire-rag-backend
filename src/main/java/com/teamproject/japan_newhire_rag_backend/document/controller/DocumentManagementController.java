package com.teamproject.japan_newhire_rag_backend.document.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.document.controller.dto.DocumentManagementDetailResponse;
import com.teamproject.japan_newhire_rag_backend.document.controller.dto.DocumentManagementListPageResponse;
import com.teamproject.japan_newhire_rag_backend.document.service.DocumentDeletionService;
import com.teamproject.japan_newhire_rag_backend.document.service.DocumentManagementQueryService;

@RestController
@RequestMapping("/api/documents")
@PreAuthorize("hasAnyRole('HR_MANAGER', 'SYSTEM_ADMIN')")
public class DocumentManagementController {

    private final DocumentManagementQueryService documentManagementQueryService;
    private final DocumentDeletionService documentDeletionService;

    public DocumentManagementController(
            DocumentManagementQueryService documentManagementQueryService,
            DocumentDeletionService documentDeletionService) {
        this.documentManagementQueryService = documentManagementQueryService;
        this.documentDeletionService = documentDeletionService;
    }

    @GetMapping
    public DocumentManagementListPageResponse getDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return DocumentManagementListPageResponse.from(
                documentManagementQueryService.getDocuments(keyword, page, size));
    }

    @GetMapping("/{documentId}")
    public DocumentManagementDetailResponse getDocument(@PathVariable Long documentId) {
        return documentManagementQueryService.getDocument(documentId);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable Long documentId) {
        documentDeletionService.deleteDocument(documentId);
    }
}
