package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationSummaryResponse;

public interface ManagerEvaluationService {

    List<ManagerEvaluationSummaryResponse> getMyAssignedEvaluations();

    ManagerEvaluationResponse getMyManagerEvaluation(Long evaluationId);

    ManagerEvaluationResponse saveDraft(
            Long evaluationId,
            ManagerEvaluationDraftRequest request);
}
