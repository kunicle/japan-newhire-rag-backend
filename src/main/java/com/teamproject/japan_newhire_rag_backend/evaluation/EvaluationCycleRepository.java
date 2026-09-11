package com.teamproject.japan_newhire_rag_backend.evaluation;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationCycleRepository extends JpaRepository<EvaluationCycle, Long> {

    List<EvaluationCycle> findAllByDeletedAtIsNullOrderByStartDateDescEvaluationCycleIdDesc();

    List<EvaluationCycle> findAllByStartDateAndDeletedAtIsNull(LocalDate startDate);
}
