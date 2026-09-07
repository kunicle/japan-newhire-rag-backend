package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationCycleCreateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationCycleResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationCycleUpdateRequest;

public interface EvaluationCycleService {

    EvaluationCycleResponse create(EvaluationCycleCreateRequest request);

    EvaluationCycleResponse getById(Long evaluationCycleId);

    List<EvaluationCycleResponse> getCycles();

    EvaluationCycleResponse update(
            Long evaluationCycleId,
            EvaluationCycleUpdateRequest request
    );

    EvaluationCycleStatus getCurrentStatus(Long evaluationCycleId);
}
