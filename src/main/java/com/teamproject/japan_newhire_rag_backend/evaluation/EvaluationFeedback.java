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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "evaluation_feedback",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_evaluation_feedback_item",
            columnNames = {"evaluation_id", "evaluation_item_id", "feedback_type"}
        )
    },
    indexes = {
        @Index(
            name = "idx_evaluation_feedback_visible",
            columnList = "evaluation_id, is_visible_to_employee"
        )
    }
)
public class EvaluationFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_feedback_id")
    private Long evaluationFeedbackId;

    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Column(name = "evaluation_item_id")
    private Long evaluationItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 20)
    private FeedbackType feedbackType;

    @Column(name = "feedback_content", nullable = false, length = 2000)
    private String feedbackContent;

    @Column(name = "is_visible_to_employee", nullable = false)
    private Boolean isVisibleToEmployee = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected EvaluationFeedback() {
    }

    public EvaluationFeedback(Long evaluationId, Long evaluationItemId,
                              FeedbackType feedbackType, String feedbackContent,
                              Boolean isVisibleToEmployee) {
        this.evaluationId = evaluationId;
        this.evaluationItemId = evaluationItemId;
        this.feedbackType = feedbackType;
        this.feedbackContent = feedbackContent;
        this.isVisibleToEmployee = isVisibleToEmployee != null ? isVisibleToEmployee : false;
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

    public Long getEvaluationFeedbackId() {
        return evaluationFeedbackId;
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

    public FeedbackType getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(FeedbackType feedbackType) {
        this.feedbackType = feedbackType;
    }

    public String getFeedbackContent() {
        return feedbackContent;
    }

    public void setFeedbackContent(String feedbackContent) {
        this.feedbackContent = feedbackContent;
    }

    public Boolean getIsVisibleToEmployee() {
        return isVisibleToEmployee;
    }

    public void setIsVisibleToEmployee(Boolean isVisibleToEmployee) {
        this.isVisibleToEmployee = isVisibleToEmployee != null ? isVisibleToEmployee : false;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
