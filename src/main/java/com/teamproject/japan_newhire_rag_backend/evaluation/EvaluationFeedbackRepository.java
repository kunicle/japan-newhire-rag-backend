package com.teamproject.japan_newhire_rag_backend.evaluation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationFeedbackRepository extends JpaRepository<EvaluationFeedback, Long> {

    List<EvaluationFeedback> findByEvaluationId(Long evaluationId);

    List<EvaluationFeedback> findByEvaluationIdAndIsVisibleToEmployeeTrue(Long evaluationId);

    Optional<EvaluationFeedback> findByEvaluationIdAndEvaluationItemIdAndFeedbackType(
            Long evaluationId,
            Long evaluationItemId,
            FeedbackType feedbackType);

    List<EvaluationFeedback> findByEvaluationIdAndFeedbackType(
            Long evaluationId,
            FeedbackType feedbackType);
}
