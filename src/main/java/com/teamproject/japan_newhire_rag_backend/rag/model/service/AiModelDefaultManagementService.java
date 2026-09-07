package com.teamproject.japan_newhire_rag_backend.rag.model.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.rag.model.entity.AiModel;
import com.teamproject.japan_newhire_rag_backend.rag.model.repository.AiModelRepository;

@Service
@Transactional
public class AiModelDefaultManagementService {

    private static final String ACTIVE_MODEL_STATUS = "ACTIVE";
    private static final String EMBEDDING_MODEL_TYPE = "EMBEDDING";

    private final AiModelRepository aiModelRepository;

    public AiModelDefaultManagementService(AiModelRepository aiModelRepository) {
        this.aiModelRepository = aiModelRepository;
    }

    public void designateDefaultModel(Long aiModelId) {
        if (aiModelId == null) {
            throw new IllegalArgumentException("AI 모델 ID가 없습니다.");
        }

        AiModel initiallyFoundModel = aiModelRepository.findById(aiModelId)
                .orElseThrow(() -> new IllegalArgumentException("AI 모델이 존재하지 않습니다."));
        List<AiModel> lockedModels = aiModelRepository
                .findForUpdateByModelType(initiallyFoundModel.getModelType());
        AiModel target = lockedModels.stream()
                .filter(model -> Objects.equals(model.getAiModelId(), aiModelId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("AI 모델이 존재하지 않습니다."));

        validateTarget(target);

        for (AiModel model : lockedModels) {
            if (model.isDefault() && !Objects.equals(model.getAiModelId(), aiModelId)) {
                model.clearDefault();
            }
        }
        target.markAsDefault();
    }

    private void validateTarget(AiModel target) {
        if (!ACTIVE_MODEL_STATUS.equals(target.getModelStatus())) {
            throw new IllegalStateException("활성 AI 모델만 기본 모델로 지정할 수 있습니다.");
        }
        if (EMBEDDING_MODEL_TYPE.equals(target.getModelType())) {
            Integer embeddingDimension = target.getEmbeddingDimension();
            if (embeddingDimension == null || embeddingDimension <= 0) {
                throw new IllegalStateException("Embedding 모델 차원이 유효하지 않습니다.");
            }
        }
    }
}
