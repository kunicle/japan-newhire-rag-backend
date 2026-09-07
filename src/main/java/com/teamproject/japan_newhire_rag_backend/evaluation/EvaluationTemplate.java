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
    name = "evaluation_template",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_evaluation_template", columnNames = {"evaluation_cycle_id", "evaluation_type"})
    },
    indexes = {
        @Index(name = "idx_evaluation_template_cycle", columnList = "evaluation_cycle_id, is_active")
    }
)
public class EvaluationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_template_id")
    private Long evaluationTemplateId;

    @Column(name = "evaluation_cycle_id", nullable = false)
    private Long evaluationCycleId;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_type", nullable = false, length = 20)
    private EvaluationType evaluationType;

    @Column(name = "template_description", length = 1000)
    private String templateDescription;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected EvaluationTemplate() {
    }

    public EvaluationTemplate(Long evaluationCycleId, String templateName,
                             EvaluationType evaluationType, String templateDescription,
                             Boolean isActive, Long createdBy) {
        this.evaluationCycleId = evaluationCycleId;
        this.templateName = templateName;
        this.evaluationType = evaluationType;
        this.templateDescription = templateDescription;
        this.isActive = isActive != null ? isActive : true;
        this.createdBy = createdBy;
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

    public Long getEvaluationTemplateId() {
        return evaluationTemplateId;
    }

    public Long getEvaluationCycleId() {
        return evaluationCycleId;
    }

    public void setEvaluationCycleId(Long evaluationCycleId) {
        this.evaluationCycleId = evaluationCycleId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public EvaluationType getEvaluationType() {
        return evaluationType;
    }

    public void setEvaluationType(EvaluationType evaluationType) {
        this.evaluationType = evaluationType;
    }

    public String getTemplateDescription() {
        return templateDescription;
    }

    public void setTemplateDescription(String templateDescription) {
        this.templateDescription = templateDescription;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive != null ? isActive : true;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
