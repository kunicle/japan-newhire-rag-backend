package com.teamproject.japan_newhire_rag_backend.evaluation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationTemplateRepository extends JpaRepository<EvaluationTemplate, Long> {

    boolean existsByEvaluationCycleIdAndEvaluationType(
            Long evaluationCycleId,
            EvaluationType evaluationType);

    List<EvaluationTemplate> findByEvaluationCycleIdOrderByEvaluationTypeAsc(
            Long evaluationCycleId);
}
