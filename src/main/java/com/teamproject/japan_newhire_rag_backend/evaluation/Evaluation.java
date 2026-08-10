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
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "evaluation",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_evaluation_assignment",
            columnNames = {"evaluation_cycle_id", "target_employee_id", "evaluator_employee_id", "evaluation_type"}
        )
    },
    indexes = {
        @Index(name = "idx_evaluation_target", columnList = "target_employee_id, evaluation_cycle_id, evaluation_status"),
        @Index(name = "idx_evaluation_evaluator", columnList = "evaluator_employee_id, evaluation_cycle_id, evaluation_status"),
        @Index(name = "idx_evaluation_cycle_status", columnList = "evaluation_cycle_id, evaluation_status"),
        @Index(name = "idx_evaluation_published", columnList = "target_employee_id, published_at")
    }
)
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id")
    private Long evaluationId;

    @Column(name = "evaluation_cycle_id", nullable = false)
    private Long evaluationCycleId;

    @Column(name = "evaluation_template_id", nullable = false)
    private Long evaluationTemplateId;

    @Column(name = "target_employee_id", nullable = false)
    private Long targetEmployeeId;

    @Column(name = "evaluator_employee_id", nullable = false)
    private Long evaluatorEmployeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_type", nullable = false, length = 20)
    private EvaluationType evaluationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", nullable = false, length = 20)
    private EvaluationStatus evaluationStatus = EvaluationStatus.DRAFT;

    @Column(name = "total_score", precision = 7, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Evaluation() {
    }

    public Evaluation(Long evaluationCycleId, Long evaluationTemplateId,
                     Long targetEmployeeId, Long evaluatorEmployeeId,
                     EvaluationType evaluationType, EvaluationStatus evaluationStatus,
                     BigDecimal totalScore, LocalDateTime submittedAt, LocalDateTime publishedAt) {
        this.evaluationCycleId = evaluationCycleId;
        this.evaluationTemplateId = evaluationTemplateId;
        this.targetEmployeeId = targetEmployeeId;
        this.evaluatorEmployeeId = evaluatorEmployeeId;
        this.evaluationType = evaluationType;
        this.evaluationStatus = evaluationStatus != null ? evaluationStatus : EvaluationStatus.DRAFT;
        this.totalScore = totalScore;
        this.submittedAt = submittedAt;
        this.publishedAt = publishedAt;
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

    public Long getEvaluationId() {
        return evaluationId;
    }

    public Long getEvaluationCycleId() {
        return evaluationCycleId;
    }

    public void setEvaluationCycleId(Long evaluationCycleId) {
        this.evaluationCycleId = evaluationCycleId;
    }

    public Long getEvaluationTemplateId() {
        return evaluationTemplateId;
    }

    public void setEvaluationTemplateId(Long evaluationTemplateId) {
        this.evaluationTemplateId = evaluationTemplateId;
    }

    public Long getTargetEmployeeId() {
        return targetEmployeeId;
    }

    public void setTargetEmployeeId(Long targetEmployeeId) {
        this.targetEmployeeId = targetEmployeeId;
    }

    public Long getEvaluatorEmployeeId() {
        return evaluatorEmployeeId;
    }

    public void setEvaluatorEmployeeId(Long evaluatorEmployeeId) {
        this.evaluatorEmployeeId = evaluatorEmployeeId;
    }

    public EvaluationType getEvaluationType() {
        return evaluationType;
    }

    public void setEvaluationType(EvaluationType evaluationType) {
        this.evaluationType = evaluationType;
    }

    public EvaluationStatus getEvaluationStatus() {
        return evaluationStatus;
    }

    public void setEvaluationStatus(EvaluationStatus evaluationStatus) {
        this.evaluationStatus = evaluationStatus != null ? evaluationStatus : EvaluationStatus.DRAFT;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
