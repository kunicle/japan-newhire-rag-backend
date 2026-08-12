package com.teamproject.japan_newhire_rag_backend.document.processing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;

public interface DocumentProcessingJobRepository extends JpaRepository<DocumentProcessingJob, Long> {
}
