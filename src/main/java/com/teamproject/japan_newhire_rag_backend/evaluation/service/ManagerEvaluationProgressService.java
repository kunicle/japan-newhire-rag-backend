package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationProgressResponse;

public interface ManagerEvaluationProgressService {

    ManagerEvaluationProgressResponse getManagedProgress(Long evaluationCycleId);
}
