package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.SelfEvaluationDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.SelfEvaluationResponse;

public interface SelfEvaluationService {

    SelfEvaluationResponse getMySelfEvaluation(Long evaluationId);

    SelfEvaluationResponse saveDraft(
            Long evaluationId,
            SelfEvaluationDraftRequest request);
}
