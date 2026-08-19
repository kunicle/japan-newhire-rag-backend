package com.teamproject.japan_newhire_rag_backend.document.processing.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.document.processing.controller.dto.DocumentProcessingJobStatusResponse;
import com.teamproject.japan_newhire_rag_backend.document.processing.service.DocumentProcessingJobQueryService;

@RestController
@RequestMapping("/api/hr/document-processing-jobs")
@PreAuthorize("hasRole('HR_MANAGER')")
public class DocumentProcessingJobController {

    private final DocumentProcessingJobQueryService documentProcessingJobQueryService;

    public DocumentProcessingJobController(
            DocumentProcessingJobQueryService documentProcessingJobQueryService) {
        this.documentProcessingJobQueryService = documentProcessingJobQueryService;
    }

    @GetMapping
    public List<DocumentProcessingJobStatusResponse> getProcessingJobs() {
        return documentProcessingJobQueryService.getProcessingJobs()
                .stream()
                .map(DocumentProcessingJobStatusResponse::from)
                .toList();
    }
}
