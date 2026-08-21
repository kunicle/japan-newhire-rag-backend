package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationAssignmentRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationAssignmentResponse;

public interface EvaluationAssignmentService {

    EvaluationAssignmentResponse assign(EvaluationAssignmentRequest request);
}
