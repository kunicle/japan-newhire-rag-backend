package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import org.springframework.stereotype.Service;

import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;

@Service
public class DocumentProcessingRetryService {

    private final DocumentProcessingRetryPreparationService preparationService;
    private final DocumentEmbeddingOrchestrationService embeddingOrchestrationService;

    public DocumentProcessingRetryService(
            DocumentProcessingRetryPreparationService preparationService,
            DocumentEmbeddingOrchestrationService embeddingOrchestrationService) {
        this.preparationService = preparationService;
        this.embeddingOrchestrationService = embeddingOrchestrationService;
    }

    public DocumentProcessingJob retry(Long jobId) {
        DocumentProcessingJob newJob = preparationService.prepare(jobId);
        return embeddingOrchestrationService.processEmbeddings(
                newJob,
                newJob.getDocumentVersion());
    }
}
