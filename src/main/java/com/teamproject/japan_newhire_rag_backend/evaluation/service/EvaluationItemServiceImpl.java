package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationItem;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationItemRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationTemplate;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationTemplateRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationItemCreateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationItemResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationItemUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@Service
@Transactional(readOnly = true)
public class EvaluationItemServiceImpl implements EvaluationItemService {

    private final EvaluationItemRepository itemRepository;
    private final EvaluationTemplateRepository templateRepository;
    private final EvaluationCycleRepository cycleRepository;
    private final EvaluationRepository evaluationRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public EvaluationItemServiceImpl(
            EvaluationItemRepository itemRepository,
            EvaluationTemplateRepository templateRepository,
            EvaluationCycleRepository cycleRepository,
            EvaluationRepository evaluationRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.itemRepository = itemRepository;
        this.templateRepository = templateRepository;
        this.cycleRepository = cycleRepository;
        this.evaluationRepository = evaluationRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EvaluationItemResponse create(EvaluationItemCreateRequest request) {
        requireHrManager();
        validateRequest(request);
        EvaluationTemplate template = findTemplate(request.evaluationTemplateId());
        requirePlanned(findCycle(template.getEvaluationCycleId()));
        requireTemplateEditable(request.evaluationTemplateId());
        if (itemRepository.existsByEvaluationTemplateIdAndItemOrder(
                request.evaluationTemplateId(), request.itemOrder())) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ITEM_DUPLICATE_ORDER);
        }

        EvaluationItem item = new EvaluationItem(
                request.evaluationTemplateId(), request.itemName(), request.itemDescription(),
                request.itemOrder(), request.weight(), request.isRequired(),
                request.minimumScore(), request.maximumScore());
        return EvaluationItemResponse.from(itemRepository.save(item));
    }

    @Override
    public EvaluationItemResponse getById(Long evaluationItemId) {
        requireHrManager();
        return EvaluationItemResponse.from(findItem(evaluationItemId));
    }

    @Override
    public List<EvaluationItemResponse> getByTemplateId(Long evaluationTemplateId) {
        requireHrManager();
        findTemplate(evaluationTemplateId);
        return itemRepository.findByEvaluationTemplateIdOrderByItemOrderAsc(evaluationTemplateId)
                .stream()
                .map(EvaluationItemResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public EvaluationItemResponse update(
            Long evaluationItemId,
            EvaluationItemUpdateRequest request
    ) {
        requireHrManager();
        validateRequest(request);
        EvaluationItem item = findItem(evaluationItemId);
        EvaluationTemplate template = findTemplate(item.getEvaluationTemplateId());
        requirePlanned(findCycle(template.getEvaluationCycleId()));
        requireTemplateEditable(item.getEvaluationTemplateId());
        if (itemRepository.existsByEvaluationTemplateIdAndItemOrderAndEvaluationItemIdNot(
                item.getEvaluationTemplateId(), request.itemOrder(), evaluationItemId)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ITEM_DUPLICATE_ORDER);
        }

        item.setItemName(request.itemName());
        item.setItemDescription(request.itemDescription());
        item.setItemOrder(request.itemOrder());
        item.setWeight(request.weight());
        item.setIsRequired(request.isRequired());
        item.setMinimumScore(request.minimumScore());
        item.setMaximumScore(request.maximumScore());
        return EvaluationItemResponse.from(item);
    }

    private void requireHrManager() {
        if (!currentUserProvider.getCurrentUser().roles().contains(RoleType.HR_MANAGER)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ACCESS_DENIED);
        }
    }

    private EvaluationItem findItem(Long evaluationItemId) {
        return itemRepository.findById(evaluationItemId)
                .orElseThrow(() -> new BusinessException(
                        EvaluationErrorCode.EVALUATION_ITEM_NOT_FOUND));
    }

    private EvaluationTemplate findTemplate(Long evaluationTemplateId) {
        return templateRepository.findById(evaluationTemplateId)
                .orElseThrow(() -> new BusinessException(
                        EvaluationErrorCode.EVALUATION_TEMPLATE_NOT_FOUND));
    }

    private EvaluationCycle findCycle(Long evaluationCycleId) {
        return cycleRepository.findById(evaluationCycleId)
                .orElseThrow(() -> new BusinessException(
                        EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND));
    }

    private void requirePlanned(EvaluationCycle cycle) {
        if (!LocalDate.now(clock).isBefore(cycle.getStartDate())) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ITEM_NOT_EDITABLE);
        }
    }

    private void requireTemplateEditable(Long evaluationTemplateId) {
        if (evaluationRepository.existsByEvaluationTemplateId(evaluationTemplateId)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ITEM_NOT_EDITABLE);
        }
    }

    private void validateRequest(EvaluationItemCreateRequest request) {
        if (request == null || request.evaluationTemplateId() == null) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ITEM_INVALID_VALUE);
        }
        validateValues(request.itemName(), request.itemDescription(), request.itemOrder(),
                request.weight(), request.minimumScore(), request.maximumScore());
    }

    private void validateRequest(EvaluationItemUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ITEM_INVALID_VALUE);
        }
        validateValues(request.itemName(), request.itemDescription(), request.itemOrder(),
                request.weight(), request.minimumScore(), request.maximumScore());
    }

    private void validateValues(
            String itemName,
            String itemDescription,
            Integer itemOrder,
            BigDecimal weight,
            Integer minimumScore,
            Integer maximumScore
    ) {
        if (itemName == null
                || itemName.isBlank()
                || itemName.length() > 100
                || (itemDescription != null && itemDescription.length() > 1000)
                || itemOrder == null
                || !isValidWeight(weight)
                || (minimumScore != null && minimumScore != 1)
                || (maximumScore != null && maximumScore != 5)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_ITEM_INVALID_VALUE);
        }
    }

    private boolean isValidWeight(BigDecimal weight) {
        if (weight == null || weight.signum() <= 0) {
            return false;
        }
        BigDecimal normalized = weight.stripTrailingZeros();
        int scale = Math.max(normalized.scale(), 0);
        int integerDigits = Math.max(normalized.precision() - normalized.scale(), 0);
        return scale <= 2 && integerDigits <= 5;
    }
}
