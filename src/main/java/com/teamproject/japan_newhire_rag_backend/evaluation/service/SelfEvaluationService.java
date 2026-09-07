package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.MyEvaluationSummaryResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.SelfEvaluationDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.SelfEvaluationResponse;

public interface SelfEvaluationService {

    List<MyEvaluationSummaryResponse> getMyEvaluations();

    SelfEvaluationResponse getMySelfEvaluation(Long evaluationId);

    SelfEvaluationResponse saveDraft(
            Long evaluationId,
            SelfEvaluationDraftRequest request);
}
