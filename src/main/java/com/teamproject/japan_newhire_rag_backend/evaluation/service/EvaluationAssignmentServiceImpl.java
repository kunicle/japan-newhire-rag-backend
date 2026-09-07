package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
import com.teamproject.japan_newhire_rag_backend.evaluation.Evaluation;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationItemRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationTemplate;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationTemplateRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationAssignmentRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationAssignmentResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@Service
public class EvaluationAssignmentServiceImpl implements EvaluationAssignmentService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationCycleRepository cycleRepository;
    private final EvaluationTemplateRepository templateRepository;
    private final EvaluationItemRepository itemRepository;
    private final OrganizationQueryService organizationQueryService;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public EvaluationAssignmentServiceImpl(
            EvaluationRepository evaluationRepository,
            EvaluationCycleRepository cycleRepository,
            EvaluationTemplateRepository templateRepository,
            EvaluationItemRepository itemRepository,
            OrganizationQueryService organizationQueryService,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.evaluationRepository = evaluationRepository;
        this.cycleRepository = cycleRepository;
        this.templateRepository = templateRepository;
        this.itemRepository = itemRepository;
        this.organizationQueryService = organizationQueryService;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EvaluationAssignmentResponse assign(EvaluationAssignmentRequest request) {
        requireHrManager();
        validateRequest(request);

        EvaluationCycle cycle = findCycle(request.evaluationCycleId());
        requirePlanned(cycle);
        requireValidTarget(request.targetEmployeeId());
        Long managerEmployeeId = findValidDirectManager(request.targetEmployeeId());

        EvaluationTemplate selfTemplate = findReadyTemplate(
                request.evaluationCycleId(), EvaluationType.SELF);
        EvaluationTemplate managerTemplate = findReadyTemplate(
                request.evaluationCycleId(), EvaluationType.MANAGER);
        requireItems(selfTemplate, managerTemplate);
        requireNoExistingAssignment(request.evaluationCycleId(), request.targetEmployeeId());

        Evaluation selfEvaluation = new Evaluation(
                request.evaluationCycleId(), selfTemplate.getEvaluationTemplateId(),
                request.targetEmployeeId(), request.targetEmployeeId(),
                EvaluationType.SELF, EvaluationStatus.DRAFT, null, null, null);
        Evaluation managerEvaluation = new Evaluation(
                request.evaluationCycleId(), managerTemplate.getEvaluationTemplateId(),
                request.targetEmployeeId(), managerEmployeeId,
                EvaluationType.MANAGER, EvaluationStatus.DRAFT, null, null, null);

        Evaluation savedSelf = evaluationRepository.save(selfEvaluation);
        Evaluation savedManager = evaluationRepository.save(managerEvaluation);
        return new EvaluationAssignmentResponse(
                request.evaluationCycleId(), request.targetEmployeeId(), managerEmployeeId,
                savedSelf.getEvaluationId(), savedManager.getEvaluationId());
    }

    private void requireHrManager() {
        if (!currentUserProvider.getCurrentUser().roles().contains(RoleType.HR_MANAGER)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ACCESS_DENIED);
        }
    }

    private void validateRequest(EvaluationAssignmentRequest request) {
        if (request == null
                || request.evaluationCycleId() == null
                || request.targetEmployeeId() == null) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ASSIGNMENT_INVALID_VALUE);
        }
    }

    private EvaluationCycle findCycle(Long evaluationCycleId) {
        return cycleRepository.findById(evaluationCycleId)
                .orElseThrow(() -> new BusinessException(
                        EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND));
    }

    private void requirePlanned(EvaluationCycle cycle) {
        if (!LocalDate.now(clock).isBefore(cycle.getStartDate())) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_CYCLE_NOT_ASSIGNABLE);
        }
    }

    private void requireValidTarget(Long targetEmployeeId) {
        if (!organizationQueryService.isValidEmployee(targetEmployeeId)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_TARGET_INVALID);
        }
    }

    private Long findValidDirectManager(Long targetEmployeeId) {
        Long managerEmployeeId;
        try {
            managerEmployeeId = organizationQueryService
                    .findDirectManagerEmployeeId(targetEmployeeId);
        } catch (IllegalStateException exception) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_MANAGER_RELATION_INVALID);
        }
        if (managerEmployeeId == null || managerEmployeeId.equals(targetEmployeeId)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_MANAGER_RELATION_INVALID);
        }
        if (!organizationQueryService.isValidEmployee(managerEmployeeId)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_EVALUATOR_INVALID);
        }
        return managerEmployeeId;
    }

    private EvaluationTemplate findReadyTemplate(
            Long evaluationCycleId,
            EvaluationType evaluationType
    ) {
        return templateRepository
                .findByEvaluationCycleIdAndEvaluationTypeAndIsActiveTrue(
                        evaluationCycleId, evaluationType)
                .orElseThrow(() -> new BusinessException(
                        EvaluationErrorCode.EVALUATION_TEMPLATE_NOT_READY));
    }

    private void requireItems(
            EvaluationTemplate selfTemplate,
            EvaluationTemplate managerTemplate
    ) {
        if (!itemRepository.existsByEvaluationTemplateId(
                selfTemplate.getEvaluationTemplateId())
                || !itemRepository.existsByEvaluationTemplateId(
                managerTemplate.getEvaluationTemplateId())) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_TEMPLATE_NOT_READY);
        }
    }

    private void requireNoExistingAssignment(
            Long evaluationCycleId,
            Long targetEmployeeId
    ) {
        if (evaluationRepository.existsByEvaluationCycleIdAndTargetEmployeeIdAndEvaluationType(
                evaluationCycleId, targetEmployeeId, EvaluationType.SELF)
                || evaluationRepository
                .existsByEvaluationCycleIdAndTargetEmployeeIdAndEvaluationType(
                        evaluationCycleId, targetEmployeeId, EvaluationType.MANAGER)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_DUPLICATE_ASSIGNMENT);
        }
    }
}
