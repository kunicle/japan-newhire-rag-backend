package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationTemplateCreateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationTemplateResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationTemplateUpdateRequest;

public interface EvaluationTemplateService {

    EvaluationTemplateResponse create(EvaluationTemplateCreateRequest request);

    EvaluationTemplateResponse getById(Long evaluationTemplateId);

    List<EvaluationTemplateResponse> getByCycleId(Long evaluationCycleId);

    EvaluationTemplateResponse update(
            Long evaluationTemplateId,
            EvaluationTemplateUpdateRequest request);
}
