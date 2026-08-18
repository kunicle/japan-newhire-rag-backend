package com.teamproject.japan_newhire_rag_backend.evaluation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    boolean existsByEvaluationTemplateId(Long evaluationTemplateId);

    boolean existsByEvaluationCycleIdAndTargetEmployeeIdAndEvaluationType(
            Long evaluationCycleId,
            Long targetEmployeeId,
            EvaluationType evaluationType);

    List<Evaluation> findByEvaluatorEmployeeIdAndEvaluationType(
            Long evaluatorEmployeeId,
            EvaluationType evaluationType);

    List<Evaluation> findByEvaluationCycleId(Long evaluationCycleId);

    List<Evaluation> findByEvaluationCycleIdAndTargetEmployeeId(
            Long evaluationCycleId,
            Long targetEmployeeId);

    List<Evaluation> findByEvaluationCycleIdAndTargetEmployeeIdAndEvaluationTypeIn(
            Long evaluationCycleId,
            Long targetEmployeeId,
            List<EvaluationType> evaluationTypes);
}
