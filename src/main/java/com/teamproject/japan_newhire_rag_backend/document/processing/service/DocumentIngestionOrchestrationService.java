package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;
import com.teamproject.japan_newhire_rag_backend.document.service.DocumentLifecycleService;
import com.teamproject.japan_newhire_rag_backend.document.storage.DocumentStorageService;
import com.teamproject.japan_newhire_rag_backend.document.validation.TxtDocumentValidator;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

@Service
public class DocumentIngestionOrchestrationService {

    private final TxtDocumentValidator txtDocumentValidator;
    private final DocumentStorageService documentStorageService;
    private final DocumentLifecycleService documentLifecycleService;
    private final DocumentChunkingProcessingService documentChunkingProcessingService;
    private final DocumentEmbeddingOrchestrationService documentEmbeddingOrchestrationService;

    public DocumentIngestionOrchestrationService(
            TxtDocumentValidator txtDocumentValidator,
            DocumentStorageService documentStorageService,
            DocumentLifecycleService documentLifecycleService,
            DocumentChunkingProcessingService documentChunkingProcessingService,
            DocumentEmbeddingOrchestrationService documentEmbeddingOrchestrationService) {
        this.txtDocumentValidator = txtDocumentValidator;
        this.documentStorageService = documentStorageService;
        this.documentLifecycleService = documentLifecycleService;
        this.documentChunkingProcessingService = documentChunkingProcessingService;
        this.documentEmbeddingOrchestrationService = documentEmbeddingOrchestrationService;
    }

    public DocumentIngestionResult ingest(
            Long documentCategoryId,
            String documentName,
            String documentDescription,
            String versionName,
            String originalFileName,
            byte[] content,
            Long createdByAppUserId) {
        txtDocumentValidator.validate(originalFileName, content);
        String text = new String(content, StandardCharsets.UTF_8);
        String storedFilePath = documentStorageService.store(originalFileName, content);

        DocumentVersion documentVersion;
        try {
            documentVersion = documentLifecycleService.createDocumentWithInitialVersion(
                    documentCategoryId,
                    documentName,
                    documentDescription,
                    versionName,
                    originalFileName,
                    storedFilePath,
                    content.length,
                    createdByAppUserId);
        } catch (RuntimeException original) {
            try {
                documentStorageService.delete(storedFilePath);
            } catch (RuntimeException cleanupFailure) {
                original.addSuppressed(cleanupFailure);
            }
            throw original;
        }

        DocumentProcessingJob job = documentChunkingProcessingService.processChunking(
                documentVersion,
                text,
                createdByAppUserId);
        DocumentProcessingJob finalJob =
                documentEmbeddingOrchestrationService.processEmbeddings(job, documentVersion);
        return new DocumentIngestionResult(
                documentVersion.getDocument().getDocumentId(),
                documentVersion.getDocumentVersionId(),
                finalJob.getDocumentProcessingJobId(),
                finalJob.getProcessingStatus());
    }
}
