package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationProgressResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@ExtendWith(MockitoExtension.class)
class ManagerEvaluationProgressServiceImplTest {

    private static final Long CYCLE_ID = 1L;
    private static final Long MANAGER_EMPLOYEE_ID = 10L;

    @Mock private EvaluationRepository evaluationRepository;
    @Mock private EvaluationCycleRepository cycleRepository;
    @Mock private OrganizationQueryService organizationQueryService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private EvaluationCycle cycle;

    private ManagerEvaluationProgressServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ManagerEvaluationProgressServiceImpl(
                evaluationRepository, cycleRepository,
                organizationQueryService, currentUserProvider);
        lenient().when(currentUserProvider.getCurrentUser())
                .thenReturn(user(Set.of(RoleType.MANAGER), MANAGER_EMPLOYEE_ID));
        lenient().when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
        lenient().when(cycle.getEvaluationCycleId()).thenReturn(CYCLE_ID);
        lenient().when(cycle.getCycleName()).thenReturn("2026 Review");
        lenient().when(organizationQueryService.findManagedEmployeeIds(MANAGER_EMPLOYEE_ID))
                .thenReturn(List.of());
        lenient().when(evaluationRepository.findByEvaluationCycleId(CYCLE_ID))
                .thenReturn(List.of());
        lenient().when(organizationQueryService.findEmployeeSummaries(Set.of()))
                .thenReturn(List.of());
    }

    @Test
    void managerCanReadManagedProgress() {
        ManagerEvaluationProgressResponse response = service.getManagedProgress(CYCLE_ID);

        assertEquals(CYCLE_ID, response.cycleId());
        assertEquals("2026 Review", response.cycleName());
        assertEquals(new BigDecimal("0.0"), response.completionRate());
    }

    @Test
    void managerWithOtherRoleIsAllowed() {
        when(currentUserProvider.getCurrentUser()).thenReturn(user(
                Set.of(RoleType.MANAGER, RoleType.HR_MANAGER), MANAGER_EMPLOYEE_ID));

        service.getManagedProgress(CYCLE_ID);

        verify(cycleRepository).findById(CYCLE_ID);
    }

    @ParameterizedTest
    @EnumSource(value = RoleType.class, names = {"EMPLOYEE", "SYSTEM_ADMIN", "HR_MANAGER"})
    void nonManagerRoleIsRejected(RoleType role) {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(user(Set.of(role), MANAGER_EMPLOYEE_ID));

        assertError(EvaluationErrorCode.EVALUATION_ACCESS_DENIED,
                () -> service.getManagedProgress(CYCLE_ID));
        verify(cycleRepository, never()).findById(CYCLE_ID);
    }

    @Test
    void managerWithoutEmployeeIdIsRejected() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(user(Set.of(RoleType.MANAGER), null));

        assertError(EvaluationErrorCode.EVALUATION_ACCESS_DENIED,
                () -> service.getManagedProgress(CYCLE_ID));
    }

    @Test
    void missingCycleIsRejected() {
        when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.empty());

        assertError(EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND,
                () -> service.getManagedProgress(CYCLE_ID));
        verify(organizationQueryService, never()).findManagedEmployeeIds(MANAGER_EMPLOYEE_ID);
    }

    @Test
    void includesOnlyIntersectionOfManagedAndAssignedEmployees() {
        Evaluation managedSelf = evaluation(101L, 20L, EvaluationType.SELF,
                EvaluationStatus.SUBMITTED);
        Evaluation managedManager = evaluation(102L, 20L, EvaluationType.MANAGER,
                EvaluationStatus.PUBLISHED);
        Evaluation unmanaged = evaluation(103L, 30L, EvaluationType.SELF,
                EvaluationStatus.SUBMITTED);
        when(organizationQueryService.findManagedEmployeeIds(MANAGER_EMPLOYEE_ID))
                .thenReturn(List.of(20L, 20L, 40L));
        when(evaluationRepository.findByEvaluationCycleId(CYCLE_ID))
                .thenReturn(List.of(managedSelf, unmanaged, managedManager));
        when(organizationQueryService.findEmployeeSummaries(Set.of(20L)))
                .thenReturn(List.of(summary(20L)));

        ManagerEvaluationProgressResponse response = service.getManagedProgress(CYCLE_ID);

        assertEquals(1, response.totalEmployees());
        assertEquals(1, response.completedEmployees());
        assertEquals(1, response.selfCompletedCount());
        assertEquals(1, response.managerCompletedCount());
        assertEquals(20L, response.employees().get(0).employee().employeeId());
        assertTrue(response.employees().get(0).completed());
        verify(organizationQueryService).findEmployeeSummaries(Set.of(20L));
    }

    @Test
    void mapsEmployeeDetailsAndSortsByEmployeeId() {
        Evaluation high = evaluation(101L, 30L, EvaluationType.SELF,
                EvaluationStatus.DRAFT);
        Evaluation low = evaluation(102L, 20L, EvaluationType.MANAGER,
                EvaluationStatus.RETURNED);
        when(organizationQueryService.findManagedEmployeeIds(MANAGER_EMPLOYEE_ID))
                .thenReturn(List.of(30L, 20L));
        when(evaluationRepository.findByEvaluationCycleId(CYCLE_ID))
                .thenReturn(List.of(high, low));
        when(organizationQueryService.findEmployeeSummaries(Set.of(20L, 30L)))
                .thenReturn(List.of(summary(30L), summary(20L)));

        ManagerEvaluationProgressResponse response = service.getManagedProgress(CYCLE_ID);

        assertEquals(List.of(20L, 30L), response.employees().stream()
                .map(employee -> employee.employee().employeeId()).toList());
        assertEquals("Employee 20", response.employees().get(0).employee().employeeName());
        assertEquals("Department", response.employees().get(0).employee().departmentName());
        assertEquals("Grade", response.employees().get(0).employee().jobGradeName());
        assertNull(response.employees().get(0).selfEvaluation());
        assertEquals(EvaluationStatus.RETURNED,
                response.employees().get(0).managerEvaluation().evaluationStatus());
    }

    @Test
    void calculatesStatusCountsAndEmployeeCompletion() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 8, 18, 9, 30);
        Evaluation completedSelf = evaluation(101L, 20L, EvaluationType.SELF,
                EvaluationStatus.SUBMITTED, submittedAt);
        Evaluation completedManager = evaluation(102L, 20L, EvaluationType.MANAGER,
                EvaluationStatus.PUBLISHED, submittedAt);
        Evaluation draftSelf = evaluation(103L, 30L, EvaluationType.SELF,
                EvaluationStatus.DRAFT);
        Evaluation submittedManager = evaluation(104L, 30L, EvaluationType.MANAGER,
                EvaluationStatus.SUBMITTED);
        Evaluation returnedSelf = evaluation(105L, 40L, EvaluationType.SELF,
                EvaluationStatus.RETURNED);
        when(organizationQueryService.findManagedEmployeeIds(MANAGER_EMPLOYEE_ID))
                .thenReturn(List.of(20L, 30L, 40L));
        when(evaluationRepository.findByEvaluationCycleId(CYCLE_ID)).thenReturn(List.of(
                completedSelf, completedManager, draftSelf, submittedManager, returnedSelf));
        when(organizationQueryService.findEmployeeSummaries(Set.of(20L, 30L, 40L)))
                .thenReturn(List.of(summary(20L), summary(30L), summary(40L)));

        ManagerEvaluationProgressResponse response = service.getManagedProgress(CYCLE_ID);

        assertEquals(3, response.totalEmployees());
        assertEquals(1, response.completedEmployees());
        assertEquals(1, response.selfCompletedCount());
        assertEquals(2, response.managerCompletedCount());
        assertEquals(new BigDecimal("33.3"), response.completionRate());
        assertEquals(submittedAt, response.employees().get(0).selfEvaluation().submittedAt());
        assertNull(response.employees().get(2).managerEvaluation());
    }

    @ParameterizedTest
    @CsvSource({
            "0,0,0.0",
            "0,3,0.0",
            "1,3,33.3",
            "2,3,66.7",
            "3,4,75.0",
            "4,4,100.0"
    })
    void calculatesEmployeeCompletionRateWithHalfUp(
            int completed,
            int total,
            String expectedRate
    ) {
        givenCompletionScenario(completed, total);

        ManagerEvaluationProgressResponse response = service.getManagedProgress(CYCLE_ID);

        assertEquals(total, response.totalEmployees());
        assertEquals(completed, response.completedEmployees());
        assertEquals(new BigDecimal(expectedRate), response.completionRate());
    }

    @Test
    void detectsDuplicateEvaluationForSameTargetAndType() {
        Evaluation first = evaluation(101L, 20L, EvaluationType.SELF,
                EvaluationStatus.SUBMITTED);
        Evaluation duplicate = evaluation(102L, 20L, EvaluationType.SELF,
                EvaluationStatus.SUBMITTED);
        when(organizationQueryService.findManagedEmployeeIds(MANAGER_EMPLOYEE_ID))
                .thenReturn(List.of(20L));
        when(evaluationRepository.findByEvaluationCycleId(CYCLE_ID))
                .thenReturn(List.of(first, duplicate));

        assertError(EvaluationErrorCode.EVALUATION_PUBLISH_CONFLICT,
                () -> service.getManagedProgress(CYCLE_ID));
    }

    @Test
    void missingEmployeeSummaryIsRejected() {
        Evaluation evaluation = evaluation(101L, 20L, EvaluationType.SELF,
                EvaluationStatus.SUBMITTED);
        when(organizationQueryService.findManagedEmployeeIds(MANAGER_EMPLOYEE_ID))
                .thenReturn(List.of(20L));
        when(evaluationRepository.findByEvaluationCycleId(CYCLE_ID))
                .thenReturn(List.of(evaluation));
        when(organizationQueryService.findEmployeeSummaries(Set.of(20L)))
                .thenReturn(List.of());

        assertError(EvaluationErrorCode.EVALUATION_TARGET_INVALID,
                () -> service.getManagedProgress(CYCLE_ID));
    }

    @Test
    void isReadOnlyAndDoesNotMutateEvaluation() {
        Evaluation evaluation = evaluation(101L, 20L, EvaluationType.SELF,
                EvaluationStatus.SUBMITTED);
        when(organizationQueryService.findManagedEmployeeIds(MANAGER_EMPLOYEE_ID))
                .thenReturn(List.of(20L));
        when(evaluationRepository.findByEvaluationCycleId(CYCLE_ID))
                .thenReturn(List.of(evaluation));
        when(organizationQueryService.findEmployeeSummaries(Set.of(20L)))
                .thenReturn(List.of(summary(20L)));

        service.getManagedProgress(CYCLE_ID);

        Transactional transactional = ManagerEvaluationProgressServiceImpl.class
                .getAnnotation(Transactional.class);
        assertTrue(transactional.readOnly());
        verify(evaluation, never()).setEvaluationStatus(
                org.mockito.ArgumentMatchers.any());
        verify(evaluation, never()).setTotalScore(org.mockito.ArgumentMatchers.any());
    }

    private void givenCompletionScenario(int completed, int total) {
        List<Long> employeeIds = IntStream.range(0, total)
                .mapToObj(index -> 20L + index)
                .toList();
        List<Evaluation> evaluations = new ArrayList<>();
        for (int index = 0; index < total; index++) {
            Long employeeId = employeeIds.get(index);
            EvaluationStatus managerStatus = index < completed
                    ? EvaluationStatus.SUBMITTED : EvaluationStatus.DRAFT;
            evaluations.add(evaluation(100L + index * 2, employeeId,
                    EvaluationType.SELF, EvaluationStatus.PUBLISHED));
            evaluations.add(evaluation(101L + index * 2, employeeId,
                    EvaluationType.MANAGER, managerStatus));
        }
        List<EmployeeSummary> summaries = employeeIds.stream().map(this::summary).toList();
        when(organizationQueryService.findManagedEmployeeIds(MANAGER_EMPLOYEE_ID))
                .thenReturn(employeeIds);
        when(evaluationRepository.findByEvaluationCycleId(CYCLE_ID))
                .thenReturn(evaluations);
        if (!employeeIds.isEmpty()) {
            when(organizationQueryService.findEmployeeSummaries(Set.copyOf(employeeIds)))
                    .thenReturn(summaries);
        }
    }

    private CurrentUserContext user(Set<RoleType> roles, Long employeeId) {
        return new CurrentUserContext(1L, employeeId, roles, null, null, null);
    }

    private Evaluation evaluation(
            Long evaluationId,
            Long targetEmployeeId,
            EvaluationType type,
            EvaluationStatus status
    ) {
        return evaluation(evaluationId, targetEmployeeId, type, status, null);
    }

    private Evaluation evaluation(
            Long evaluationId,
            Long targetEmployeeId,
            EvaluationType type,
            EvaluationStatus status,
            LocalDateTime submittedAt
    ) {
        Evaluation evaluation = mock(Evaluation.class);
        lenient().when(evaluation.getEvaluationId()).thenReturn(evaluationId);
        when(evaluation.getTargetEmployeeId()).thenReturn(targetEmployeeId);
        lenient().when(evaluation.getEvaluationType()).thenReturn(type);
        lenient().when(evaluation.getEvaluationStatus()).thenReturn(status);
        lenient().when(evaluation.getSubmittedAt()).thenReturn(submittedAt);
        return evaluation;
    }

    private EmployeeSummary summary(Long employeeId) {
        return new EmployeeSummary(
                employeeId, "Employee " + employeeId,
                100L, "Department", 200L, "Grade");
    }

    private void assertError(EvaluationErrorCode expected, Runnable action) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertSame(expected, exception.getErrorCode());
    }
}
