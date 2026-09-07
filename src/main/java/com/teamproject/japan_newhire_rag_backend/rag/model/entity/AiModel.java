package com.teamproject.japan_newhire_rag_backend.rag.model.entity;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_model")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiModel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_model_id")
    private Long aiModelId;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "model_name", nullable = false, length = 150)
    private String modelName;

    @Column(name = "model_type", nullable = false, length = 20)
    private String modelType;

    @Column(name = "embedding_dimension")
    private Integer embeddingDimension;

    @Column(name = "model_status", nullable = false, length = 20)
    private String modelStatus;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    private AiModel(
            String providerName,
            String modelName,
            String modelType,
            Integer embeddingDimension) {
        this.providerName = providerName;
        this.modelName = modelName;
        this.modelType = modelType;
        this.embeddingDimension = embeddingDimension;
        this.modelStatus = "ACTIVE";
        this.isDefault = false;
    }

    public static AiModel create(
            String providerName,
            String modelName,
            String modelType,
            Integer embeddingDimension) {
        return new AiModel(providerName, modelName, modelType, embeddingDimension);
    }

    public void markAsDefault() {
        this.isDefault = true;
    }

    public void clearDefault() {
        this.isDefault = false;
    }
}
