package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class EvaluationCycleServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 12);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-12T00:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private EvaluationCycleRepository evaluationCycleRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private EvaluationCycleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EvaluationCycleServiceImpl(
                evaluationCycleRepository,
                currentUserProvider,
                FIXED_CLOCK);
    }

    @Test
    void hrManagerCreatesCycle() {
        givenRoles(RoleType.HR_MANAGER);
        when(evaluationCycleRepository.save(any(EvaluationCycle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EvaluationCycleResponse response = service.create(createRequest(
                TODAY.plusDays(1),
                TODAY.plusDays(10),
                TODAY.plusDays(11)));

        ArgumentCaptor<EvaluationCycle> captor = ArgumentCaptor.forClass(EvaluationCycle.class);
        verify(evaluationCycleRepository).save(captor.capture());
        EvaluationCycle saved = captor.getValue();
        assertEquals(10L, saved.getCreatedBy());
        assertEquals(EvaluationCycleStatus.PLANNED, saved.getCycleStatus());
        assertEquals(EvaluationCycleStatus.PLANNED, response.cycleStatus());
    }

    @Test
    void systemAdminAloneCannotCreateCycle() {
        givenRoles(RoleType.SYSTEM_ADMIN);

        assertError(
                EvaluationErrorCode.EVALUATION_ACCESS_DENIED,
                () -> service.create(validCreateRequest()));
        verify(evaluationCycleRepository, never()).save(any());
    }

    @Test
    void employeeCannotCreateCycle() {
        givenRoles(RoleType.EMPLOYEE);

        assertError(
                EvaluationErrorCode.EVALUATION_ACCESS_DENIED,
                () -> service.create(validCreateRequest()));
    }

    @Test
    void hrManagerListsCyclesInRepositoryOrderWithLiveStatuses() {
        givenRoles(RoleType.HR_MANAGER);
        EvaluationCycle planned = mock(EvaluationCycle.class);
        when(planned.getCycleName()).thenReturn("Planned");
        when(planned.getStartDate()).thenReturn(TODAY.plusDays(1));
        when(planned.getEndDate()).thenReturn(TODAY.plusDays(10));
        EvaluationCycle closed = mock(EvaluationCycle.class);
        when(closed.getCycleName()).thenReturn("Closed");
        when(closed.getStartDate()).thenReturn(TODAY.minusDays(10));
        when(closed.getEndDate()).thenReturn(TODAY.minusDays(1));
        when(evaluationCycleRepository
                .findAllByDeletedAtIsNullOrderByStartDateDescEvaluationCycleIdDesc())
                .thenReturn(List.of(planned, closed));

        List<EvaluationCycleResponse> result = service.getCycles();

        assertEquals(2, result.size());
        assertEquals(List.of("Planned", "Closed"),
                result.stream().map(EvaluationCycleResponse::cycleName).toList());
        assertEquals(EvaluationCycleStatus.PLANNED, result.get(0).cycleStatus());
        assertEquals(EvaluationCycleStatus.CLOSED, result.get(1).cycleStatus());
        verify(evaluationCycleRepository)
                .findAllByDeletedAtIsNullOrderByStartDateDescEvaluationCycleIdDesc();
        verify(evaluationCycleRepository, never()).findAll();
    }

    @Test
    void hrManagerListsEmptyCycles() {
        givenRoles(RoleType.HR_MANAGER);
        when(evaluationCycleRepository
                .findAllByDeletedAtIsNullOrderByStartDateDescEvaluationCycleIdDesc())
                .thenReturn(List.of());

        List<EvaluationCycleResponse> result = service.getCycles();

        assertEquals(List.of(), result);
    }

    @Test
    void employeeCannotListCycles() {
        givenRoles(RoleType.EMPLOYEE);

        assertError(EvaluationErrorCode.EVALUATION_ACCESS_DENIED, service::getCycles);
        verify(evaluationCycleRepository, never())
                .findAllByDeletedAtIsNullOrderByStartDateDescEvaluationCycleIdDesc();
    }

    @Test
    void systemAdminAloneCannotListCycles() {
        givenRoles(RoleType.SYSTEM_ADMIN);

        assertError(EvaluationErrorCode.EVALUATION_ACCESS_DENIED, service::getCycles);
        verify(evaluationCycleRepository, never())
                .findAllByDeletedAtIsNullOrderByStartDateDescEvaluationCycleIdDesc();
    }

    @Test
    void createRejectsStartDateAfterEndDate() {
        givenRoles(RoleType.HR_MANAGER);

        assertError(
                EvaluationErrorCode.EVALUATION_CYCLE_INVALID_DATE,
                () -> service.create(createRequest(
                        TODAY.plusDays(5),
                        TODAY.plusDays(4),
                        TODAY.plusDays(6))));
    }

    @Test
    void createRejectsPublishDateBeforeStartDate() {
        givenRoles(RoleType.HR_MANAGER);

        assertError(
                EvaluationErrorCode.EVALUATION_CYCLE_INVALID_DATE,
                () -> service.create(createRequest(
                        TODAY.plusDays(2),
                        TODAY.plusDays(5),
                        TODAY.plusDays(1))));
    }

    @Test
    void plannedCycleAllowsAllFieldsToChange() {
        givenRoles(RoleType.HR_MANAGER);
        EvaluationCycle cycle = cycle(TODAY.plusDays(3), TODAY.plusDays(10));
        when(evaluationCycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        EvaluationCycleUpdateRequest request = new EvaluationCycleUpdateRequest(
                "Changed",
                TODAY.plusDays(4),
                TODAY.plusDays(12),
                TODAY.plusDays(13));

        service.update(1L, request);

        assertEquals("Changed", cycle.getCycleName());
        assertEquals(request.startDate(), cycle.getStartDate());
        assertEquals(request.endDate(), cycle.getEndDate());
        assertEquals(request.plannedPublishDate(), cycle.getPlannedPublishDate());
    }

    @Test
    void openCycleAllowsNameToChange() {
        givenRoles(RoleType.HR_MANAGER);
        EvaluationCycle cycle = cycle(TODAY.minusDays(1), TODAY.plusDays(5));
        when(evaluationCycleRepository.findById(1L)).thenReturn(Optional.of(cycle));

        service.update(1L, updateRequest(cycle, "Changed", cycle.getPlannedPublishDate()));

        assertEquals("Changed", cycle.getCycleName());
    }

    @Test
    void openCycleAllowsPublishDateToChange() {
        givenRoles(RoleType.HR_MANAGER);
        EvaluationCycle cycle = cycle(TODAY.minusDays(1), TODAY.plusDays(5));
        when(evaluationCycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        LocalDate changedPublishDate = TODAY.plusDays(10);

        service.update(1L, updateRequest(cycle, cycle.getCycleName(), changedPublishDate));

        assertEquals(changedPublishDate, cycle.getPlannedPublishDate());
    }

    @Test
    void openCycleRejectsStartDateChange() {
        givenRoles(RoleType.HR_MANAGER);
        EvaluationCycle cycle = cycle(TODAY.minusDays(1), TODAY.plusDays(5));
        when(evaluationCycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        EvaluationCycleUpdateRequest request = new EvaluationCycleUpdateRequest(
                cycle.getCycleName(),
                cycle.getStartDate().minusDays(1),
                cycle.getEndDate(),
                cycle.getPlannedPublishDate());

        assertError(
                EvaluationErrorCode.EVALUATION_CYCLE_NOT_EDITABLE,
                () -> service.update(1L, request));
    }

    @Test
    void openCycleRejectsEndDateChange() {
        givenRoles(RoleType.HR_MANAGER);
        EvaluationCycle cycle = cycle(TODAY.minusDays(1), TODAY.plusDays(5));
        when(evaluationCycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        EvaluationCycleUpdateRequest request = new EvaluationCycleUpdateRequest(
                cycle.getCycleName(),
                cycle.getStartDate(),
                cycle.getEndDate().plusDays(1),
                cycle.getPlannedPublishDate());

        assertError(
                EvaluationErrorCode.EVALUATION_CYCLE_NOT_EDITABLE,
                () -> service.update(1L, request));
    }

    @Test
    void closedCycleRejectsEveryUpdate() {
        givenRoles(RoleType.HR_MANAGER);
        EvaluationCycle cycle = cycle(TODAY.minusDays(10), TODAY.minusDays(1));
        when(evaluationCycleRepository.findById(1L)).thenReturn(Optional.of(cycle));

        assertError(
                EvaluationErrorCode.EVALUATION_CYCLE_NOT_EDITABLE,
                () -> service.update(1L, updateRequest(
                        cycle,
                        "Changed",
                        cycle.getPlannedPublishDate())));
    }

    @Test
    void missingCycleIsReportedForGetAndUpdate() {
        givenRoles(RoleType.HR_MANAGER);
        when(evaluationCycleRepository.findById(99L)).thenReturn(Optional.empty());

        assertError(
                EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND,
                () -> service.getById(99L));
        assertError(
                EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND,
                () -> service.update(99L, new EvaluationCycleUpdateRequest(
                        "Cycle",
                        TODAY.plusDays(1),
                        TODAY.plusDays(2),
                        TODAY.plusDays(3))));
    }

    @Test
    void dateBeforeStartIsPlanned() {
        givenRoles(RoleType.HR_MANAGER);
        EvaluationCycle cycle = cycle(TODAY.plusDays(1), TODAY.plusDays(5));
        when(evaluationCycleRepository.findById(1L)).thenReturn(Optional.of(cycle));

        assertEquals(EvaluationCycleStatus.PLANNED, service.getCurrentStatus(1L));
    }

    @Test
    void dateFromStartThroughEndIsOpen() {
        givenRoles(RoleType.HR_MANAGER);
        EvaluationCycle startsToday = cycle(TODAY, TODAY.plusDays(5));
        EvaluationCycle endsToday = cycle(TODAY.minusDays(5), TODAY);
        when(evaluationCycleRepository.findById(1L)).thenReturn(Optional.of(startsToday));
        when(evaluationCycleRepository.findById(2L)).thenReturn(Optional.of(endsToday));

        assertEquals(EvaluationCycleStatus.OPEN, service.getCurrentStatus(1L));
        assertEquals(EvaluationCycleStatus.OPEN, service.getCurrentStatus(2L));
    }

    @Test
    void dateAfterEndIsClosed() {
        givenRoles(RoleType.HR_MANAGER);
        EvaluationCycle cycle = cycle(TODAY.minusDays(5), TODAY.minusDays(1));
        when(evaluationCycleRepository.findById(1L)).thenReturn(Optional.of(cycle));

        assertEquals(EvaluationCycleStatus.CLOSED, service.getCurrentStatus(1L));
    }

    private void givenRoles(RoleType... roles) {
        CurrentUserContext currentUser = new CurrentUserContext(
                10L,
                20L,
                Set.of(roles),
                30L,
                1,
                null);
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
    }

    private EvaluationCycleCreateRequest validCreateRequest() {
        return createRequest(TODAY.plusDays(1), TODAY.plusDays(5), TODAY.plusDays(6));
    }

    private EvaluationCycleCreateRequest createRequest(
            LocalDate startDate,
            LocalDate endDate,
            LocalDate plannedPublishDate
    ) {
        return new EvaluationCycleCreateRequest(
                "Cycle",
                startDate,
                endDate,
                plannedPublishDate);
    }

    private EvaluationCycle cycle(LocalDate startDate, LocalDate endDate) {
        return new EvaluationCycle(
                "Cycle",
                startDate,
                endDate,
                endDate.plusDays(1),
                EvaluationCycleStatus.PLANNED,
                10L);
    }

    private EvaluationCycleUpdateRequest updateRequest(
            EvaluationCycle cycle,
            String cycleName,
            LocalDate plannedPublishDate
    ) {
        return new EvaluationCycleUpdateRequest(
                cycleName,
                cycle.getStartDate(),
                cycle.getEndDate(),
                plannedPublishDate);
    }

    private void assertError(EvaluationErrorCode expected, Runnable action) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertSame(expected, exception.getErrorCode());
    }
}
