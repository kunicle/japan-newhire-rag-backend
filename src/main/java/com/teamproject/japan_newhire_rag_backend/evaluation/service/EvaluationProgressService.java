package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationProgressResponse;

public interface EvaluationProgressService {

    EvaluationProgressResponse getCycleProgress(Long evaluationCycleId);
}
