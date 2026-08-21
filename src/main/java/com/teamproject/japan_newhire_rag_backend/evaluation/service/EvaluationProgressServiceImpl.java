package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.EmployeeSummary;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
import com.teamproject.japan_newhire_rag_backend.evaluation.Evaluation;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationProgressDetailResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationProgressEmployeeResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationProgressResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationProgressStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationProgressSummary;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@Service
@Transactional(readOnly = true)
public class EvaluationProgressServiceImpl implements EvaluationProgressService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationCycleRepository cycleRepository;
    private final OrganizationQueryService organizationQueryService;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public EvaluationProgressServiceImpl(
            EvaluationRepository evaluationRepository,
            EvaluationCycleRepository cycleRepository,
            OrganizationQueryService organizationQueryService,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.evaluationRepository = evaluationRepository;
        this.cycleRepository = cycleRepository;
        this.organizationQueryService = organizationQueryService;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Override
    public EvaluationProgressResponse getCycleProgress(Long evaluationCycleId) {
        requireHrManager();
        EvaluationCycle cycle = cycleRepository.findById(evaluationCycleId)
                .orElseThrow(() -> new BusinessException(EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND));
        List<Evaluation> evaluations = evaluationRepository.findByEvaluationCycleId(evaluationCycleId);

        Map<Long, TargetEvaluations> byTarget = new LinkedHashMap<>();
        for (Evaluation evaluation : evaluations) {
            TargetEvaluations target = byTarget.computeIfAbsent(
                    evaluation.getTargetEmployeeId(), ignored -> new TargetEvaluations());
            if (evaluation.getEvaluationType() == EvaluationType.SELF) {
                target.self = evaluation;
            } else if (evaluation.getEvaluationType() == EvaluationType.MANAGER) {
                target.manager = evaluation;
            }
        }

        Map<Long, EmployeeSummary> summaries = employeeSummaries(new ArrayList<>(byTarget.keySet()));
        List<EvaluationProgressEmployeeResponse> employees = byTarget.entrySet().stream()
                .map(entry -> new EvaluationProgressEmployeeResponse(
                        requireEmployeeSummary(summaries, entry.getKey()),
                        detail(entry.getValue().self),
                        detail(entry.getValue().manager)))
                .toList();

        return new EvaluationProgressResponse(
                cycle.getEvaluationCycleId(), cycle.getCycleName(),
                cycle.getStartDate(), cycle.getEndDate(), currentCycleStatus(cycle),
                byTarget.size(), summary(evaluations, EvaluationType.SELF),
                summary(evaluations, EvaluationType.MANAGER), employees);
    }

    private void requireHrManager() {
        if (!currentUserProvider.getCurrentUser().roles().contains(RoleType.HR_MANAGER)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ACCESS_DENIED);
        }
    }

    private EvaluationProgressSummary summary(List<Evaluation> evaluations, EvaluationType type) {
        long notStarted = 0;
        long inProgress = 0;
        long submitted = 0;
        for (Evaluation evaluation : evaluations) {
            if (evaluation.getEvaluationType() != type) continue;
            switch (progressStatus(evaluation)) {
                case NOT_STARTED -> notStarted++;
                case IN_PROGRESS -> inProgress++;
                case SUBMITTED -> submitted++;
            }
        }
        return new EvaluationProgressSummary(notStarted, inProgress, submitted);
    }

    private EvaluationProgressDetailResponse detail(Evaluation evaluation) {
        if (evaluation == null) return null;
        return new EvaluationProgressDetailResponse(
                evaluation.getEvaluationId(), evaluation.getEvaluationStatus(),
                progressStatus(evaluation), evaluation.getSubmittedAt());
    }

    private EvaluationProgressStatus progressStatus(Evaluation evaluation) {
        EvaluationStatus status = evaluation.getEvaluationStatus();
        if (status == EvaluationStatus.SUBMITTED || status == EvaluationStatus.PUBLISHED) {
            return EvaluationProgressStatus.SUBMITTED;
        }
        if (status == EvaluationStatus.RETURNED || evaluation.getLastDraftSavedAt() != null) {
            return EvaluationProgressStatus.IN_PROGRESS;
        }
        return EvaluationProgressStatus.NOT_STARTED;
    }

    private Map<Long, EmployeeSummary> employeeSummaries(List<Long> employeeIds) {
        Map<Long, EmployeeSummary> result = new HashMap<>();
        organizationQueryService.findEmployeeSummaries(employeeIds)
                .forEach(summary -> result.put(summary.employeeId(), summary));
        return result;
    }

    private EmployeeSummary requireEmployeeSummary(Map<Long, EmployeeSummary> summaries, Long employeeId) {
        EmployeeSummary summary = summaries.get(employeeId);
        if (summary == null) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_TARGET_INVALID);
        }
        return summary;
    }

    private EvaluationCycleStatus currentCycleStatus(EvaluationCycle cycle) {
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(cycle.getStartDate())) return EvaluationCycleStatus.PLANNED;
        if (today.isAfter(cycle.getEndDate())) return EvaluationCycleStatus.CLOSED;
        return EvaluationCycleStatus.OPEN;
    }

    private static final class TargetEvaluations {
        private Evaluation self;
        private Evaluation manager;
    }
}
