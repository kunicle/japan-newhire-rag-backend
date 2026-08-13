package com.teamproject.japan_newhire_rag_backend.evaluation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationItemRepository extends JpaRepository<EvaluationItem, Long> {

    boolean existsByEvaluationTemplateIdAndItemOrder(
            Long evaluationTemplateId,
            Integer itemOrder);

    boolean existsByEvaluationTemplateIdAndItemOrderAndEvaluationItemIdNot(
            Long evaluationTemplateId,
            Integer itemOrder,
            Long evaluationItemId);

    List<EvaluationItem> findByEvaluationTemplateIdOrderByItemOrderAsc(
            Long evaluationTemplateId);

    boolean existsByEvaluationTemplateId(Long evaluationTemplateId);
}
