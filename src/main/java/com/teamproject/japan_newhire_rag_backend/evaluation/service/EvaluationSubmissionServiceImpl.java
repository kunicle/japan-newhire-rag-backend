package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
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
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@Service
public class EvaluationSubmissionServiceImpl implements EvaluationSubmissionService {

    private static final BigDecimal MIN_SCORE = new BigDecimal("1.0");
    private static final BigDecimal MAX_SCORE = new BigDecimal("5.0");

    private final EvaluationRepository evaluationRepository;
    private final EvaluationCycleRepository cycleRepository;
    private final EvaluationItemRepository itemRepository;
    private final EvaluationScoreRepository scoreRepository;
    private final EvaluationFeedbackRepository feedbackRepository;
    private final OrganizationQueryService organizationQueryService;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public EvaluationSubmissionServiceImpl(
            EvaluationRepository evaluationRepository,
            EvaluationCycleRepository cycleRepository,
            EvaluationItemRepository itemRepository,
            EvaluationScoreRepository scoreRepository,
            EvaluationFeedbackRepository feedbackRepository,
            OrganizationQueryService organizationQueryService,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.evaluationRepository = evaluationRepository;
        this.cycleRepository = cycleRepository;
        this.itemRepository = itemRepository;
        this.scoreRepository = scoreRepository;
        this.feedbackRepository = feedbackRepository;
        this.organizationQueryService = organizationQueryService;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void submitSelf(Long evaluationId) {
        CurrentUserContext user = currentUserProvider.getCurrentUser();
        Evaluation evaluation = findEvaluation(evaluationId);
        if (user.employeeId() == null
                || evaluation.getEvaluationType() != EvaluationType.SELF
                || !user.employeeId().equals(evaluation.getTargetEmployeeId())
                || !evaluation.getTargetEmployeeId().equals(evaluation.getEvaluatorEmployeeId())) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_OWNER);
        }
        validateCommon(evaluation);
        submit(evaluation);
    }

    @Override
    @Transactional
    public void submitManager(Long evaluationId) {
        CurrentUserContext user = currentUserProvider.getCurrentUser();
        Evaluation evaluation = findEvaluation(evaluationId);
        if (user.employeeId() == null
                || !user.roles().contains(RoleType.MANAGER)
                || evaluation.getEvaluationType() != EvaluationType.MANAGER
                || !user.employeeId().equals(evaluation.getEvaluatorEmployeeId())) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_OWNER);
        }
        requireCurrentDirectManager(evaluation, user.employeeId());
        validateCommon(evaluation);
        validateManagerOverallFeedback(evaluation.getEvaluationId());
        submit(evaluation);
    }

    private Evaluation findEvaluation(Long evaluationId) {
        return evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(EvaluationErrorCode.EVALUATION_NOT_FOUND));
    }

    private void requireCurrentDirectManager(Evaluation evaluation, Long employeeId) {
        Long managerEmployeeId;
        try {
            managerEmployeeId = organizationQueryService
                    .findDirectManagerEmployeeId(evaluation.getTargetEmployeeId());
        } catch (IllegalStateException exception) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_MANAGER_RELATION_INVALID);
        }
        if (!employeeId.equals(managerEmployeeId)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_MANAGER_RELATION_INVALID);
        }
    }

    private void validateCommon(Evaluation evaluation) {
        if (evaluation.getEvaluationStatus() != EvaluationStatus.DRAFT) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_WRITABLE);
        }
        EvaluationCycle cycle = cycleRepository.findById(evaluation.getEvaluationCycleId())
                .orElseThrow(() -> new BusinessException(EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND));
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(cycle.getStartDate()) || today.isAfter(cycle.getEndDate())) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_WRITABLE);
        }
        validateScores(evaluation);
    }

    private void validateScores(Evaluation evaluation) {
        List<EvaluationItem> items = itemRepository
                .findByEvaluationTemplateIdOrderByItemOrderAsc(evaluation.getEvaluationTemplateId());
        Map<Long, EvaluationItem> itemsById = new HashMap<>();
        items.forEach(item -> itemsById.put(item.getEvaluationItemId(), item));

        Map<Long, EvaluationScore> scoresByItemId = new HashMap<>();
        for (EvaluationScore score : scoreRepository.findByEvaluationId(evaluation.getEvaluationId())) {
            if (!itemsById.containsKey(score.getEvaluationItemId())) {
                throw new BusinessException(EvaluationErrorCode.EVALUATION_ITEM_MISMATCH);
            }
            validateScore(score.getScore());
            scoresByItemId.put(score.getEvaluationItemId(), score);
        }
        boolean requiredScoreMissing = items.stream()
                .anyMatch(item -> Boolean.TRUE.equals(item.getIsRequired())
                        && !scoresByItemId.containsKey(item.getEvaluationItemId()));
        if (requiredScoreMissing) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_SCORE_INVALID);
        }
    }

    private void validateScore(BigDecimal score) {
        if (score == null
                || Math.max(score.stripTrailingZeros().scale(), 0) > 1
                || score.compareTo(MIN_SCORE) < 0
                || score.compareTo(MAX_SCORE) > 0) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_SCORE_INVALID);
        }
    }

    private void validateManagerOverallFeedback(Long evaluationId) {
        List<EvaluationFeedback> feedbacks = feedbackRepository
                .findByEvaluationIdAndFeedbackType(evaluationId, FeedbackType.OVERALL);
        if (feedbacks.size() > 1) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_FEEDBACK_CONFLICT);
        }
        if (feedbacks.isEmpty()) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_FEEDBACK_INVALID);
        }
        String content = feedbacks.get(0).getFeedbackContent();
        if (content == null || content.isBlank() || content.length() > 2000) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_FEEDBACK_INVALID);
        }
    }

    private void submit(Evaluation evaluation) {
        evaluation.setEvaluationStatus(EvaluationStatus.SUBMITTED);
        evaluation.setSubmittedAt(LocalDateTime.now(clock));
    }
}
