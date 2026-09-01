package com.teamproject.japan_newhire_rag_backend.document.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.document.controller.dto.DocumentManagementDetailResponse;
import com.teamproject.japan_newhire_rag_backend.document.controller.dto.DocumentManagementListItemResponse;
import com.teamproject.japan_newhire_rag_backend.document.service.DocumentManagementQueryService;

@RestController
@RequestMapping("/api/documents")
@PreAuthorize("hasAnyRole('HR_MANAGER', 'SYSTEM_ADMIN')")
public class DocumentManagementController {

    private final DocumentManagementQueryService documentManagementQueryService;

    public DocumentManagementController(
            DocumentManagementQueryService documentManagementQueryService) {
        this.documentManagementQueryService = documentManagementQueryService;
    }

    @GetMapping
    public List<DocumentManagementListItemResponse> getDocuments() {
        return documentManagementQueryService.getDocuments();
    }

    @GetMapping("/{documentId}")
    public DocumentManagementDetailResponse getDocument(@PathVariable Long documentId) {
        return documentManagementQueryService.getDocument(documentId);
    }
}
