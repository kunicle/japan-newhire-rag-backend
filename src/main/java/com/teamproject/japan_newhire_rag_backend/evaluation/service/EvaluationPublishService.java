package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishPreviewResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishResponse;

public interface EvaluationPublishService {

    EvaluationPublishPreviewResponse getPublishPreview(Long evaluationId);

    EvaluationPublishResponse publish(
            Long evaluationId,
            EvaluationPublishRequest request);
}
