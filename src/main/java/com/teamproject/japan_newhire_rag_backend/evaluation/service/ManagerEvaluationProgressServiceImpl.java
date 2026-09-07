package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationProgressDetailResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationProgressEmployeeResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationProgressResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@Service
@Transactional(readOnly = true)
public class ManagerEvaluationProgressServiceImpl implements ManagerEvaluationProgressService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO_RATE = new BigDecimal("0.0");

    private final EvaluationRepository evaluationRepository;
    private final EvaluationCycleRepository cycleRepository;
    private final OrganizationQueryService organizationQueryService;
    private final CurrentUserProvider currentUserProvider;

    public ManagerEvaluationProgressServiceImpl(
            EvaluationRepository evaluationRepository,
            EvaluationCycleRepository cycleRepository,
            OrganizationQueryService organizationQueryService,
            CurrentUserProvider currentUserProvider
    ) {
        this.evaluationRepository = evaluationRepository;
        this.cycleRepository = cycleRepository;
        this.organizationQueryService = organizationQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public ManagerEvaluationProgressResponse getManagedProgress(Long evaluationCycleId) {
        CurrentUserContext user = requireManager();
        EvaluationCycle cycle = cycleRepository.findById(evaluationCycleId)
                .orElseThrow(() -> new BusinessException(
                        EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND));
        Set<Long> managedEmployeeIds = normalizedIds(
                organizationQueryService.findManagedEmployeeIds(user.employeeId()));
        Map<Long, EvaluationSet> evaluationsByEmployee = evaluationSets(
                evaluationRepository.findByEvaluationCycleId(evaluationCycleId),
                managedEmployeeIds);
        Map<Long, EmployeeSummary> summaries = employeeSummaries(
                evaluationsByEmployee.keySet());

        long selfCompletedCount = evaluationsByEmployee.values().stream()
                .filter(set -> isCompleted(set.self))
                .count();
        long managerCompletedCount = evaluationsByEmployee.values().stream()
                .filter(set -> isCompleted(set.manager))
                .count();
        long completedEmployees = evaluationsByEmployee.values().stream()
                .filter(this::isEmployeeCompleted)
                .count();
        long totalEmployees = evaluationsByEmployee.size();

        List<ManagerEvaluationProgressEmployeeResponse> employees = evaluationsByEmployee
                .entrySet().stream()
                .map(entry -> employeeResponse(
                        requireSummary(summaries, entry.getKey()), entry.getValue()))
                .toList();
        return new ManagerEvaluationProgressResponse(
                cycle.getEvaluationCycleId(), cycle.getCycleName(), totalEmployees,
                completedEmployees, completionRate(completedEmployees, totalEmployees),
                selfCompletedCount, managerCompletedCount, employees);
    }

    private CurrentUserContext requireManager() {
        CurrentUserContext user = currentUserProvider.getCurrentUser();
        if (!user.roles().contains(RoleType.MANAGER) || user.employeeId() == null) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ACCESS_DENIED);
        }
        return user;
    }

    private Set<Long> normalizedIds(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) return Set.of();
        Set<Long> result = new HashSet<>();
        employeeIds.stream().filter(id -> id != null).forEach(result::add);
        return result;
    }

    private Map<Long, EvaluationSet> evaluationSets(
            List<Evaluation> evaluations,
            Set<Long> managedEmployeeIds
    ) {
        Map<Long, EvaluationSet> result = new TreeMap<>();
        for (Evaluation evaluation : evaluations) {
            Long targetEmployeeId = evaluation.getTargetEmployeeId();
            if (!managedEmployeeIds.contains(targetEmployeeId)) continue;
            EvaluationSet set = result.computeIfAbsent(
                    targetEmployeeId, ignored -> new EvaluationSet());
            if (evaluation.getEvaluationType() == EvaluationType.SELF) {
                if (set.self != null) throw evaluationConflict();
                set.self = evaluation;
            } else if (evaluation.getEvaluationType() == EvaluationType.MANAGER) {
                if (set.manager != null) throw evaluationConflict();
                set.manager = evaluation;
            }
        }
        return result;
    }

    private Map<Long, EmployeeSummary> employeeSummaries(Set<Long> employeeIds) {
        Map<Long, EmployeeSummary> result = new HashMap<>();
        organizationQueryService.findEmployeeSummaries(employeeIds)
                .forEach(summary -> result.put(summary.employeeId(), summary));
        return result;
    }

    private EmployeeSummary requireSummary(
            Map<Long, EmployeeSummary> summaries,
            Long employeeId
    ) {
        EmployeeSummary summary = summaries.get(employeeId);
        if (summary == null) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_TARGET_INVALID);
        }
        return summary;
    }

    private ManagerEvaluationProgressEmployeeResponse employeeResponse(
            EmployeeSummary employee,
            EvaluationSet evaluations
    ) {
        return new ManagerEvaluationProgressEmployeeResponse(
                employee, detail(evaluations.self), detail(evaluations.manager),
                isEmployeeCompleted(evaluations));
    }

    private ManagerEvaluationProgressDetailResponse detail(Evaluation evaluation) {
        if (evaluation == null) return null;
        return new ManagerEvaluationProgressDetailResponse(
                evaluation.getEvaluationId(), evaluation.getEvaluationStatus(),
                evaluation.getSubmittedAt());
    }

    private boolean isEmployeeCompleted(EvaluationSet evaluations) {
        return isCompleted(evaluations.self) && isCompleted(evaluations.manager);
    }

    private boolean isCompleted(Evaluation evaluation) {
        if (evaluation == null) return false;
        EvaluationStatus status = evaluation.getEvaluationStatus();
        return status == EvaluationStatus.SUBMITTED || status == EvaluationStatus.PUBLISHED;
    }

    private BigDecimal completionRate(long completedEmployees, long totalEmployees) {
        if (totalEmployees == 0) return ZERO_RATE;
        return BigDecimal.valueOf(completedEmployees)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(totalEmployees), 1, RoundingMode.HALF_UP);
    }

    private BusinessException evaluationConflict() {
        return new BusinessException(EvaluationErrorCode.EVALUATION_PUBLISH_CONFLICT);
    }

    private static final class EvaluationSet {
        private Evaluation self;
        private Evaluation manager;
    }
}
