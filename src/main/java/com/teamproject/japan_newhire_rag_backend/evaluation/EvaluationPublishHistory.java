package com.teamproject.japan_newhire_rag_backend.evaluation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "evaluation_publish_history",
    indexes = {
        @Index(
            name = "idx_evaluation_publish_eval",
            columnList = "evaluation_id, published_at"
        ),
        @Index(
            name = "idx_evaluation_publish_user",
            columnList = "published_by, published_at"
        )
    }
)
public class EvaluationPublishHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_publish_history_id")
    private Long evaluationPublishHistoryId;

    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Column(name = "published_by", nullable = false)
    private Long publishedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 20)
    private EvaluationStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "published_status", nullable = false, length = 20)
    private EvaluationStatus publishedStatus = EvaluationStatus.PUBLISHED;

    @Column(name = "publish_reason", length = 500)
    private String publishReason;

    @Column(name = "published_at", nullable = false, updatable = false)
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected EvaluationPublishHistory() {
    }

    public EvaluationPublishHistory(Long evaluationId, Long publishedBy,
                                    EvaluationStatus previousStatus,
                                    EvaluationStatus publishedStatus,
                                    String publishReason) {
        this(evaluationId, publishedBy, previousStatus, publishedStatus,
                publishReason, null);
    }

    public EvaluationPublishHistory(Long evaluationId, Long publishedBy,
                                    EvaluationStatus previousStatus,
                                    EvaluationStatus publishedStatus,
                                    String publishReason,
                                    LocalDateTime publishedAt) {
        this.evaluationId = evaluationId;
        this.publishedBy = publishedBy;
        this.previousStatus = previousStatus;
        this.publishedStatus = publishedStatus != null
            ? publishedStatus
            : EvaluationStatus.PUBLISHED;
        this.publishReason = publishReason;
        this.publishedAt = publishedAt;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.publishedAt == null) {
            this.publishedAt = now;
        }
        this.createdAt = now;
    }

    public Long getEvaluationPublishHistoryId() {
        return evaluationPublishHistoryId;
    }

    public Long getEvaluationId() {
        return evaluationId;
    }

    public Long getPublishedBy() {
        return publishedBy;
    }

    public EvaluationStatus getPreviousStatus() {
        return previousStatus;
    }

    public EvaluationStatus getPublishedStatus() {
        return publishedStatus;
    }

    public String getPublishReason() {
        return publishReason;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
