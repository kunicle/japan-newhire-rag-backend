package com.teamproject.japan_newhire_rag_backend.document.processing.entity;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "document_processing_job")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentProcessingJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_processing_job_id")
    private Long documentProcessingJobId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @Column(name = "job_type", nullable = false, length = 30)
    private String jobType;

    @Column(name = "processing_status", nullable = false, length = 20)
    private String processingStatus;

    @Column(name = "total_chunk_count", nullable = false)
    private int totalChunkCount;

    @Column(name = "completed_chunk_count", nullable = false)
    private int completedChunkCount;

    @Column(name = "failed_chunk_count", nullable = false)
    private int failedChunkCount;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    private DocumentProcessingJob(DocumentVersion documentVersion, Long createdBy) {
        this.documentVersion = documentVersion;
        this.jobType = "FULL_PROCESSING";
        this.processingStatus = "PENDING";
        this.totalChunkCount = 0;
        this.completedChunkCount = 0;
        this.failedChunkCount = 0;
        this.retryCount = 0;
        this.failureReason = null;
        this.startedAt = null;
        this.completedAt = null;
        this.createdBy = createdBy;
    }

    public static DocumentProcessingJob create(DocumentVersion documentVersion, Long createdBy) {
        return new DocumentProcessingJob(documentVersion, createdBy);
    }
}
