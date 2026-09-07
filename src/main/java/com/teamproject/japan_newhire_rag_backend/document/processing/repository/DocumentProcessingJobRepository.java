package com.teamproject.japan_newhire_rag_backend.document.processing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;

import jakarta.persistence.LockModeType;

public interface DocumentProcessingJobRepository extends JpaRepository<DocumentProcessingJob, Long> {

    List<DocumentProcessingJob> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<DocumentProcessingJob>
            findForUpdateByDocumentVersion_DocumentVersionIdOrderByCreatedAtDescDocumentProcessingJobIdDesc(
                    Long documentVersionId);
}
