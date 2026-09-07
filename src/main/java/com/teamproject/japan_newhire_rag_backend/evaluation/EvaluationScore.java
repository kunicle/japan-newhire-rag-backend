package com.teamproject.japan_newhire_rag_backend.evaluation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "evaluation_score",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_evaluation_score_item",
            columnNames = {"evaluation_id", "evaluation_item_id"}
        )
    },
    indexes = {
        @Index(name = "idx_evaluation_score_evaluation", columnList = "evaluation_id"),
        @Index(name = "idx_evaluation_score_item", columnList = "evaluation_item_id")
    }
)
public class EvaluationScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_score_id")
    private Long evaluationScoreId;

    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Column(name = "evaluation_item_id", nullable = false)
    private Long evaluationItemId;

    @Column(
        name = "score",
        nullable = false,
        precision = 3,
        scale = 1
    )
    private BigDecimal score;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected EvaluationScore() {
    }

    public EvaluationScore(Long evaluationId, Long evaluationItemId, BigDecimal score) {
        this.evaluationId = evaluationId;
        this.evaluationItemId = evaluationItemId;
        this.score = score;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getEvaluationScoreId() {
        return evaluationScoreId;
    }

    public Long getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Long evaluationId) {
        this.evaluationId = evaluationId;
    }

    public Long getEvaluationItemId() {
        return evaluationItemId;
    }

    public void setEvaluationItemId(Long evaluationItemId) {
        this.evaluationItemId = evaluationItemId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
