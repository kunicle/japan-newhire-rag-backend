package com.teamproject.japan_newhire_rag_backend.document.version.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.document.version.controller.dto.DocumentPublicationResponse;
import com.teamproject.japan_newhire_rag_backend.document.version.service.DocumentPublicationResult;
import com.teamproject.japan_newhire_rag_backend.document.version.service.DocumentPublicationService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;

@RestController
@RequestMapping("/api/documents")
@PreAuthorize("hasAnyRole('HR_MANAGER', 'SYSTEM_ADMIN')")
public class DocumentPublicationController {

    private final DocumentPublicationService documentPublicationService;
    private final CurrentUserProvider currentUserProvider;

    public DocumentPublicationController(
            DocumentPublicationService documentPublicationService,
            CurrentUserProvider currentUserProvider) {
        this.documentPublicationService = documentPublicationService;
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
}
