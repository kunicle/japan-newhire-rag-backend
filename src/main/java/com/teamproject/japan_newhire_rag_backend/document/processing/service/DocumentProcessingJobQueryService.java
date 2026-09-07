package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobRepository;

@Service
@Transactional(readOnly = true)
public class DocumentProcessingJobQueryService {

    private final DocumentProcessingJobRepository documentProcessingJobRepository;

    public DocumentProcessingJobQueryService(
            DocumentProcessingJobRepository documentProcessingJobRepository) {
        this.documentProcessingJobRepository = documentProcessingJobRepository;
    }

    public List<DocumentProcessingJobStatus> getProcessingJobs() {
        return documentProcessingJobRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toStatus)
                .toList();
    }

    private DocumentProcessingJobStatus toStatus(DocumentProcessingJob job) {
        return new DocumentProcessingJobStatus(
                job.getDocumentProcessingJobId(),
                job.getDocumentVersion().getDocumentVersionId(),
                job.getProcessingStatus(),
                job.getFailureReason(),
                job.getCreatedAt());
    }
}
