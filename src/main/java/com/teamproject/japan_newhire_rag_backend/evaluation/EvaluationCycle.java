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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "evaluation_cycle",
    indexes = {
        @Index(name = "idx_evaluation_cycle_status_dates", columnList = "cycle_status, start_date, end_date"),
        @Index(name = "idx_evaluation_cycle_publish", columnList = "planned_publish_date, cycle_status")
    }
)
public class EvaluationCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_cycle_id")
    private Long evaluationCycleId;

    @Column(name = "cycle_name", nullable = false, length = 100)
    private String cycleName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "planned_publish_date", nullable = false)
    private LocalDate plannedPublishDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle_status", nullable = false, length = 20)
    private EvaluationCycleStatus cycleStatus = EvaluationCycleStatus.PLANNED;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected EvaluationCycle() {
    }

    public EvaluationCycle(String cycleName, LocalDate startDate, LocalDate endDate,
                          LocalDate plannedPublishDate, EvaluationCycleStatus cycleStatus,
                          Long createdBy) {
        this.cycleName = cycleName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.plannedPublishDate = plannedPublishDate;
        this.cycleStatus = cycleStatus != null ? cycleStatus : EvaluationCycleStatus.PLANNED;
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

    public Long getEvaluationCycleId() {
        return evaluationCycleId;
    }

    public String getCycleName() {
        return cycleName;
    }

    public void setCycleName(String cycleName) {
        this.cycleName = cycleName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getPlannedPublishDate() {
        return plannedPublishDate;
    }

    public void setPlannedPublishDate(LocalDate plannedPublishDate) {
        this.plannedPublishDate = plannedPublishDate;
    }

    public EvaluationCycleStatus getCycleStatus() {
        return cycleStatus;
    }

    public void setCycleStatus(EvaluationCycleStatus cycleStatus) {
        this.cycleStatus = cycleStatus != null ? cycleStatus : EvaluationCycleStatus.PLANNED;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
