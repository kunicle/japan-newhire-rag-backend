package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
import com.teamproject.japan_newhire_rag_backend.evaluation.Evaluation;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationItemRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationTemplate;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationTemplateRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationAssignmentRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@ExtendWith(MockitoExtension.class)
class EvaluationAssignmentServiceImplTest {

    private static final Long CYCLE_ID = 1L;
    private static final Long TARGET_ID = 100L;
    private static final Long MANAGER_ID = 200L;
    private static final Long SELF_TEMPLATE_ID = 10L;
    private static final Long MANAGER_TEMPLATE_ID = 20L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 12);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private EvaluationCycleRepository cycleRepository;
    @Mock
    private EvaluationTemplateRepository templateRepository;
    @Mock
    private EvaluationItemRepository itemRepository;
    @Mock
    private OrganizationQueryService organizationQueryService;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private EvaluationTemplate selfTemplate;
    @Mock
    private EvaluationTemplate managerTemplate;

    private EvaluationAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EvaluationAssignmentServiceImpl(
                evaluationRepository, cycleRepository, templateRepository, itemRepository,
                organizationQueryService, currentUserProvider, FIXED_CLOCK);
        givenDefaultValidAssignment();
    }

    @Test
    void hrManagerCreatesOneSelfAndOneManagerEvaluation() {
        service.assign(request());

        List<Evaluation> saved = capturedEvaluations();
        assertEquals(2, saved.size());
        assertEquals(List.of(EvaluationType.SELF, EvaluationType.MANAGER),
                saved.stream().map(Evaluation::getEvaluationType).toList());
    }

    @Test
    void systemAdminAloneCannotAssign() {
        givenRoles(RoleType.SYSTEM_ADMIN);

        assertError(EvaluationErrorCode.EVALUATION_ACCESS_DENIED,
                () -> service.assign(request()));
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void employeeCannotAssign() {
        givenRoles(RoleType.EMPLOYEE);

        assertError(EvaluationErrorCode.EVALUATION_ACCESS_DENIED,
                () -> service.assign(request()));
    }

    @Test
    void missingCycleIsReported() {
        when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.empty());

        assertError(EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND,
                () -> service.assign(request()));
    }

    @Test
    void openCycleRejectsAssignment() {
        when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(openCycle()));

        assertError(EvaluationErrorCode.EVALUATION_CYCLE_NOT_ASSIGNABLE,
                () -> service.assign(request()));
    }

    @Test
    void closedCycleRejectsAssignment() {
        when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(closedCycle()));

        assertError(EvaluationErrorCode.EVALUATION_CYCLE_NOT_ASSIGNABLE,
                () -> service.assign(request()));
    }

    @Test
    void invalidTargetIsRejected() {
        when(organizationQueryService.isValidEmployee(TARGET_ID)).thenReturn(false);

        assertError(EvaluationErrorCode.EVALUATION_TARGET_INVALID,
                () -> service.assign(request()));
    }

    @Test
    void missingDirectManagerIsRejected() {
        when(organizationQueryService.findDirectManagerEmployeeId(TARGET_ID)).thenReturn(null);

        assertError(EvaluationErrorCode.EVALUATION_MANAGER_RELATION_INVALID,
                () -> service.assign(request()));
    }

    @Test
    void invalidManagerIsRejected() {
        when(organizationQueryService.isValidEmployee(MANAGER_ID)).thenReturn(false);

        assertError(EvaluationErrorCode.EVALUATION_EVALUATOR_INVALID,
                () -> service.assign(request()));
    }

    @Test
    void missingActiveSelfTemplateIsRejected() {
        when(templateRepository.findByEvaluationCycleIdAndEvaluationTypeAndIsActiveTrue(
                CYCLE_ID, EvaluationType.SELF)).thenReturn(Optional.empty());

        assertError(EvaluationErrorCode.EVALUATION_TEMPLATE_NOT_READY,
                () -> service.assign(request()));
    }

    @Test
    void missingActiveManagerTemplateIsRejected() {
        when(templateRepository.findByEvaluationCycleIdAndEvaluationTypeAndIsActiveTrue(
                CYCLE_ID, EvaluationType.MANAGER)).thenReturn(Optional.empty());

        assertError(EvaluationErrorCode.EVALUATION_TEMPLATE_NOT_READY,
                () -> service.assign(request()));
    }

    @Test
    void selfTemplateWithoutItemsIsRejected() {
        when(itemRepository.existsByEvaluationTemplateId(SELF_TEMPLATE_ID)).thenReturn(false);

        assertError(EvaluationErrorCode.EVALUATION_TEMPLATE_NOT_READY,
                () -> service.assign(request()));
    }

    @Test
    void managerTemplateWithoutItemsIsRejected() {
        when(itemRepository.existsByEvaluationTemplateId(MANAGER_TEMPLATE_ID)).thenReturn(false);

        assertError(EvaluationErrorCode.EVALUATION_TEMPLATE_NOT_READY,
                () -> service.assign(request()));
    }

    @Test
    void existingSelfRejectsWholeAssignment() {
        when(evaluationRepository
                .existsByEvaluationCycleIdAndTargetEmployeeIdAndEvaluationType(
                        CYCLE_ID, TARGET_ID, EvaluationType.SELF)).thenReturn(true);

        assertError(EvaluationErrorCode.EVALUATION_DUPLICATE_ASSIGNMENT,
                () -> service.assign(request()));
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void existingManagerRejectsWholeAssignment() {
        when(evaluationRepository
                .existsByEvaluationCycleIdAndTargetEmployeeIdAndEvaluationType(
                        CYCLE_ID, TARGET_ID, EvaluationType.MANAGER)).thenReturn(true);

        assertError(EvaluationErrorCode.EVALUATION_DUPLICATE_ASSIGNMENT,
                () -> service.assign(request()));
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void bothEvaluationsStartAsDraftWithNoScoreOrTimestamps() {
        service.assign(request());

        for (Evaluation evaluation : capturedEvaluations()) {
            assertEquals(EvaluationStatus.DRAFT, evaluation.getEvaluationStatus());
            assertNull(evaluation.getTotalScore());
            assertNull(evaluation.getSubmittedAt());
            assertNull(evaluation.getPublishedAt());
        }
    }

    @Test
    void selfUsesTargetAsEvaluator() {
        service.assign(request());

        Evaluation self = capturedEvaluations().get(0);
        assertEquals(TARGET_ID, self.getTargetEmployeeId());
        assertEquals(TARGET_ID, self.getEvaluatorEmployeeId());
    }

    @Test
    void managerUsesDifferentEvaluator() {
        service.assign(request());

        Evaluation manager = capturedEvaluations().get(1);
        assertEquals(TARGET_ID, manager.getTargetEmployeeId());
        assertEquals(MANAGER_ID, manager.getEvaluatorEmployeeId());
    }

    @Test
    void selfAndManagerUseMatchingTemplates() {
        service.assign(request());

        List<Evaluation> saved = capturedEvaluations();
        assertEquals(SELF_TEMPLATE_ID, saved.get(0).getEvaluationTemplateId());
        assertEquals(EvaluationType.SELF, saved.get(0).getEvaluationType());
        assertEquals(MANAGER_TEMPLATE_ID, saved.get(1).getEvaluationTemplateId());
        assertEquals(EvaluationType.MANAGER, saved.get(1).getEvaluationType());
    }

    @Test
    void managerComesFromDirectManagerPublicApi() {
        service.assign(request());

        verify(organizationQueryService).findDirectManagerEmployeeId(TARGET_ID);
        verify(organizationQueryService).isValidEmployee(MANAGER_ID);
    }

    @Test
    void secondSaveFailurePropagatesFromTransactionalMethod() {
        RuntimeException failure = new RuntimeException("second save failed");
        when(evaluationRepository.save(any(Evaluation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(failure);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> service.assign(request()));

        assertSame(failure, thrown);
        verify(evaluationRepository, times(2)).save(any(Evaluation.class));
    }

    @Test
    void multipleDirectManagersAreConvertedToBusinessError() {
        when(organizationQueryService.findDirectManagerEmployeeId(TARGET_ID))
                .thenThrow(new IllegalStateException("multiple direct managers"));

        assertError(EvaluationErrorCode.EVALUATION_MANAGER_RELATION_INVALID,
                () -> service.assign(request()));
    }

    @Test
    void legacyManagedEmployeeApiIsNeverUsed() {
        service.assign(request());

        verify(organizationQueryService, never()).isManagedEmployee(any(), any());
    }

    @Test
    void assignMethodDefinesTransactionBoundary() throws NoSuchMethodException {
        Transactional transactional = EvaluationAssignmentServiceImpl.class
                .getMethod("assign", EvaluationAssignmentRequest.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
    }

    private void givenDefaultValidAssignment() {
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(context(RoleType.HR_MANAGER));
        lenient().when(cycleRepository.findById(CYCLE_ID))
                .thenReturn(Optional.of(plannedCycle()));
        lenient().when(organizationQueryService.isValidEmployee(TARGET_ID)).thenReturn(true);
        lenient().when(organizationQueryService.findDirectManagerEmployeeId(TARGET_ID))
                .thenReturn(MANAGER_ID);
        lenient().when(organizationQueryService.isValidEmployee(MANAGER_ID)).thenReturn(true);
        lenient().when(selfTemplate.getEvaluationTemplateId()).thenReturn(SELF_TEMPLATE_ID);
        lenient().when(managerTemplate.getEvaluationTemplateId()).thenReturn(MANAGER_TEMPLATE_ID);
        lenient().when(templateRepository
                .findByEvaluationCycleIdAndEvaluationTypeAndIsActiveTrue(
                        CYCLE_ID, EvaluationType.SELF)).thenReturn(Optional.of(selfTemplate));
        lenient().when(templateRepository
                .findByEvaluationCycleIdAndEvaluationTypeAndIsActiveTrue(
                        CYCLE_ID, EvaluationType.MANAGER)).thenReturn(Optional.of(managerTemplate));
        lenient().when(itemRepository.existsByEvaluationTemplateId(SELF_TEMPLATE_ID))
                .thenReturn(true);
        lenient().when(itemRepository.existsByEvaluationTemplateId(MANAGER_TEMPLATE_ID))
                .thenReturn(true);
        lenient().when(evaluationRepository
                .existsByEvaluationCycleIdAndTargetEmployeeIdAndEvaluationType(
                        CYCLE_ID, TARGET_ID, EvaluationType.SELF)).thenReturn(false);
        lenient().when(evaluationRepository
                .existsByEvaluationCycleIdAndTargetEmployeeIdAndEvaluationType(
                        CYCLE_ID, TARGET_ID, EvaluationType.MANAGER)).thenReturn(false);
        lenient().when(evaluationRepository.save(any(Evaluation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenRoles(RoleType... roles) {
        when(currentUserProvider.getCurrentUser()).thenReturn(context(roles));
    }

    private CurrentUserContext context(RoleType... roles) {
        return new CurrentUserContext(1L, 2L, Set.of(roles), 3L, 1, null);
    }

    private EvaluationAssignmentRequest request() {
        return new EvaluationAssignmentRequest(CYCLE_ID, TARGET_ID);
    }

    private List<Evaluation> capturedEvaluations() {
        ArgumentCaptor<Evaluation> captor = ArgumentCaptor.forClass(Evaluation.class);
        verify(evaluationRepository, times(2)).save(captor.capture());
        return captor.getAllValues();
    }

    private EvaluationCycle plannedCycle() {
        return cycle(TODAY.plusDays(1), TODAY.plusDays(10));
    }

    private EvaluationCycle openCycle() {
        return cycle(TODAY.minusDays(1), TODAY.plusDays(10));
    }

    private EvaluationCycle closedCycle() {
        return cycle(TODAY.minusDays(10), TODAY.minusDays(1));
    }

    private EvaluationCycle cycle(LocalDate startDate, LocalDate endDate) {
        return new EvaluationCycle(
                "Cycle", startDate, endDate, endDate.plusDays(1),
                EvaluationCycleStatus.PLANNED, 1L);
    }

    private void assertError(EvaluationErrorCode expected, Runnable action) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertSame(expected, exception.getErrorCode());
    }
}
