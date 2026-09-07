package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationTemplate;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationTemplateRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationTemplateCreateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationTemplateResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationTemplateUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@Service
@Transactional(readOnly = true)
public class EvaluationTemplateServiceImpl implements EvaluationTemplateService {

    private final EvaluationTemplateRepository evaluationTemplateRepository;
    private final EvaluationCycleRepository evaluationCycleRepository;
    private final EvaluationRepository evaluationRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public EvaluationTemplateServiceImpl(
            EvaluationTemplateRepository evaluationTemplateRepository,
            EvaluationCycleRepository evaluationCycleRepository,
            EvaluationRepository evaluationRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.evaluationTemplateRepository = evaluationTemplateRepository;
        this.evaluationCycleRepository = evaluationCycleRepository;
        this.evaluationRepository = evaluationRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EvaluationTemplateResponse create(EvaluationTemplateCreateRequest request) {
        CurrentUserContext currentUser = requireHrManager();
        validateRequest(request);
        EvaluationCycle cycle = findCycle(request.evaluationCycleId());
        requirePlanned(cycle);
        if (evaluationTemplateRepository.existsByEvaluationCycleIdAndEvaluationType(
                request.evaluationCycleId(), request.evaluationType())) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_TEMPLATE_DUPLICATE_TYPE);
        }

        EvaluationTemplate template = new EvaluationTemplate(
                request.evaluationCycleId(),
                request.templateName(),
                request.evaluationType(),
                request.templateDescription(),
                request.isActive(),
                currentUser.appUserId());
        return EvaluationTemplateResponse.from(evaluationTemplateRepository.save(template));
    }

    @Override
    public EvaluationTemplateResponse getById(Long evaluationTemplateId) {
        requireHrManager();
        return EvaluationTemplateResponse.from(findTemplate(evaluationTemplateId));
    }

    @Override
    public List<EvaluationTemplateResponse> getByCycleId(Long evaluationCycleId) {
        requireHrManager();
        findCycle(evaluationCycleId);
        return evaluationTemplateRepository
                .findByEvaluationCycleIdOrderByEvaluationTypeAsc(evaluationCycleId)
                .stream()
                .map(EvaluationTemplateResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public EvaluationTemplateResponse update(
            Long evaluationTemplateId,
            EvaluationTemplateUpdateRequest request
    ) {
        requireHrManager();
        validateRequest(request);
        EvaluationTemplate template = findTemplate(evaluationTemplateId);
        requirePlanned(findCycle(template.getEvaluationCycleId()));
        if (evaluationRepository.existsByEvaluationTemplateId(evaluationTemplateId)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_TEMPLATE_NOT_EDITABLE);
        }
        if (template.getEvaluationType() != request.evaluationType()
                && evaluationTemplateRepository.existsByEvaluationCycleIdAndEvaluationType(
                template.getEvaluationCycleId(), request.evaluationType())) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_TEMPLATE_DUPLICATE_TYPE);
        }

        template.setTemplateName(request.templateName());
        template.setEvaluationType(request.evaluationType());
        template.setTemplateDescription(request.templateDescription());
        template.setIsActive(request.isActive());
        return EvaluationTemplateResponse.from(template);
    }

    private CurrentUserContext requireHrManager() {
        CurrentUserContext currentUser = currentUserProvider.getCurrentUser();
        if (!currentUser.roles().contains(RoleType.HR_MANAGER)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ACCESS_DENIED);
        }
        return currentUser;
    }

    private EvaluationCycle findCycle(Long evaluationCycleId) {
        return evaluationCycleRepository.findById(evaluationCycleId)
                .orElseThrow(() -> new BusinessException(
                        EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND));
    }

    private EvaluationTemplate findTemplate(Long evaluationTemplateId) {
        return evaluationTemplateRepository.findById(evaluationTemplateId)
                .orElseThrow(() -> new BusinessException(
                        EvaluationErrorCode.EVALUATION_TEMPLATE_NOT_FOUND));
    }

    private void requirePlanned(EvaluationCycle cycle) {
        LocalDate today = LocalDate.now(clock);
        if (!today.isBefore(cycle.getStartDate())) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_CYCLE_NOT_EDITABLE);
        }
    }

    private void validateRequest(EvaluationTemplateCreateRequest request) {
        if (request == null || request.evaluationCycleId() == null) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_TEMPLATE_INVALID_VALUE);
        }
        validateValues(
                request.templateName(),
                request.evaluationType(),
                request.templateDescription());
    }

    private void validateRequest(EvaluationTemplateUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_TEMPLATE_INVALID_VALUE);
        }
        validateValues(
                request.templateName(),
                request.evaluationType(),
                request.templateDescription());
    }

    private void validateValues(
            String templateName,
            EvaluationType evaluationType,
            String templateDescription
    ) {
        if (templateName == null
                || templateName.isBlank()
                || templateName.length() > 100
                || evaluationType == null
                || (templateDescription != null && templateDescription.length() > 1000)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_TEMPLATE_INVALID_VALUE);
        }
    }
}
