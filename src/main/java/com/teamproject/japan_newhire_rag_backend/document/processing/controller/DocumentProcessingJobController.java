package com.teamproject.japan_newhire_rag_backend.document.processing.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.document.processing.controller.dto.DocumentProcessingJobStatusResponse;
import com.teamproject.japan_newhire_rag_backend.document.processing.service.DocumentProcessingJobQueryService;
import com.teamproject.japan_newhire_rag_backend.document.processing.service.DocumentProcessingJobStatus;
import com.teamproject.japan_newhire_rag_backend.document.processing.service.DocumentProcessingRetryService;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;

@RestController
@RequestMapping("/api/hr/document-processing-jobs")
@PreAuthorize("hasRole('HR_MANAGER')")
public class DocumentProcessingJobController {

    private final DocumentProcessingJobQueryService documentProcessingJobQueryService;
    private final DocumentProcessingRetryService documentProcessingRetryService;

    public DocumentProcessingJobController(
            DocumentProcessingJobQueryService documentProcessingJobQueryService,
            DocumentProcessingRetryService documentProcessingRetryService) {
        this.documentProcessingJobQueryService = documentProcessingJobQueryService;
        this.documentProcessingRetryService = documentProcessingRetryService;
    }

    @GetMapping
    public List<DocumentProcessingJobStatusResponse> getProcessingJobs() {
        return documentProcessingJobQueryService.getProcessingJobs()
                .stream()
                .map(DocumentProcessingJobStatusResponse::from)
                .toList();
    }

    @PostMapping("/{jobId}/retry")
    public DocumentProcessingJobStatusResponse retry(@PathVariable Long jobId) {
        DocumentProcessingJob job = documentProcessingRetryService.retry(jobId);
        return DocumentProcessingJobStatusResponse.from(new DocumentProcessingJobStatus(
                job.getDocumentProcessingJobId(),
                job.getDocumentVersion().getDocumentVersionId(),
                job.getProcessingStatus(),
                job.getFailureReason(),
                job.getCreatedAt()));
    }
}
