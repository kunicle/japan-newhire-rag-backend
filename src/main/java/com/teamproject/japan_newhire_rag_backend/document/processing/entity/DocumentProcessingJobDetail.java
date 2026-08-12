package com.teamproject.japan_newhire_rag_backend.document.processing.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "document_processing_job_detail")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentProcessingJobDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "processing_job_detail_id")
    private Long processingJobDetailId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_processing_job_id", nullable = false)
    private DocumentProcessingJob documentProcessingJob;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "document_chunk_id", nullable = true)
    private DocumentChunk documentChunk;

    @Column(name = "processing_step", nullable = false, length = 30)
    private String processingStep;

    @Column(name = "processing_status", nullable = false, length = 20)
    private String processingStatus;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private DocumentProcessingJobDetail(
            DocumentProcessingJob documentProcessingJob,
            DocumentChunk documentChunk,
            String processingStep) {
        this.documentProcessingJob = documentProcessingJob;
        this.documentChunk = documentChunk;
        this.processingStep = processingStep;
        this.processingStatus = "PENDING";
        this.attemptNumber = 1;
        this.failureReason = null;
        this.processedAt = null;
    }

    public static DocumentProcessingJobDetail create(
            DocumentProcessingJob documentProcessingJob,
            DocumentChunk documentChunk,
            String processingStep) {
        return new DocumentProcessingJobDetail(
                documentProcessingJob,
                documentChunk,
                processingStep);
    }
}
