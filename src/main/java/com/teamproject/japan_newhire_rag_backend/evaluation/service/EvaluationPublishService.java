package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishResponse;

public interface EvaluationPublishService {

    EvaluationPublishResponse publish(
            Long evaluationId,
            EvaluationPublishRequest request);
}
