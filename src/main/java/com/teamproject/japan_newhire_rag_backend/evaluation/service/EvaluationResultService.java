package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationResultResponse;

public interface EvaluationResultService {

    EvaluationResultResponse getMyResult(Long evaluationCycleId);
}
