package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.internal.AuditLogRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.internal.AuditLogRecordService;
import com.teamproject.japan_newhire_rag_backend.evaluation.Evaluation;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationFeedback;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationFeedbackRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationPublishHistory;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationPublishHistoryRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishPreviewFeedbackResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishPreviewResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@Service
public class EvaluationPublishServiceImpl implements EvaluationPublishService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationCycleRepository cycleRepository;
    private final EvaluationFeedbackRepository feedbackRepository;
    private final EvaluationPublishHistoryRepository historyRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLogRecordService auditLogRecordService;
    private final Clock clock;

    public EvaluationPublishServiceImpl(
            EvaluationRepository evaluationRepository,
            EvaluationCycleRepository cycleRepository,
            EvaluationFeedbackRepository feedbackRepository,
            EvaluationPublishHistoryRepository historyRepository,
            CurrentUserProvider currentUserProvider,
            AuditLogRecordService auditLogRecordService,
            Clock clock
    ) {
        this.evaluationRepository = evaluationRepository;
        this.cycleRepository = cycleRepository;
        this.feedbackRepository = feedbackRepository;
        this.historyRepository = historyRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditLogRecordService = auditLogRecordService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationPublishPreviewResponse getPublishPreview(Long evaluationId) {
        requireHrManager();
        Evaluation entry = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(EvaluationErrorCode.EVALUATION_NOT_FOUND));
        EvaluationSet evaluations = findEvaluationSet(entry);
        List<EvaluationPublishPreviewFeedbackResponse> managerFeedbacks = feedbackRepository
                .findByEvaluationId(evaluations.manager.getEvaluationId()).stream()
                .map(feedback -> new EvaluationPublishPreviewFeedbackResponse(
                        feedback.getEvaluationFeedbackId(),
                        feedback.getEvaluationItemId(),
                        feedback.getFeedbackType(),
                        feedback.getFeedbackContent(),
                        feedback.getIsVisibleToEmployee()))
                .toList();
        return new EvaluationPublishPreviewResponse(
                evaluations.self.getEvaluationCycleId(),
                evaluations.self.getTargetEmployeeId(),
                evaluations.self.getEvaluationId(),
                evaluations.manager.getEvaluationId(),
                managerFeedbacks);
    }

    @Override
    @Transactional
    public EvaluationPublishResponse publish(
            Long evaluationId,
            EvaluationPublishRequest request
    ) {
        CurrentUserContext user = requireHrManager();
        Evaluation entry = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(EvaluationErrorCode.EVALUATION_NOT_FOUND));
        EvaluationSet evaluations = findEvaluationSet(entry);
        EvaluationCycle cycle = cycleRepository.findById(entry.getEvaluationCycleId())
                .orElseThrow(() -> new BusinessException(EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND));
        requireClosed(cycle);

        if (bothPublished(evaluations)) {
            return response(evaluations, currentlyVisibleManagerFeedbackIds(
                    evaluations.manager.getEvaluationId()), true);
        }
        requireSubmittedSet(evaluations);

        String publishReason = request == null ? null : request.publishReason();
        List<Long> selectedIds = normalizeSelectedIds(
                request == null ? null : request.visibleManagerFeedbackIds());
        validatePublishReason(publishReason);

        List<EvaluationFeedback> selfFeedbacks = feedbackRepository
                .findByEvaluationId(evaluations.self.getEvaluationId());
        List<EvaluationFeedback> managerFeedbacks = feedbackRepository
                .findByEvaluationId(evaluations.manager.getEvaluationId());
        validateSelectedFeedbackIds(selectedIds, managerFeedbacks);

        Set<Long> selected = Set.copyOf(selectedIds);
        selfFeedbacks.forEach(feedback -> feedback.setIsVisibleToEmployee(true));
        managerFeedbacks.forEach(feedback -> feedback.setIsVisibleToEmployee(
                selected.contains(feedback.getEvaluationFeedbackId())));

        LocalDateTime publishedAt = LocalDateTime.now(clock);
        publish(evaluations.self, publishedAt);
        publish(evaluations.manager, publishedAt);
        saveHistories(evaluations, user.appUserId(), publishReason, publishedAt);
        recordAudit(evaluations, user.appUserId(), selectedIds);
        return response(evaluations, selectedIds, false);
    }

    private CurrentUserContext requireHrManager() {
        CurrentUserContext user = currentUserProvider.getCurrentUser();
        if (!user.roles().contains(RoleType.HR_MANAGER) || user.appUserId() == null) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ACCESS_DENIED);
        }
        return user;
    }

    private EvaluationSet findEvaluationSet(Evaluation entry) {
        List<Evaluation> rows = evaluationRepository
                .findByEvaluationCycleIdAndTargetEmployeeId(
                        entry.getEvaluationCycleId(), entry.getTargetEmployeeId());
        Evaluation self = null;
        Evaluation manager = null;
        for (Evaluation row : rows) {
            if (row.getEvaluationType() == EvaluationType.SELF) {
                if (self != null) throw publishConflict();
                self = row;
            } else if (row.getEvaluationType() == EvaluationType.MANAGER) {
                if (manager != null) throw publishConflict();
                manager = row;
            }
        }
        if (self == null || manager == null) throw publishConflict();
        return new EvaluationSet(self, manager);
    }

    private void requireClosed(EvaluationCycle cycle) {
        if (!LocalDate.now(clock).isAfter(cycle.getEndDate())) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_PUBLISHABLE);
        }
    }

    private boolean bothPublished(EvaluationSet evaluations) {
        return evaluations.self.getEvaluationStatus() == EvaluationStatus.PUBLISHED
                && evaluations.manager.getEvaluationStatus() == EvaluationStatus.PUBLISHED;
    }

    private void requireSubmittedSet(EvaluationSet evaluations) {
        if (evaluations.self.getEvaluationStatus() != EvaluationStatus.SUBMITTED
                || evaluations.manager.getEvaluationStatus() != EvaluationStatus.SUBMITTED) {
            if (evaluations.self.getEvaluationStatus() == EvaluationStatus.PUBLISHED
                    || evaluations.manager.getEvaluationStatus() == EvaluationStatus.PUBLISHED) {
                throw publishConflict();
            }
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_PUBLISHABLE);
        }
    }

    private List<Long> normalizeSelectedIds(List<Long> values) {
        if (values == null) return List.of();
        if (values.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(values).size() != values.size()) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_FEEDBACK_INVALID);
        }
        return values.stream().sorted().toList();
    }

    private void validatePublishReason(String reason) {
        if (reason != null && reason.length() > 500) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_NOT_PUBLISHABLE);
        }
    }

    private void validateSelectedFeedbackIds(
            List<Long> selectedIds,
            List<EvaluationFeedback> managerFeedbacks
    ) {
        Set<Long> managerIds = new HashSet<>();
        managerFeedbacks.forEach(feedback -> managerIds.add(feedback.getEvaluationFeedbackId()));
        if (!managerIds.containsAll(selectedIds)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_FEEDBACK_INVALID);
        }
    }

    private List<Long> currentlyVisibleManagerFeedbackIds(Long evaluationId) {
        return feedbackRepository.findByEvaluationId(evaluationId).stream()
                .filter(feedback -> Boolean.TRUE.equals(feedback.getIsVisibleToEmployee()))
                .map(EvaluationFeedback::getEvaluationFeedbackId)
                .sorted()
                .toList();
    }

    private void publish(Evaluation evaluation, LocalDateTime publishedAt) {
        evaluation.setEvaluationStatus(EvaluationStatus.PUBLISHED);
        evaluation.setPublishedAt(publishedAt);
    }

    private void saveHistories(
            EvaluationSet evaluations,
            Long actorAppUserId,
            String reason,
            LocalDateTime publishedAt
    ) {
        historyRepository.saveAll(List.of(
                history(evaluations.self, actorAppUserId, reason, publishedAt),
                history(evaluations.manager, actorAppUserId, reason, publishedAt)));
    }

    private EvaluationPublishHistory history(
            Evaluation evaluation,
            Long actorAppUserId,
            String reason,
            LocalDateTime publishedAt
    ) {
        return new EvaluationPublishHistory(
                evaluation.getEvaluationId(), actorAppUserId,
                EvaluationStatus.SUBMITTED, EvaluationStatus.PUBLISHED,
                reason, publishedAt);
    }

    private void recordAudit(
            EvaluationSet evaluations,
            Long actorAppUserId,
            List<Long> selectedIds
    ) {
        Map<String, Object> changed = new LinkedHashMap<>();
        changed.put("cycleId", evaluations.self.getEvaluationCycleId());
        changed.put("targetEmployeeId", evaluations.self.getTargetEmployeeId());
        changed.put("selfEvaluationId", evaluations.self.getEvaluationId());
        changed.put("managerEvaluationId", evaluations.manager.getEvaluationId());
        changed.put("visibleManagerFeedbackIds", new ArrayList<>(selectedIds));
        auditLogRecordService.record(new AuditLogRecordCommand(
                actorAppUserId, AuditActionType.EVALUATION_RESULT_PUBLISHED,
                evaluations.self.getEvaluationId(), null, changed, null, null));
    }

    private EvaluationPublishResponse response(
            EvaluationSet evaluations,
            List<Long> visibleManagerFeedbackIds,
            boolean idempotent
    ) {
        return new EvaluationPublishResponse(
                evaluations.self.getEvaluationCycleId(), evaluations.self.getTargetEmployeeId(),
                evaluations.self.getEvaluationId(), evaluations.manager.getEvaluationId(),
                EvaluationStatus.PUBLISHED, EvaluationStatus.PUBLISHED,
                evaluations.self.getPublishedAt(), List.copyOf(visibleManagerFeedbackIds), idempotent);
    }

    private BusinessException publishConflict() {
        return new BusinessException(EvaluationErrorCode.EVALUATION_PUBLISH_CONFLICT);
    }

    private record EvaluationSet(Evaluation self, Evaluation manager) {
    }
}
