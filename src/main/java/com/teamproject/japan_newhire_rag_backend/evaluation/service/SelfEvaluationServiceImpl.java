package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.evaluation.Evaluation;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
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
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.MyEvaluationSummaryResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.SelfEvaluationDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.SelfEvaluationItemDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.SelfEvaluationItemResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.SelfEvaluationResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@Service
@Transactional(readOnly = true)
public class SelfEvaluationServiceImpl implements SelfEvaluationService {

    private static final BigDecimal MIN_SCORE = new BigDecimal("1.0");
    private static final BigDecimal MAX_SCORE = new BigDecimal("5.0");

    private final EvaluationRepository evaluationRepository;
    private final EvaluationCycleRepository cycleRepository;
    private final EvaluationItemRepository itemRepository;
    private final EvaluationScoreRepository scoreRepository;
    private final EvaluationFeedbackRepository feedbackRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public SelfEvaluationServiceImpl(
            EvaluationRepository evaluationRepository,
            EvaluationCycleRepository cycleRepository,
            EvaluationItemRepository itemRepository,
            EvaluationScoreRepository scoreRepository,
            EvaluationFeedbackRepository feedbackRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.evaluationRepository = evaluationRepository;
        this.cycleRepository = cycleRepository;
        this.itemRepository = itemRepository;
        this.scoreRepository = scoreRepository;
        this.feedbackRepository = feedbackRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Override
    public List<MyEvaluationSummaryResponse> getMyEvaluations() {
        Long employeeId = currentUserProvider.getCurrentUser().employeeId();
        return evaluationRepository.findByEvaluatorEmployeeIdAndEvaluationType(
                        employeeId, EvaluationType.SELF).stream()
                .map(evaluation -> {
                    EvaluationCycle cycle = findCycle(evaluation.getEvaluationCycleId());
                    return new MyEvaluationSummaryResponse(
                            evaluation.getEvaluationId(),
                            evaluation.getEvaluationCycleId(),
                            cycle.getCycleName(),
                            cycle.getStartDate(),
                            cycle.getEndDate(),
                            evaluation.getEvaluationStatus(),
                            determineStatus(cycle));
                })
                .sorted(Comparator
                        .comparing(MyEvaluationSummaryResponse::cycleStartDate)
                        .reversed()
                        .thenComparing(Comparator.comparing(
                                MyEvaluationSummaryResponse::evaluationId).reversed()))
                .toList();
    }

    @Override
    public SelfEvaluationResponse getMySelfEvaluation(Long evaluationId) {
        Evaluation evaluation = findOwnedSelfEvaluation(evaluationId);
        EvaluationCycle cycle = findCycle(evaluation.getEvaluationCycleId());
        return buildResponse(evaluation, cycle);
    }

    @Override
    @Transactional
    public SelfEvaluationResponse saveDraft(
            Long evaluationId,
            SelfEvaluationDraftRequest request
    ) {
        Evaluation evaluation = findOwnedSelfEvaluation(evaluationId);
        EvaluationCycle cycle = findCycle(evaluation.getEvaluationCycleId());
        requireWritable(evaluation, cycle);
        if (request == null || request.items() == null) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ITEM_MISMATCH);
        }

        List<EvaluationItem> templateItems = itemRepository
                .findByEvaluationTemplateIdOrderByItemOrderAsc(
                        evaluation.getEvaluationTemplateId());
        Map<Long, EvaluationItem> itemById = new HashMap<>();
        for (EvaluationItem item : templateItems) {
            itemById.put(item.getEvaluationItemId(), item);
        }
        validateDraft(request, itemById);

        List<EvaluationFeedback> overallFeedbacks = feedbackRepository
                .findByEvaluationIdAndFeedbackType(evaluationId, FeedbackType.OVERALL);
        if (overallFeedbacks.size() > 1) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_FEEDBACK_CONFLICT);
        }

        for (SelfEvaluationItemDraftRequest itemDraft : request.items()) {
            saveScore(evaluationId, itemDraft);
            saveItemFeedback(evaluationId, itemDraft);
        }
        saveOverallFeedback(evaluationId, request.overallFeedback(), overallFeedbacks);
        evaluation.setLastDraftSavedAt(LocalDateTime.now(clock));

        return buildResponse(evaluation, cycle);
    }

    private Evaluation findOwnedSelfEvaluation(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(
                        EvaluationErrorCode.EVALUATION_NOT_FOUND));
        Long employeeId = currentUserProvider.getCurrentUser().employeeId();
        if (evaluation.getEvaluationType() != EvaluationType.SELF
                || employeeId == null
                || !employeeId.equals(evaluation.getTargetEmployeeId())
                || !employeeId.equals(evaluation.getEvaluatorEmployeeId())) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_OWNER);
        }
        return evaluation;
    }

    private EvaluationCycle findCycle(Long evaluationCycleId) {
        return cycleRepository.findById(evaluationCycleId)
                .orElseThrow(() -> new BusinessException(
                        EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND));
    }

    private void requireWritable(Evaluation evaluation, EvaluationCycle cycle) {
        if (determineStatus(cycle) != EvaluationCycleStatus.OPEN
                || evaluation.getEvaluationStatus() != EvaluationStatus.DRAFT) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_WRITABLE);
        }
    }

    private void validateDraft(
            SelfEvaluationDraftRequest request,
            Map<Long, EvaluationItem> itemById
    ) {
        validateFeedback(request.overallFeedback(), 2000);
        Set<Long> requestedItemIds = new HashSet<>();
        for (SelfEvaluationItemDraftRequest itemDraft : request.items()) {
            if (itemDraft == null
                    || itemDraft.evaluationItemId() == null
                    || !requestedItemIds.add(itemDraft.evaluationItemId())
                    || !itemById.containsKey(itemDraft.evaluationItemId())) {
                throw new BusinessException(EvaluationErrorCode.EVALUATION_ITEM_MISMATCH);
            }
            validateScore(itemDraft.score());
            validateFeedback(itemDraft.itemFeedback(), 1000);
        }
    }

    private void validateScore(BigDecimal score) {
        if (score == null) {
            return;
        }
        int normalizedScale = Math.max(score.stripTrailingZeros().scale(), 0);
        if (normalizedScale > 1
                || score.compareTo(MIN_SCORE) < 0
                || score.compareTo(MAX_SCORE) > 0) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_SCORE_INVALID);
        }
    }

    private void validateFeedback(String content, int maximumLength) {
        if (content != null && content.length() > maximumLength) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_FEEDBACK_INVALID);
        }
    }

    private void saveScore(Long evaluationId, SelfEvaluationItemDraftRequest itemDraft) {
        EvaluationScore existing = scoreRepository
                .findByEvaluationIdAndEvaluationItemId(
                        evaluationId, itemDraft.evaluationItemId())
                .orElse(null);
        if (itemDraft.score() == null) {
            if (existing != null) {
                scoreRepository.delete(existing);
            }
        } else if (existing == null) {
            scoreRepository.save(new EvaluationScore(
                    evaluationId, itemDraft.evaluationItemId(), itemDraft.score()));
        } else {
            existing.setScore(itemDraft.score());
        }
    }

    private void saveItemFeedback(
            Long evaluationId,
            SelfEvaluationItemDraftRequest itemDraft
    ) {
        EvaluationFeedback existing = feedbackRepository
                .findByEvaluationIdAndEvaluationItemIdAndFeedbackType(
                        evaluationId, itemDraft.evaluationItemId(), FeedbackType.ITEM)
                .orElse(null);
        String content = itemDraft.itemFeedback();
        if (isRemoved(content)) {
            if (existing != null) {
                feedbackRepository.delete(existing);
            }
        } else if (existing == null) {
            feedbackRepository.save(new EvaluationFeedback(
                    evaluationId, itemDraft.evaluationItemId(), FeedbackType.ITEM,
                    content, false));
        } else {
            existing.setFeedbackContent(content);
        }
    }

    private void saveOverallFeedback(
            Long evaluationId,
            String content,
            List<EvaluationFeedback> existingFeedbacks
    ) {
        EvaluationFeedback existing = existingFeedbacks.isEmpty()
                ? null : existingFeedbacks.get(0);
        if (isRemoved(content)) {
            if (existing != null) {
                feedbackRepository.delete(existing);
            }
        } else if (existing == null) {
            feedbackRepository.save(new EvaluationFeedback(
                    evaluationId, null, FeedbackType.OVERALL, content, false));
        } else {
            existing.setFeedbackContent(content);
        }
    }

    private boolean isRemoved(String content) {
        return content == null || content.isEmpty();
    }

    private SelfEvaluationResponse buildResponse(
            Evaluation evaluation,
            EvaluationCycle cycle
    ) {
        List<EvaluationItem> items = itemRepository
                .findByEvaluationTemplateIdOrderByItemOrderAsc(
                        evaluation.getEvaluationTemplateId());
        Map<Long, BigDecimal> scores = new HashMap<>();
        for (EvaluationScore score : scoreRepository.findByEvaluationId(
                evaluation.getEvaluationId())) {
            scores.put(score.getEvaluationItemId(), score.getScore());
        }
        Map<Long, String> itemFeedbacks = new HashMap<>();
        for (EvaluationFeedback feedback : feedbackRepository
                .findByEvaluationIdAndFeedbackType(
                        evaluation.getEvaluationId(), FeedbackType.ITEM)) {
            if (feedback.getEvaluationItemId() != null) {
                itemFeedbacks.put(feedback.getEvaluationItemId(), feedback.getFeedbackContent());
            }
        }
        List<EvaluationFeedback> overallFeedbacks = feedbackRepository
                .findByEvaluationIdAndFeedbackType(
                        evaluation.getEvaluationId(), FeedbackType.OVERALL);
        if (overallFeedbacks.size() > 1) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_FEEDBACK_CONFLICT);
        }
        String overallFeedback = overallFeedbacks.isEmpty()
                ? null : overallFeedbacks.get(0).getFeedbackContent();

        List<SelfEvaluationItemResponse> itemResponses = items.stream()
                .map(item -> new SelfEvaluationItemResponse(
                        item.getEvaluationItemId(), item.getItemOrder(), item.getItemName(),
                        item.getItemDescription(), item.getWeight(), item.getIsRequired(),
                        item.getMinimumScore(), item.getMaximumScore(),
                        scores.get(item.getEvaluationItemId()),
                        itemFeedbacks.get(item.getEvaluationItemId())))
                .toList();
        return new SelfEvaluationResponse(
                evaluation.getEvaluationId(), evaluation.getEvaluationCycleId(),
                evaluation.getEvaluationTemplateId(), evaluation.getEvaluationStatus(),
                determineStatus(cycle), itemResponses, overallFeedback);
    }

    private EvaluationCycleStatus determineStatus(EvaluationCycle cycle) {
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(cycle.getStartDate())) {
            return EvaluationCycleStatus.PLANNED;
        }
        if (today.isAfter(cycle.getEndDate())) {
            return EvaluationCycleStatus.CLOSED;
        }
        return EvaluationCycleStatus.OPEN;
    }
}
