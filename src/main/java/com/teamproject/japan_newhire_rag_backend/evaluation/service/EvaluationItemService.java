package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationItemCreateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationItemResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationItemUpdateRequest;

public interface EvaluationItemService {

    EvaluationItemResponse create(EvaluationItemCreateRequest request);

    EvaluationItemResponse getById(Long evaluationItemId);

    List<EvaluationItemResponse> getByTemplateId(Long evaluationTemplateId);

    EvaluationItemResponse update(
            Long evaluationItemId,
            EvaluationItemUpdateRequest request);
}
