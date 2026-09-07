package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.evaluation.Evaluation;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationFeedback;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationFeedbackRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationItem;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationItemRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationScore;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationScoreRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;
import com.teamproject.japan_newhire_rag_backend.evaluation.FeedbackType;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationResultCycleResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationResultDetailResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationResultItemResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationResultResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@Service
@Transactional(readOnly = true)
public class EvaluationResultServiceImpl implements EvaluationResultService {

    private static final List<EvaluationType> RESULT_TYPES =
            List.of(EvaluationType.SELF, EvaluationType.MANAGER);

    private final EvaluationRepository evaluationRepository;
    private final EvaluationCycleRepository cycleRepository;
    private final EvaluationItemRepository itemRepository;
    private final EvaluationScoreRepository scoreRepository;
    private final EvaluationFeedbackRepository feedbackRepository;
    private final CurrentUserProvider currentUserProvider;

    public EvaluationResultServiceImpl(
            EvaluationRepository evaluationRepository,
            EvaluationCycleRepository cycleRepository,
            EvaluationItemRepository itemRepository,
            EvaluationScoreRepository scoreRepository,
            EvaluationFeedbackRepository feedbackRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.evaluationRepository = evaluationRepository;
        this.cycleRepository = cycleRepository;
        this.itemRepository = itemRepository;
        this.scoreRepository = scoreRepository;
        this.feedbackRepository = feedbackRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public EvaluationResultResponse getMyResult(Long evaluationCycleId) {
        CurrentUserContext user = currentUserProvider.getCurrentUser();
        if (user.employeeId() == null) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_OWNER);
        }
        EvaluationCycle cycle = cycleRepository.findById(evaluationCycleId)
                .orElseThrow(() -> new BusinessException(
                        EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND));
        EvaluationSet evaluations = findEvaluationSet(evaluationCycleId, user.employeeId());
        requireOwner(evaluations, user.employeeId());
        requirePublished(evaluations);

        return new EvaluationResultResponse(
                cycleResponse(cycle),
                detailResponse(evaluations.self),
                detailResponse(evaluations.manager));
    }

    private EvaluationSet findEvaluationSet(Long cycleId, Long employeeId) {
        List<Evaluation> evaluations = evaluationRepository
                .findByEvaluationCycleIdAndTargetEmployeeIdAndEvaluationTypeIn(
                        cycleId, employeeId, RESULT_TYPES);
        Evaluation self = null;
        Evaluation manager = null;
        for (Evaluation evaluation : evaluations) {
            if (evaluation.getEvaluationType() == EvaluationType.SELF) {
                if (self != null) throw publishConflict();
                self = evaluation;
            } else if (evaluation.getEvaluationType() == EvaluationType.MANAGER) {
                if (manager != null) throw publishConflict();
                manager = evaluation;
            }
        }
        if (self == null || manager == null) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_FOUND);
        }
        return new EvaluationSet(self, manager);
    }

    private void requireOwner(EvaluationSet evaluations, Long employeeId) {
        if (!employeeId.equals(evaluations.self.getTargetEmployeeId())
                || !employeeId.equals(evaluations.manager.getTargetEmployeeId())) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_OWNER);
        }
    }

    private void requirePublished(EvaluationSet evaluations) {
        boolean selfPublished = evaluations.self.getEvaluationStatus() == EvaluationStatus.PUBLISHED;
        boolean managerPublished = evaluations.manager.getEvaluationStatus() == EvaluationStatus.PUBLISHED;
        if (selfPublished != managerPublished) {
            throw publishConflict();
        }
        if (!selfPublished) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_RESULT_NOT_AVAILABLE);
        }
    }

    private EvaluationResultCycleResponse cycleResponse(EvaluationCycle cycle) {
        return new EvaluationResultCycleResponse(
                cycle.getEvaluationCycleId(), cycle.getCycleName(), cycle.getStartDate(),
                cycle.getEndDate(), cycle.getPlannedPublishDate());
    }

    private EvaluationResultDetailResponse detailResponse(Evaluation evaluation) {
        List<EvaluationItem> items = itemRepository
                .findByEvaluationTemplateIdOrderByItemOrderAsc(
                        evaluation.getEvaluationTemplateId());
        Map<Long, BigDecimal> scores = scoresByItem(evaluation.getEvaluationId());
        VisibleFeedback visibleFeedback = visibleFeedback(evaluation.getEvaluationId());
        List<EvaluationResultItemResponse> itemResponses = items.stream()
                .map(item -> new EvaluationResultItemResponse(
                        item.getEvaluationItemId(), item.getItemOrder(), item.getItemName(),
                        item.getItemDescription(), item.getWeight(), item.getIsRequired(),
                        scores.get(item.getEvaluationItemId()),
                        visibleFeedback.itemFeedbacks.get(item.getEvaluationItemId())))
                .toList();
        return new EvaluationResultDetailResponse(
                evaluation.getEvaluationId(), evaluation.getEvaluationStatus(),
                evaluation.getTotalScore(), itemResponses, visibleFeedback.overallFeedback);
    }

    private Map<Long, BigDecimal> scoresByItem(Long evaluationId) {
        Map<Long, BigDecimal> scores = new HashMap<>();
        for (EvaluationScore score : scoreRepository.findByEvaluationId(evaluationId)) {
            scores.put(score.getEvaluationItemId(), score.getScore());
        }
        return scores;
    }

    private VisibleFeedback visibleFeedback(Long evaluationId) {
        Map<Long, String> itemFeedbacks = new HashMap<>();
        String overallFeedback = null;
        for (EvaluationFeedback feedback : feedbackRepository
                .findByEvaluationIdAndIsVisibleToEmployeeTrue(evaluationId)) {
            if (feedback.getFeedbackType() == FeedbackType.OVERALL) {
                if (overallFeedback != null) throw feedbackConflict();
                overallFeedback = feedback.getFeedbackContent();
            } else if (feedback.getFeedbackType() == FeedbackType.ITEM
                    && feedback.getEvaluationItemId() != null) {
                if (itemFeedbacks.putIfAbsent(feedback.getEvaluationItemId(),
                        feedback.getFeedbackContent()) != null) {
                    throw feedbackConflict();
                }
            }
        }
        return new VisibleFeedback(itemFeedbacks, overallFeedback);
    }

    private BusinessException publishConflict() {
        return new BusinessException(EvaluationErrorCode.EVALUATION_PUBLISH_CONFLICT);
    }

    private BusinessException feedbackConflict() {
        return new BusinessException(EvaluationErrorCode.EVALUATION_FEEDBACK_CONFLICT);
    }

    private record EvaluationSet(Evaluation self, Evaluation manager) {
    }

    private record VisibleFeedback(Map<Long, String> itemFeedbacks, String overallFeedback) {
    }
}
