package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.document.chunk.repository.DocumentChunkRepository;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;

@Service
public class DocumentProcessingRetryPreparationService {

    private static final String FAILED_STATUS = "FAILED";
    private static final String ACTIVE_CHUNK_STATUS = "ACTIVE";

    private final DocumentProcessingJobRepository documentProcessingJobRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final CurrentUserProvider currentUserProvider;

    public DocumentProcessingRetryPreparationService(
            DocumentProcessingJobRepository documentProcessingJobRepository,
            DocumentChunkRepository documentChunkRepository,
            CurrentUserProvider currentUserProvider) {
        this.documentProcessingJobRepository = documentProcessingJobRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public DocumentProcessingJob prepare(Long jobId) {
        DocumentProcessingJob targetJob = documentProcessingJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "처리 작업을 찾을 수 없습니다."));
        DocumentVersion documentVersion = targetJob.getDocumentVersion();
        Long documentVersionId = documentVersion.getDocumentVersionId();

        List<DocumentProcessingJob> lockedJobs = documentProcessingJobRepository
                .findForUpdateByDocumentVersion_DocumentVersionIdOrderByCreatedAtDescDocumentProcessingJobIdDesc(
                        documentVersionId);
        DocumentProcessingJob latestJob = lockedJobs.stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "처리 작업을 찾을 수 없습니다."));

        if (!Objects.equals(latestJob.getDocumentProcessingJobId(), jobId)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "최신 처리 작업만 재시도할 수 있습니다.");
        }
        if (!FAILED_STATUS.equals(latestJob.getProcessingStatus())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "실패한 작업만 재시도할 수 있습니다.");
        }

        List<DocumentChunk> activeChunks = documentChunkRepository
                .findByDocumentVersion_DocumentVersionIdAndChunkStatusOrderByChunkSequenceAsc(
                        documentVersionId,
                        ACTIVE_CHUNK_STATUS);
        if (activeChunks.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "재처리할 청크가 없습니다.");
        }

        Long appUserId = currentUserProvider.getCurrentUser().appUserId();
        DocumentProcessingJob newJob = DocumentProcessingJob.create(documentVersion, appUserId);
        DocumentProcessingJob savedJob = documentProcessingJobRepository.save(newJob);
        savedJob.recordTotalChunkCount(activeChunks.size());
        savedJob.markProcessing(LocalDateTime.now());
        return savedJob;
    }
}
