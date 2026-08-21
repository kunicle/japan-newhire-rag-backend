package com.teamproject.japan_newhire_rag_backend.evaluation.service;

public interface EvaluationSubmissionService {

    void submitSelf(Long evaluationId);

    void submitManager(Long evaluationId);
}
