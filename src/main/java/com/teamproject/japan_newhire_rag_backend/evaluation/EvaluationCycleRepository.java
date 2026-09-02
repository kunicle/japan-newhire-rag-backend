package com.teamproject.japan_newhire_rag_backend.evaluation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationCycleRepository extends JpaRepository<EvaluationCycle, Long> {

    List<EvaluationCycle> findAllByDeletedAtIsNullOrderByStartDateDescEvaluationCycleIdDesc();
}
