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
    name = "evaluation_item",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_evaluation_item_order", columnNames = {"evaluation_template_id", "item_order"})
    },
    indexes = {
        @Index(name = "idx_evaluation_item_template", columnList = "evaluation_template_id, item_order")
    }
)
public class EvaluationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_item_id")
    private Long evaluationItemId;

    @Column(name = "evaluation_template_id", nullable = false)
    private Long evaluationTemplateId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "item_description", length = 1000)
    private String itemDescription;

    @Column(name = "item_order", nullable = false)
    private Integer itemOrder;

    @Column(name = "weight", nullable = false, precision = 7, scale = 2)
    private BigDecimal weight;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = true;

    @Column(name = "minimum_score", nullable = false)
    private Integer minimumScore = 1;

    @Column(name = "maximum_score", nullable = false)
    private Integer maximumScore = 5;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected EvaluationItem() {
    }

    public EvaluationItem(Long evaluationTemplateId, String itemName, String itemDescription,
                         Integer itemOrder, BigDecimal weight, Boolean isRequired,
                         Integer minimumScore, Integer maximumScore) {
        this.evaluationTemplateId = evaluationTemplateId;
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.itemOrder = itemOrder;
        this.weight = weight;
        this.isRequired = isRequired != null ? isRequired : true;
        this.minimumScore = minimumScore != null ? minimumScore : 1;
        this.maximumScore = maximumScore != null ? maximumScore : 5;
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

    public Long getEvaluationItemId() {
        return evaluationItemId;
    }

    public Long getEvaluationTemplateId() {
        return evaluationTemplateId;
    }

    public void setEvaluationTemplateId(Long evaluationTemplateId) {
        this.evaluationTemplateId = evaluationTemplateId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public Integer getItemOrder() {
        return itemOrder;
    }

    public void setItemOrder(Integer itemOrder) {
        this.itemOrder = itemOrder;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired != null ? isRequired : true;
    }

    public Integer getMinimumScore() {
        return minimumScore;
    }

    public void setMinimumScore(Integer minimumScore) {
        this.minimumScore = minimumScore != null ? minimumScore : 1;
    }

    public Integer getMaximumScore() {
        return maximumScore;
    }

    public void setMaximumScore(Integer maximumScore) {
        this.maximumScore = maximumScore != null ? maximumScore : 5;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
