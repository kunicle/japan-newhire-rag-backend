package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationCycleCreateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationCycleResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationCycleUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@Service
@Transactional(readOnly = true)
public class EvaluationCycleServiceImpl implements EvaluationCycleService {

    private final EvaluationCycleRepository evaluationCycleRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public EvaluationCycleServiceImpl(
            EvaluationCycleRepository evaluationCycleRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.evaluationCycleRepository = evaluationCycleRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EvaluationCycleResponse create(EvaluationCycleCreateRequest request) {
        CurrentUserContext currentUser = requireHrManager();
        validateDates(request.startDate(), request.endDate(), request.plannedPublishDate());

        EvaluationCycleStatus currentStatus = determineStatus(
                request.startDate(),
                request.endDate());
        EvaluationCycle cycle = new EvaluationCycle(
                request.cycleName(),
                request.startDate(),
                request.endDate(),
                request.plannedPublishDate(),
                currentStatus,
                currentUser.appUserId());

        return toResponse(evaluationCycleRepository.save(cycle));
    }

    @Override
    public EvaluationCycleResponse getById(Long evaluationCycleId) {
        requireHrManager();
        return toResponse(findCycle(evaluationCycleId));
    }

    @Override
    @Transactional
    public EvaluationCycleResponse update(
            Long evaluationCycleId,
            EvaluationCycleUpdateRequest request
    ) {
        requireHrManager();
        EvaluationCycle cycle = findCycle(evaluationCycleId);
        EvaluationCycleStatus currentStatus = determineStatus(cycle.getStartDate(), cycle.getEndDate());

        if (currentStatus == EvaluationCycleStatus.CLOSED) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_CYCLE_NOT_EDITABLE);
        }
        if (currentStatus == EvaluationCycleStatus.OPEN
                && (!Objects.equals(cycle.getStartDate(), request.startDate())
                || !Objects.equals(cycle.getEndDate(), request.endDate()))) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_CYCLE_NOT_EDITABLE);
        }

        validateDates(request.startDate(), request.endDate(), request.plannedPublishDate());
        cycle.setCycleName(request.cycleName());
        cycle.setPlannedPublishDate(request.plannedPublishDate());
        if (currentStatus == EvaluationCycleStatus.PLANNED) {
            cycle.setStartDate(request.startDate());
            cycle.setEndDate(request.endDate());
        }
        cycle.setCycleStatus(determineStatus(cycle.getStartDate(), cycle.getEndDate()));

        return toResponse(cycle);
    }

    @Override
    public EvaluationCycleStatus getCurrentStatus(Long evaluationCycleId) {
        requireHrManager();
        EvaluationCycle cycle = findCycle(evaluationCycleId);
        return determineStatus(cycle.getStartDate(), cycle.getEndDate());
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

    private void validateDates(
            LocalDate startDate,
            LocalDate endDate,
            LocalDate plannedPublishDate
    ) {
        if (startDate == null
                || endDate == null
                || plannedPublishDate == null
                || startDate.isAfter(endDate)
                || plannedPublishDate.isBefore(startDate)) {
            throw new BusinessException(EvaluationErrorCode.EVALUATION_CYCLE_INVALID_DATE);
        }
    }

    private EvaluationCycleStatus determineStatus(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(startDate)) {
            return EvaluationCycleStatus.PLANNED;
        }
        if (today.isAfter(endDate)) {
            return EvaluationCycleStatus.CLOSED;
        }
        return EvaluationCycleStatus.OPEN;
    }

    private EvaluationCycleResponse toResponse(EvaluationCycle cycle) {
        return EvaluationCycleResponse.from(
                cycle,
                determineStatus(cycle.getStartDate(), cycle.getEndDate()));
    }
}
