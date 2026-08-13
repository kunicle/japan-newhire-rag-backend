package com.teamproject.japan_newhire_rag_backend.evaluation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationScoreRepository extends JpaRepository<EvaluationScore, Long> {

    Optional<EvaluationScore> findByEvaluationIdAndEvaluationItemId(
            Long evaluationId,
            Long evaluationItemId);

    List<EvaluationScore> findByEvaluationId(Long evaluationId);
}
