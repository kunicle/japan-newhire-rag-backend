package com.teamproject.japan_newhire_rag_backend.document.access.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.version.service.DocumentVersionCandidateService;

@Service
@Transactional(readOnly = true)
public class DocumentSearchScopeService {

    private final DocumentVersionCandidateService documentVersionCandidateService;
    private final DocumentAccessScopeService documentAccessScopeService;

    public DocumentSearchScopeService(
            DocumentVersionCandidateService documentVersionCandidateService,
            DocumentAccessScopeService documentAccessScopeService) {
        this.documentVersionCandidateService = documentVersionCandidateService;
        this.documentAccessScopeService = documentAccessScopeService;
    }

    public Set<Long> findAllowedDocumentVersionIds() {
        Set<Long> candidateVersionIds = documentVersionCandidateService.findCandidateDocumentVersionIds();
        if (candidateVersionIds.isEmpty()) {
            return Set.of();
        }
        return documentAccessScopeService.filterAccessibleDocumentVersionIds(candidateVersionIds);
    }
}
