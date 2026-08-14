package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationProgressStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@ExtendWith(MockitoExtension.class)
class EvaluationProgressServiceImplTest {

    private static final Long CYCLE_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 14);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC);

    @Mock private EvaluationRepository evaluationRepository;
    @Mock private EvaluationCycleRepository cycleRepository;
    @Mock private OrganizationQueryService organizationQueryService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private EvaluationCycle cycle;

    private EvaluationProgressServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EvaluationProgressServiceImpl(
                evaluationRepository, cycleRepository, organizationQueryService,
                currentUserProvider, CLOCK);
        givenRoles(RoleType.HR_MANAGER);
        lenient().when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
        lenient().when(cycle.getEvaluationCycleId()).thenReturn(CYCLE_ID);
        lenient().when(cycle.getCycleName()).thenReturn("2026 Review");
        lenient().when(cycle.getStartDate()).thenReturn(TODAY.minusDays(1));
        lenient().when(cycle.getEndDate()).thenReturn(TODAY.plusDays(1));
        lenient().when(evaluationRepository.findByEvaluationCycleId(CYCLE_ID)).thenReturn(List.of());
        lenient().when(organizationQueryService.findEmployeeSummaries(anyList())).thenReturn(List.of());
    }

    @Test
    void hrManagerReadsCycleProgress() {
        assertEquals(CYCLE_ID, service.getCycleProgress(CYCLE_ID).cycleId());
    }

    @Test
    void hrManagerWithSystemAdminReadsCycleProgress() {
        givenRoles(RoleType.HR_MANAGER, RoleType.SYSTEM_ADMIN);
        assertEquals(CYCLE_ID, service.getCycleProgress(CYCLE_ID).cycleId());
    }

    @ParameterizedTest
    @MethodSource("deniedRoles")
    void roleWithoutHrManagerIsDenied(Set<RoleType> roles) {
        givenRoles(roles.toArray(RoleType[]::new));
        assertError(EvaluationErrorCode.EVALUATION_ACCESS_DENIED,
                () -> service.getCycleProgress(CYCLE_ID));
    }

    static List<Arguments> deniedRoles() {
        return List.of(
                Arguments.of(Set.of(RoleType.SYSTEM_ADMIN)),
                Arguments.of(Set.of(RoleType.MANAGER)),
                Arguments.of(Set.of(RoleType.EMPLOYEE)));
    }

    @Test
    void missingCycleIsReported() {
        when(cycleRepository.findById(99L)).thenReturn(Optional.empty());
        assertError(EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND,
                () -> service.getCycleProgress(99L));
    }

    @ParameterizedTest
    @MethodSource("cycleDates")
    void currentCycleStatusUsesDatesNotSnapshot(
            LocalDate startDate,
            LocalDate endDate,
            EvaluationCycleStatus expected
    ) {
        when(cycle.getStartDate()).thenReturn(startDate);
        when(cycle.getEndDate()).thenReturn(endDate);
        assertEquals(expected, service.getCycleProgress(CYCLE_ID).currentCycleStatus());
        verify(cycle, never()).getCycleStatus();
    }

    static List<Arguments> cycleDates() {
        return List.of(
                Arguments.of(TODAY.plusDays(1), TODAY.plusDays(2), EvaluationCycleStatus.PLANNED),
                Arguments.of(TODAY, TODAY, EvaluationCycleStatus.OPEN),
                Arguments.of(TODAY.minusDays(2), TODAY.minusDays(1), EvaluationCycleStatus.CLOSED));
    }

    @Test
    void progressMappingCountsEachTypeAndKeepsOriginalStatuses() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 8, 13, 9, 30);
        List<Evaluation> evaluations = List.of(
                evaluation(10L, 100L, EvaluationType.SELF, EvaluationStatus.DRAFT, null, null),
                evaluation(11L, 100L, EvaluationType.MANAGER, EvaluationStatus.DRAFT,
                        LocalDateTime.of(2026, 8, 12, 1, 0), null),
                evaluation(12L, 101L, EvaluationType.SELF, EvaluationStatus.RETURNED, null, submittedAt),
                evaluation(13L, 101L, EvaluationType.MANAGER, EvaluationStatus.SUBMITTED, null, submittedAt),
                evaluation(14L, 102L, EvaluationType.SELF, EvaluationStatus.PUBLISHED, null, submittedAt),
                evaluation(15L, 102L, EvaluationType.MANAGER, EvaluationStatus.PUBLISHED, null, submittedAt));
        when(evaluationRepository.findByEvaluationCycleId(CYCLE_ID)).thenReturn(evaluations);
        when(organizationQueryService.findEmployeeSummaries(List.of(100L, 101L, 102L)))
                .thenReturn(List.of(summary(100L), summary(101L), summary(102L)));

        var response = service.getCycleProgress(CYCLE_ID);

        assertEquals(3, response.totalTargetCount());
        assertEquals(1, response.selfSummary().notStartedCount());
        assertEquals(1, response.selfSummary().inProgressCount());
        assertEquals(1, response.selfSummary().submittedCount());
        assertEquals(0, response.managerSummary().notStartedCount());
        assertEquals(1, response.managerSummary().inProgressCount());
        assertEquals(2, response.managerSummary().submittedCount());
        assertEquals(EvaluationProgressStatus.NOT_STARTED,
                response.employees().get(0).selfEvaluation().progressStatus());
        assertEquals(EvaluationProgressStatus.IN_PROGRESS,
                response.employees().get(0).managerEvaluation().progressStatus());
        assertEquals(EvaluationStatus.PUBLISHED,
                response.employees().get(2).managerEvaluation().evaluationStatus());
        assertEquals(submittedAt, response.employees().get(2).managerEvaluation().submittedAt());
    }

    @Test
    void employeeInformationIsLoadedOnceForDistinctTargets() {
        Evaluation self = evaluation(
                10L, 100L, EvaluationType.SELF, EvaluationStatus.DRAFT, null, null);
        Evaluation manager = evaluation(
                11L, 100L, EvaluationType.MANAGER, EvaluationStatus.DRAFT, null, null);
        when(evaluationRepository.findByEvaluationCycleId(CYCLE_ID))
                .thenReturn(List.of(self, manager));
        EmployeeSummary summary = summary(100L);
        when(organizationQueryService.findEmployeeSummaries(List.of(100L))).thenReturn(List.of(summary));

        var employee = service.getCycleProgress(CYCLE_ID).employees().get(0);

        assertSame(summary, employee.employee());
        assertEquals("Employee 100", employee.employee().employeeName());
        assertEquals("Department", employee.employee().departmentName());
        assertEquals("Grade", employee.employee().jobGradeName());
        verify(organizationQueryService).findEmployeeSummaries(List.of(100L));
    }

    @Test
    void missingEvaluationTypeIsRepresentedAsNull() {
        Evaluation self = evaluation(
                10L, 100L, EvaluationType.SELF, EvaluationStatus.DRAFT, null, null);
        when(evaluationRepository.findByEvaluationCycleId(CYCLE_ID)).thenReturn(List.of(self));
        when(organizationQueryService.findEmployeeSummaries(List.of(100L)))
                .thenReturn(List.of(summary(100L)));

        var employee = service.getCycleProgress(CYCLE_ID).employees().get(0);

        assertEquals(10L, employee.selfEvaluation().evaluationId());
        assertNull(employee.managerEvaluation());
    }

    @Test
    void emptyCycleReturnsZeroCountsAndEmptyEmployees() {
        var response = service.getCycleProgress(CYCLE_ID);

        assertEquals(0, response.totalTargetCount());
        assertEquals(0, response.selfSummary().notStartedCount());
        assertEquals(0, response.managerSummary().submittedCount());
        assertEquals(List.of(), response.employees());
    }

    @Test
    void missingEmployeeSummaryIsReported() {
        Evaluation self = evaluation(
                10L, 100L, EvaluationType.SELF, EvaluationStatus.DRAFT, null, null);
        when(evaluationRepository.findByEvaluationCycleId(CYCLE_ID)).thenReturn(List.of(self));
        assertError(EvaluationErrorCode.EVALUATION_TARGET_INVALID,
                () -> service.getCycleProgress(CYCLE_ID));
    }

    @Test
    void repositoryQueryIsRestrictedToRequestedCycle() {
        service.getCycleProgress(CYCLE_ID);
        verify(evaluationRepository).findByEvaluationCycleId(CYCLE_ID);
    }

    private Evaluation evaluation(
            Long evaluationId,
            Long targetId,
            EvaluationType type,
            EvaluationStatus status,
            LocalDateTime lastDraftSavedAt,
            LocalDateTime submittedAt
    ) {
        Evaluation evaluation = mock(Evaluation.class);
        lenient().when(evaluation.getEvaluationId()).thenReturn(evaluationId);
        lenient().when(evaluation.getTargetEmployeeId()).thenReturn(targetId);
        lenient().when(evaluation.getEvaluationType()).thenReturn(type);
        lenient().when(evaluation.getEvaluationStatus()).thenReturn(status);
        lenient().when(evaluation.getLastDraftSavedAt()).thenReturn(lastDraftSavedAt);
        lenient().when(evaluation.getSubmittedAt()).thenReturn(submittedAt);
        return evaluation;
    }

    private EmployeeSummary summary(Long employeeId) {
        return new EmployeeSummary(
                employeeId, "Employee " + employeeId, 20L, "Department", 30L, "Grade");
    }

    private void givenRoles(RoleType... roles) {
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUserContext(
                10L, 20L, Set.of(roles), 30L, 1, null));
    }

    private void assertError(EvaluationErrorCode expected, Runnable action) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertSame(expected, exception.getErrorCode());
    }
}
