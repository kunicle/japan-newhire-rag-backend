package com.teamproject.japan_newhire_rag_backend.document.version.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.document.version.controller.dto.DocumentPublicationResponse;
import com.teamproject.japan_newhire_rag_backend.document.version.controller.dto.DocumentRetractionResponse;
import com.teamproject.japan_newhire_rag_backend.document.version.controller.dto.DocumentVersionAuditEventPageResponse;
import com.teamproject.japan_newhire_rag_backend.document.version.service.DocumentPublicationResult;
import com.teamproject.japan_newhire_rag_backend.document.version.service.DocumentPublicationService;
import com.teamproject.japan_newhire_rag_backend.document.version.service.DocumentRetractionResult;
import com.teamproject.japan_newhire_rag_backend.document.version.service.DocumentRetractionService;
import com.teamproject.japan_newhire_rag_backend.document.version.service.DocumentVersionAuditQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;

@RestController
@RequestMapping("/api/documents")
@PreAuthorize("hasAnyRole('HR_MANAGER', 'SYSTEM_ADMIN')")
public class DocumentPublicationController {

    private final DocumentPublicationService documentPublicationService;
    private final DocumentRetractionService documentRetractionService;
    private final DocumentVersionAuditQueryService documentVersionAuditQueryService;
    private final CurrentUserProvider currentUserProvider;

    public DocumentPublicationController(
            DocumentPublicationService documentPublicationService,
            DocumentRetractionService documentRetractionService,
            DocumentVersionAuditQueryService documentVersionAuditQueryService,
            CurrentUserProvider currentUserProvider) {
        this.documentPublicationService = documentPublicationService;
        this.documentRetractionService = documentRetractionService;
        this.documentVersionAuditQueryService = documentVersionAuditQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    @PatchMapping("/{documentId}/versions/{versionId}/publish")
    public DocumentPublicationResponse publish(
            @PathVariable Long documentId,
            @PathVariable Long versionId) {
        Long appUserId = currentUserProvider.getCurrentUser().appUserId();
        DocumentPublicationResult result = documentPublicationService.publish(
                documentId,
                versionId,
                appUserId);
        return DocumentPublicationResponse.from(result);
    }

    @PatchMapping("/{documentId}/versions/{versionId}/retract")
    public DocumentRetractionResponse retract(
            @PathVariable Long documentId,
            @PathVariable Long versionId) {
        Long appUserId = currentUserProvider.getCurrentUser().appUserId();
        DocumentRetractionResult result = documentRetractionService.retract(
                documentId,
                versionId,
                appUserId);
        return DocumentRetractionResponse.from(result);
    }

    @GetMapping("/{documentId}/versions/{versionId}/audit-events")
    public DocumentVersionAuditEventPageResponse findAuditEvents(
            @PathVariable Long documentId,
            @PathVariable Long versionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return documentVersionAuditQueryService.findAll(documentId, versionId, page, size);
    }
}
