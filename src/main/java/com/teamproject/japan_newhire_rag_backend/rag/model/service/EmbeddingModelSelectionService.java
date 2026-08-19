package com.teamproject.japan_newhire_rag_backend.rag.model.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.rag.model.EmbeddingModelSelection;
import com.teamproject.japan_newhire_rag_backend.rag.model.entity.AiModel;
import com.teamproject.japan_newhire_rag_backend.rag.model.repository.AiModelRepository;

@Service
@Transactional(readOnly = true)
public class EmbeddingModelSelectionService {

    private static final String EMBEDDING_MODEL_TYPE = "EMBEDDING";
    private static final String ACTIVE_MODEL_STATUS = "ACTIVE";

    private final AiModelRepository aiModelRepository;

    public EmbeddingModelSelectionService(AiModelRepository aiModelRepository) {
        this.aiModelRepository = aiModelRepository;
    }

    public EmbeddingModelSelection selectDefaultEmbeddingModel() {
        List<AiModel> models = aiModelRepository
                .findByModelTypeAndModelStatusAndIsDefaultTrue(
                        EMBEDDING_MODEL_TYPE,
                        ACTIVE_MODEL_STATUS);
        if (models.size() != 1) {
            throw new IllegalStateException("default embedding model must be exactly one");
        }

        AiModel model = models.get(0);
        return new EmbeddingModelSelection(
                model.getAiModelId(),
                model.getProviderName(),
                model.getModelName(),
                model.getEmbeddingDimension());
    }
}
