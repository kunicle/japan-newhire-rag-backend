package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationFeedback;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationFeedbackRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationItem;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationItemRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationScore;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationScoreRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;
import com.teamproject.japan_newhire_rag_backend.evaluation.FeedbackType;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationItemDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@ExtendWith(MockitoExtension.class)
class ManagerEvaluationServiceImplTest {

    private static final Long EVALUATION_ID = 1L;
    private static final Long CYCLE_ID = 2L;
    private static final Long TEMPLATE_ID = 3L;
    private static final Long MANAGER_ID = 4L;
    private static final Long TARGET_ID = 5L;
    private static final Long ITEM_ID = 6L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 12);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC);

    @Mock private EvaluationRepository evaluationRepository;
    @Mock private EvaluationCycleRepository cycleRepository;
    @Mock private EvaluationItemRepository itemRepository;
    @Mock private EvaluationScoreRepository scoreRepository;
    @Mock private EvaluationFeedbackRepository feedbackRepository;
    @Mock private OrganizationQueryService organizationQueryService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private Evaluation evaluation;
    @Mock private EvaluationCycle cycle;
    @Mock private EvaluationItem item;

    private ManagerEvaluationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ManagerEvaluationServiceImpl(
                evaluationRepository, cycleRepository, itemRepository, scoreRepository,
                feedbackRepository, organizationQueryService, currentUserProvider, CLOCK);
        givenDefaultState();
    }

    @Test
    void managerReadsAssignedEvaluationsWithBatchTargetSummaries() {
        when(evaluationRepository.findByEvaluatorEmployeeIdAndEvaluationType(
                MANAGER_ID, EvaluationType.MANAGER)).thenReturn(List.of(evaluation));

        var result = service.getMyAssignedEvaluations();

        assertEquals(1, result.size());
        assertEquals(TARGET_ID, result.get(0).targetEmployee().employeeId());
        assertEquals(EvaluationCycleStatus.OPEN, result.get(0).currentCycleStatus());
        verify(organizationQueryService).findEmployeeSummaries(List.of(TARGET_ID));
    }

    @Test
    void listRepositoryRestrictsEvaluatorAndType() {
        service.getMyAssignedEvaluations();
        verify(evaluationRepository).findByEvaluatorEmployeeIdAndEvaluationType(
                MANAGER_ID, EvaluationType.MANAGER);
    }

    @Test
    void managerReadsOwnManagerEvaluationWithTargetAndOrderedItems() {
        EvaluationItem second = item(7L, 2);
        when(itemRepository.findByEvaluationTemplateIdOrderByItemOrderAsc(TEMPLATE_ID))
                .thenReturn(List.of(item, second));

        ManagerEvaluationResponse result = service.getMyManagerEvaluation(EVALUATION_ID);

        assertEquals(TARGET_ID, result.targetEmployee().employeeId());
        assertEquals(List.of(1, 2), result.items().stream().map(i -> i.itemOrder()).toList());
    }

    @Test
    void responseContainsExistingScoreAndFeedback() {
        EvaluationScore score = mock(EvaluationScore.class);
        when(score.getEvaluationItemId()).thenReturn(ITEM_ID);
        when(score.getScore()).thenReturn(new BigDecimal("3.5"));
        when(scoreRepository.findByEvaluationId(EVALUATION_ID)).thenReturn(List.of(score));
        EvaluationFeedback itemFeedback = feedback(ITEM_ID, "item");
        EvaluationFeedback overall = feedback(null, "overall");
        when(feedbackRepository.findByEvaluationIdAndFeedbackType(
                EVALUATION_ID, FeedbackType.ITEM)).thenReturn(List.of(itemFeedback));
        when(feedbackRepository.findByEvaluationIdAndFeedbackType(
                EVALUATION_ID, FeedbackType.OVERALL)).thenReturn(List.of(overall));

        ManagerEvaluationResponse result = service.getMyManagerEvaluation(EVALUATION_ID);

        assertEquals(new BigDecimal("3.5"), result.items().get(0).score());
        assertEquals("item", result.items().get(0).itemFeedback());
        assertEquals("overall", result.overallFeedback());
    }

    @Test
    void missingEvaluationIsRejected() {
        when(evaluationRepository.findById(99L)).thenReturn(Optional.empty());
        assertError(EvaluationErrorCode.EVALUATION_NOT_FOUND,
                () -> service.getMyManagerEvaluation(99L));
    }

    @Test
    void anotherEvaluatorIsRejected() {
        when(evaluation.getEvaluatorEmployeeId()).thenReturn(99L);
        assertOwnerError(() -> service.getMyManagerEvaluation(EVALUATION_ID));
    }

    @Test
    void selfEvaluationIsRejected() {
        when(evaluation.getEvaluationType()).thenReturn(EvaluationType.SELF);
        assertOwnerError(() -> service.getMyManagerEvaluation(EVALUATION_ID));
    }

    @Test
    void userWithoutManagerRoleIsRejected() {
        givenUser(Set.of(RoleType.HR_MANAGER));
        assertOwnerError(() -> service.getMyManagerEvaluation(EVALUATION_ID));
    }

    @Test
    void missingOrChangedDirectManagerIsRejected() {
        when(organizationQueryService.findDirectManagerEmployeeId(TARGET_ID)).thenReturn(null);
        assertRelationError(() -> service.getMyManagerEvaluation(EVALUATION_ID));
        when(organizationQueryService.findDirectManagerEmployeeId(TARGET_ID)).thenReturn(99L);
        assertRelationError(() -> service.getMyManagerEvaluation(EVALUATION_ID));
    }

    @Test
    void duplicateDirectRelationIsConverted() {
        when(organizationQueryService.findDirectManagerEmployeeId(TARGET_ID))
                .thenThrow(new IllegalStateException("duplicate"));
        assertRelationError(() -> service.getMyManagerEvaluation(EVALUATION_ID));
    }

    @Test
    void plannedAndClosedReadSucceed() {
        when(cycle.getStartDate()).thenReturn(TODAY.plusDays(1));
        ManagerEvaluationResponse planned = service.getMyManagerEvaluation(EVALUATION_ID);
        assertEquals(EvaluationCycleStatus.PLANNED, planned.currentCycleStatus());

        when(cycle.getStartDate()).thenReturn(TODAY.minusDays(2));
        when(cycle.getEndDate()).thenReturn(TODAY.minusDays(1));
        ManagerEvaluationResponse closed = service.getMyManagerEvaluation(EVALUATION_ID);
        assertEquals(EvaluationCycleStatus.CLOSED, closed.currentCycleStatus());
    }

    @ParameterizedTest
    @EnumSource(value = EvaluationStatus.class, names = {"SUBMITTED", "RETURNED", "PUBLISHED"})
    void nonDraftReadSucceeds(EvaluationStatus status) {
        when(evaluation.getEvaluationStatus()).thenReturn(status);

        ManagerEvaluationResponse result = service.getMyManagerEvaluation(EVALUATION_ID);

        assertEquals(status, result.evaluationStatus());
    }

    @ParameterizedTest
    @EnumSource(value = EvaluationStatus.class, names = {"SUBMITTED", "RETURNED", "PUBLISHED"})
    void nonDraftSaveIsRejected(EvaluationStatus status) {
        when(evaluation.getEvaluationStatus()).thenReturn(status);
        assertNotWritable(() -> service.saveDraft(EVALUATION_ID, request(null, null)));
    }

    @Test
    void openDraftCanBeSavedAndCurrentRelationIsRechecked() {
        ManagerEvaluationResponse result = service.saveDraft(
                EVALUATION_ID, request(null, null));
        assertEquals(EvaluationStatus.DRAFT, result.evaluationStatus());
        verify(organizationQueryService).findDirectManagerEmployeeId(TARGET_ID);
        verify(evaluation).setLastDraftSavedAt(LocalDateTime.of(2026, 8, 12, 0, 0));
    }

    @Test
    void everySuccessfulDraftSaveRefreshesLastDraftSavedAt() {
        service.saveDraft(EVALUATION_ID, request(null, null));
        service.saveDraft(EVALUATION_ID, request(new BigDecimal("3.5"), "feedback"));

        verify(evaluation, times(2))
                .setLastDraftSavedAt(LocalDateTime.of(2026, 8, 12, 0, 0));
    }

    @Test
    void plannedAndClosedSaveAreRejected() {
        when(cycle.getStartDate()).thenReturn(TODAY.plusDays(1));
        assertNotWritable(() -> service.saveDraft(EVALUATION_ID, request(null, null)));
        when(cycle.getStartDate()).thenReturn(TODAY.minusDays(2));
        when(cycle.getEndDate()).thenReturn(TODAY.minusDays(1));
        assertNotWritable(() -> service.saveDraft(EVALUATION_ID, request(null, null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "1.0", "3.5", "5.0"})
    void validScoresAreInserted(String value) {
        service.saveDraft(EVALUATION_ID, request(new BigDecimal(value), null));
        verify(scoreRepository).save(any(EvaluationScore.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.9", "5.1", "3.55"})
    void invalidScoresAreRejected(String value) {
        assertError(EvaluationErrorCode.EVALUATION_SCORE_INVALID,
                () -> service.saveDraft(EVALUATION_ID,
                        request(new BigDecimal(value), null)));
        verify(scoreRepository, never()).save(any());
    }

    @Test
    void scoreIsUpdatedOrDeleted() {
        EvaluationScore existing = mock(EvaluationScore.class);
        when(scoreRepository.findByEvaluationIdAndEvaluationItemId(EVALUATION_ID, ITEM_ID))
                .thenReturn(Optional.of(existing));
        service.saveDraft(EVALUATION_ID, request(new BigDecimal("4.5"), null));
        verify(existing).setScore(new BigDecimal("4.5"));

        service.saveDraft(EVALUATION_ID, request(null, null));
        verify(scoreRepository).delete(existing);
    }

    @Test
    void nullScoreWithoutRowAndRequiredItemAreAllowed() {
        when(item.getIsRequired()).thenReturn(true);
        service.saveDraft(EVALUATION_ID, request(null, null));
        verify(scoreRepository, never()).save(any());
        verify(scoreRepository, never()).delete(any());
    }

    @Test
    void itemFromAnotherTemplateIsRejectedBeforeWrites() {
        ManagerEvaluationDraftRequest invalid = new ManagerEvaluationDraftRequest(List.of(
                new ManagerEvaluationItemDraftRequest(99L, new BigDecimal("3.5"), "x")), null);
        assertError(EvaluationErrorCode.EVALUATION_ITEM_MISMATCH,
                () -> service.saveDraft(EVALUATION_ID, invalid));
        verify(scoreRepository, never()).save(any());
        verify(feedbackRepository, never()).save(any());
        verify(evaluation, never()).setLastDraftSavedAt(any());
    }

    @Test
    void itemFeedbackIsInsertedInvisibleUpdatedAndDeleted() {
        service.saveDraft(EVALUATION_ID, request(null, "new"));
        ArgumentCaptor<EvaluationFeedback> captor = ArgumentCaptor.forClass(EvaluationFeedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertFalse(captor.getValue().getIsVisibleToEmployee());

        EvaluationFeedback existing = mock(EvaluationFeedback.class);
        when(feedbackRepository.findByEvaluationIdAndEvaluationItemIdAndFeedbackType(
                EVALUATION_ID, ITEM_ID, FeedbackType.ITEM)).thenReturn(Optional.of(existing));
        service.saveDraft(EVALUATION_ID, request(null, "changed"));
        verify(existing).setFeedbackContent("changed");
        verify(existing, never()).setIsVisibleToEmployee(any());
        service.saveDraft(EVALUATION_ID, request(null, ""));
        verify(feedbackRepository).delete(existing);
    }

    @ParameterizedTest
    @ValueSource(ints = {1000, 1001})
    void itemFeedbackLengthIsValidated(int length) {
        Runnable action = () -> service.saveDraft(EVALUATION_ID, request(null, "a".repeat(length)));
        if (length == 1000) {
            action.run();
            verify(feedbackRepository).save(any());
        } else {
            assertError(EvaluationErrorCode.EVALUATION_FEEDBACK_INVALID, action);
        }
    }

    @Test
    void overallFeedbackIsInsertedInvisibleUpdatedAndDeleted() {
        service.saveDraft(EVALUATION_ID, overall("new"));
        ArgumentCaptor<EvaluationFeedback> captor = ArgumentCaptor.forClass(EvaluationFeedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertFalse(captor.getValue().getIsVisibleToEmployee());

        EvaluationFeedback existing = mock(EvaluationFeedback.class);
        givenOverall(existing);
        service.saveDraft(EVALUATION_ID, overall("changed"));
        verify(existing).setFeedbackContent("changed");
        verify(existing, never()).setIsVisibleToEmployee(any());
        service.saveDraft(EVALUATION_ID, overall(null));
        verify(feedbackRepository).delete(existing);
    }

    @ParameterizedTest
    @ValueSource(ints = {2000, 2001})
    void overallFeedbackLengthIsValidated(int length) {
        Runnable action = () -> service.saveDraft(EVALUATION_ID, overall("a".repeat(length)));
        if (length == 2000) {
            action.run();
            verify(feedbackRepository).save(any());
        } else {
            assertError(EvaluationErrorCode.EVALUATION_FEEDBACK_INVALID, action);
        }
    }

    @Test
    void multipleOverallFeedbacksAreConflict() {
        givenOverall(mock(EvaluationFeedback.class), mock(EvaluationFeedback.class));
        assertError(EvaluationErrorCode.EVALUATION_FEEDBACK_CONFLICT,
                () -> service.saveDraft(EVALUATION_ID, request(null, null)));
    }

    @Test
    void draftAndTotalScoreRemainUnchanged() {
        service.saveDraft(EVALUATION_ID, request(new BigDecimal("3.5"), null));
        verify(evaluation, never()).setEvaluationStatus(any());
        verify(evaluation, never()).setTotalScore(any());
    }

    @Test
    void saveDraftDefinesTransactionBoundary() throws NoSuchMethodException {
        Transactional annotation = ManagerEvaluationServiceImpl.class
                .getMethod("saveDraft", Long.class, ManagerEvaluationDraftRequest.class)
                .getAnnotation(Transactional.class);
        assertNotNull(annotation);
    }

    private void givenDefaultState() {
        lenient().when(evaluationRepository.findById(EVALUATION_ID))
                .thenReturn(Optional.of(evaluation));
        lenient().when(evaluation.getEvaluationId()).thenReturn(EVALUATION_ID);
        lenient().when(evaluation.getEvaluationCycleId()).thenReturn(CYCLE_ID);
        lenient().when(evaluation.getEvaluationTemplateId()).thenReturn(TEMPLATE_ID);
        lenient().when(evaluation.getEvaluationType()).thenReturn(EvaluationType.MANAGER);
        lenient().when(evaluation.getEvaluationStatus()).thenReturn(EvaluationStatus.DRAFT);
        lenient().when(evaluation.getTargetEmployeeId()).thenReturn(TARGET_ID);
        lenient().when(evaluation.getEvaluatorEmployeeId()).thenReturn(MANAGER_ID);
        givenUser(Set.of(RoleType.MANAGER));
        lenient().when(organizationQueryService.findDirectManagerEmployeeId(TARGET_ID))
                .thenReturn(MANAGER_ID);
        lenient().when(organizationQueryService.findEmployeeSummaries(anyCollection()))
                .thenReturn(List.of(summary()));
        lenient().when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
        lenient().when(cycle.getStartDate()).thenReturn(TODAY.minusDays(1));
        lenient().when(cycle.getEndDate()).thenReturn(TODAY.plusDays(1));
        lenient().when(item.getEvaluationItemId()).thenReturn(ITEM_ID);
        lenient().when(item.getItemOrder()).thenReturn(1);
        lenient().when(item.getItemName()).thenReturn("Item");
        lenient().when(item.getWeight()).thenReturn(new BigDecimal("100.00"));
        lenient().when(item.getIsRequired()).thenReturn(true);
        lenient().when(item.getMinimumScore()).thenReturn(1);
        lenient().when(item.getMaximumScore()).thenReturn(5);
        lenient().when(itemRepository.findByEvaluationTemplateIdOrderByItemOrderAsc(TEMPLATE_ID))
                .thenReturn(List.of(item));
        lenient().when(scoreRepository.findByEvaluationId(EVALUATION_ID)).thenReturn(List.of());
        lenient().when(feedbackRepository.findByEvaluationIdAndFeedbackType(
                EVALUATION_ID, FeedbackType.ITEM)).thenReturn(List.of());
        lenient().when(feedbackRepository.findByEvaluationIdAndFeedbackType(
                EVALUATION_ID, FeedbackType.OVERALL)).thenReturn(List.of());
    }

    @SuppressWarnings("unchecked")
    private Collection<Long> anyCollection() {
        return any(Collection.class);
    }

    private void givenUser(Set<RoleType> roles) {
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUserContext(
                10L, MANAGER_ID, roles, 20L, 1, null));
    }

    private EmployeeSummary summary() {
        return new EmployeeSummary(TARGET_ID, "Target", 10L, "Department", 20L, "Grade");
    }

    private EvaluationItem item(Long id, int order) {
        EvaluationItem result = mock(EvaluationItem.class);
        when(result.getEvaluationItemId()).thenReturn(id);
        when(result.getItemOrder()).thenReturn(order);
        return result;
    }

    private EvaluationFeedback feedback(Long itemId, String content) {
        EvaluationFeedback result = mock(EvaluationFeedback.class);
        lenient().when(result.getEvaluationItemId()).thenReturn(itemId);
        when(result.getFeedbackContent()).thenReturn(content);
        return result;
    }

    private ManagerEvaluationDraftRequest request(BigDecimal score, String itemFeedback) {
        return new ManagerEvaluationDraftRequest(List.of(
                new ManagerEvaluationItemDraftRequest(ITEM_ID, score, itemFeedback)), null);
    }

    private ManagerEvaluationDraftRequest overall(String content) {
        return new ManagerEvaluationDraftRequest(List.of(), content);
    }

    private void givenOverall(EvaluationFeedback... rows) {
        when(feedbackRepository.findByEvaluationIdAndFeedbackType(
                EVALUATION_ID, FeedbackType.OVERALL)).thenReturn(List.of(rows));
    }

    private void assertOwnerError(Runnable action) {
        assertError(EvaluationErrorCode.EVALUATION_NOT_OWNER, action);
    }

    private void assertRelationError(Runnable action) {
        assertError(EvaluationErrorCode.EVALUATION_MANAGER_RELATION_INVALID, action);
    }

    private void assertNotWritable(Runnable action) {
        assertError(EvaluationErrorCode.EVALUATION_NOT_WRITABLE, action);
    }

    private void assertError(EvaluationErrorCode expected, Runnable action) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertSame(expected, exception.getErrorCode());
    }
}
