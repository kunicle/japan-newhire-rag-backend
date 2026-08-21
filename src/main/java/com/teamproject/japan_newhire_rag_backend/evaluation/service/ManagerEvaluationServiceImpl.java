package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.EmployeeSummary;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
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
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationItemDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationItemResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationSummaryResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@Service
@Transactional(readOnly = true)
public class ManagerEvaluationServiceImpl implements ManagerEvaluationService {

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

    public ManagerEvaluationServiceImpl(
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
    public List<ManagerEvaluationSummaryResponse> getMyAssignedEvaluations() {
        CurrentUserContext user = requireManagerRole();
        List<Evaluation> evaluations = evaluationRepository
                .findByEvaluatorEmployeeIdAndEvaluationType(
                        user.employeeId(), EvaluationType.MANAGER);
        for (Evaluation evaluation : evaluations) {
            requireCurrentDirectManager(evaluation, user.employeeId());
        }
        Map<Long, EmployeeSummary> employees = employeeSummaries(
                evaluations.stream().map(Evaluation::getTargetEmployeeId).toList());
        return evaluations.stream().map(evaluation -> {
            EvaluationCycle cycle = findCycle(evaluation.getEvaluationCycleId());
            return new ManagerEvaluationSummaryResponse(
                    evaluation.getEvaluationId(), evaluation.getEvaluationCycleId(),
                    evaluation.getEvaluationStatus(), determineStatus(cycle),
                    requireEmployeeSummary(employees, evaluation.getTargetEmployeeId()));
        }).toList();
    }

    @Override
    public ManagerEvaluationResponse getMyManagerEvaluation(Long evaluationId) {
        CurrentUserContext user = requireManagerRole();
        Evaluation evaluation = findOwnedManagerEvaluation(evaluationId, user.employeeId());
        requireCurrentDirectManager(evaluation, user.employeeId());
        EvaluationCycle cycle = findCycle(evaluation.getEvaluationCycleId());
        requireWritable(evaluation, cycle);
        return buildResponse(evaluation, cycle);
    }

    @Override
    @Transactional
    public ManagerEvaluationResponse saveDraft(
            Long evaluationId,
            ManagerEvaluationDraftRequest request
    ) {
        CurrentUserContext user = requireManagerRole();
        Evaluation evaluation = findOwnedManagerEvaluation(evaluationId, user.employeeId());
        requireCurrentDirectManager(evaluation, user.employeeId());
        EvaluationCycle cycle = findCycle(evaluation.getEvaluationCycleId());
        requireWritable(evaluation, cycle);
        if (request == null || request.items() == null) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ITEM_MISMATCH);
        }
        Map<Long, EvaluationItem> items = templateItems(evaluation.getEvaluationTemplateId());
        validate(request, items);
        List<EvaluationFeedback> overall = overallFeedbacks(evaluationId);
        for (ManagerEvaluationItemDraftRequest draft : request.items()) {
            saveScore(evaluationId, draft);
            saveItemFeedback(evaluationId, draft);
        }
        saveOverallFeedback(evaluationId, request.overallFeedback(), overall);
        evaluation.setLastDraftSavedAt(LocalDateTime.now(clock));
        return buildResponse(evaluation, cycle);
    }

    private CurrentUserContext requireManagerRole() {
        CurrentUserContext user = currentUserProvider.getCurrentUser();
        if (!user.roles().contains(RoleType.MANAGER) || user.employeeId() == null) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_OWNER);
        }
        return user;
    }

    private Evaluation findOwnedManagerEvaluation(Long id, Long employeeId) {
        Evaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(EvaluationErrorCode.EVALUATION_NOT_FOUND));
        if (evaluation.getEvaluationType() != EvaluationType.MANAGER
                || !employeeId.equals(evaluation.getEvaluatorEmployeeId())) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_OWNER);
        }
        return evaluation;
    }

    private void requireCurrentDirectManager(Evaluation evaluation, Long employeeId) {
        Long directManager;
        try {
            directManager = organizationQueryService
                    .findDirectManagerEmployeeId(evaluation.getTargetEmployeeId());
        } catch (IllegalStateException exception) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_MANAGER_RELATION_INVALID);
        }
        if (!employeeId.equals(directManager)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_MANAGER_RELATION_INVALID);
        }
    }

    private EvaluationCycle findCycle(Long id) {
        return cycleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND));
    }

    private Map<Long, EvaluationItem> templateItems(Long templateId) {
        Map<Long, EvaluationItem> result = new HashMap<>();
        for (EvaluationItem item : itemRepository
                .findByEvaluationTemplateIdOrderByItemOrderAsc(templateId)) {
            result.put(item.getEvaluationItemId(), item);
        }
        return result;
    }

    private void validate(ManagerEvaluationDraftRequest request, Map<Long, EvaluationItem> items) {
        validateFeedback(request.overallFeedback(), 2000);
        Set<Long> ids = new HashSet<>();
        for (ManagerEvaluationItemDraftRequest draft : request.items()) {
            if (draft == null || draft.evaluationItemId() == null
                    || !ids.add(draft.evaluationItemId())
                    || !items.containsKey(draft.evaluationItemId())) {
                throw new BusinessException(EvaluationErrorCode.EVALUATION_ITEM_MISMATCH);
            }
            validateScore(draft.score());
            validateFeedback(draft.itemFeedback(), 1000);
        }
    }

    private void requireWritable(Evaluation evaluation, EvaluationCycle cycle) {
        if (determineStatus(cycle) != EvaluationCycleStatus.OPEN
                || evaluation.getEvaluationStatus() != EvaluationStatus.DRAFT) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_WRITABLE);
        }
    }

    private void validateScore(BigDecimal score) {
        if (score == null) return;
        int scale = Math.max(score.stripTrailingZeros().scale(), 0);
        if (scale > 1 || score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_SCORE_INVALID);
        }
    }

    private void validateFeedback(String content, int maximum) {
        if (content != null && content.length() > maximum) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_FEEDBACK_INVALID);
        }
    }

    private List<EvaluationFeedback> overallFeedbacks(Long evaluationId) {
        List<EvaluationFeedback> result = feedbackRepository
                .findByEvaluationIdAndFeedbackType(evaluationId, FeedbackType.OVERALL);
        if (result.size() > 1) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_FEEDBACK_CONFLICT);
        }
        return result;
    }

    private void saveScore(Long evaluationId, ManagerEvaluationItemDraftRequest draft) {
        EvaluationScore existing = scoreRepository
                .findByEvaluationIdAndEvaluationItemId(evaluationId, draft.evaluationItemId())
                .orElse(null);
        if (draft.score() == null) {
            if (existing != null) scoreRepository.delete(existing);
        } else if (existing == null) {
            scoreRepository.save(new EvaluationScore(evaluationId, draft.evaluationItemId(), draft.score()));
        } else existing.setScore(draft.score());
    }

    private void saveItemFeedback(Long evaluationId, ManagerEvaluationItemDraftRequest draft) {
        EvaluationFeedback existing = feedbackRepository
                .findByEvaluationIdAndEvaluationItemIdAndFeedbackType(
                        evaluationId, draft.evaluationItemId(), FeedbackType.ITEM).orElse(null);
        if (removed(draft.itemFeedback())) {
            if (existing != null) feedbackRepository.delete(existing);
        } else if (existing == null) {
            feedbackRepository.save(new EvaluationFeedback(
                    evaluationId, draft.evaluationItemId(), FeedbackType.ITEM,
                    draft.itemFeedback(), false));
        } else existing.setFeedbackContent(draft.itemFeedback());
    }

    private void saveOverallFeedback(Long id, String content, List<EvaluationFeedback> rows) {
        EvaluationFeedback existing = rows.isEmpty() ? null : rows.get(0);
        if (removed(content)) {
            if (existing != null) feedbackRepository.delete(existing);
        } else if (existing == null) {
            feedbackRepository.save(new EvaluationFeedback(id, null, FeedbackType.OVERALL, content, false));
        } else existing.setFeedbackContent(content);
    }

    private boolean removed(String value) { return value == null || value.isEmpty(); }

    private ManagerEvaluationResponse buildResponse(Evaluation evaluation, EvaluationCycle cycle) {
        List<EvaluationItem> items = itemRepository
                .findByEvaluationTemplateIdOrderByItemOrderAsc(evaluation.getEvaluationTemplateId());
        Map<Long, BigDecimal> scores = new HashMap<>();
        scoreRepository.findByEvaluationId(evaluation.getEvaluationId())
                .forEach(score -> scores.put(score.getEvaluationItemId(), score.getScore()));
        Map<Long, String> feedbacks = new HashMap<>();
        feedbackRepository.findByEvaluationIdAndFeedbackType(
                evaluation.getEvaluationId(), FeedbackType.ITEM)
                .forEach(feedback -> {
                    if (feedback.getEvaluationItemId() != null) {
                        feedbacks.put(feedback.getEvaluationItemId(), feedback.getFeedbackContent());
                    }
                });
        List<EvaluationFeedback> overall = overallFeedbacks(evaluation.getEvaluationId());
        EmployeeSummary target = requireEmployeeSummary(
                employeeSummaries(List.of(evaluation.getTargetEmployeeId())),
                evaluation.getTargetEmployeeId());
        return new ManagerEvaluationResponse(
                evaluation.getEvaluationId(), evaluation.getEvaluationCycleId(),
                evaluation.getEvaluationTemplateId(), evaluation.getTargetEmployeeId(),
                evaluation.getEvaluationStatus(), determineStatus(cycle), target,
                items.stream().map(item -> new ManagerEvaluationItemResponse(
                        item.getEvaluationItemId(), item.getItemOrder(), item.getItemName(),
                        item.getItemDescription(), item.getWeight(), item.getIsRequired(),
                        item.getMinimumScore(), item.getMaximumScore(),
                        scores.get(item.getEvaluationItemId()),
                        feedbacks.get(item.getEvaluationItemId()))).toList(),
                overall.isEmpty() ? null : overall.get(0).getFeedbackContent());
    }

    private Map<Long, EmployeeSummary> employeeSummaries(List<Long> ids) {
        Map<Long, EmployeeSummary> result = new HashMap<>();
        organizationQueryService.findEmployeeSummaries(ids)
                .forEach(summary -> result.put(summary.employeeId(), summary));
        return result;
    }

    private EmployeeSummary requireEmployeeSummary(Map<Long, EmployeeSummary> map, Long id) {
        EmployeeSummary result = map.get(id);
        if (result == null) throw new BusinessException(EvaluationErrorCode.EVALUATION_TARGET_INVALID);
        return result;
    }

    private EvaluationCycleStatus determineStatus(EvaluationCycle cycle) {
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(cycle.getStartDate())) return EvaluationCycleStatus.PLANNED;
        if (today.isAfter(cycle.getEndDate())) return EvaluationCycleStatus.CLOSED;
        return EvaluationCycleStatus.OPEN;
    }
}
