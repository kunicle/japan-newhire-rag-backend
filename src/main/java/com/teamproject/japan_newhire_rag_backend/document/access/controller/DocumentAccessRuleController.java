package com.teamproject.japan_newhire_rag_backend.document.access.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.document.access.controller.dto.DocumentAccessRuleReplaceRequest;
import com.teamproject.japan_newhire_rag_backend.document.access.controller.dto.DocumentAccessRuleResponse;
import com.teamproject.japan_newhire_rag_backend.document.access.service.DocumentAccessRuleResult;
import com.teamproject.japan_newhire_rag_backend.document.access.service.DocumentAccessRuleManagementService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;

@RestController
@RequestMapping("/api/documents")
@PreAuthorize("hasAnyRole('HR_MANAGER', 'SYSTEM_ADMIN')")
public class DocumentAccessRuleController {

    private final DocumentAccessRuleManagementService documentAccessRuleManagementService;
    private final CurrentUserProvider currentUserProvider;

    public DocumentAccessRuleController(
            DocumentAccessRuleManagementService documentAccessRuleManagementService,
            CurrentUserProvider currentUserProvider) {
        this.documentAccessRuleManagementService = documentAccessRuleManagementService;
        this.currentUserProvider = currentUserProvider;
    }

    @PutMapping("/{documentId}/versions/{versionId}/access-rule")
    public DocumentAccessRuleResponse replace(
            @PathVariable Long documentId,
            @PathVariable Long versionId,
            @RequestBody DocumentAccessRuleReplaceRequest request) {
        Long appUserId = currentUserProvider.getCurrentUser().appUserId();
        DocumentAccessRuleResult result = documentAccessRuleManagementService.replace(
                documentId,
                versionId,
                request.toCommand(),
                appUserId);
        return DocumentAccessRuleResponse.from(result);
    }
}
