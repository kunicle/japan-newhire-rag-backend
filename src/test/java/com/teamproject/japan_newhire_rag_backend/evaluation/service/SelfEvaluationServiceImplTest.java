package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.teamproject.japan_newhire_rag_backend.evaluation.Evaluation;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleRepository;
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
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.SelfEvaluationDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.SelfEvaluationItemDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.SelfEvaluationResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@ExtendWith(MockitoExtension.class)
class SelfEvaluationServiceImplTest {

    private static final Long EVALUATION_ID = 1L;
    private static final Long CYCLE_ID = 2L;
    private static final Long TEMPLATE_ID = 3L;
    private static final Long EMPLOYEE_ID = 4L;
    private static final Long ITEM_ID = 5L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 12);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC);

    @Mock private EvaluationRepository evaluationRepository;
    @Mock private EvaluationCycleRepository cycleRepository;
    @Mock private EvaluationItemRepository itemRepository;
    @Mock private EvaluationScoreRepository scoreRepository;
    @Mock private EvaluationFeedbackRepository feedbackRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private Evaluation evaluation;
    @Mock private EvaluationCycle cycle;
    @Mock private EvaluationItem item;

    private SelfEvaluationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SelfEvaluationServiceImpl(
                evaluationRepository, cycleRepository, itemRepository, scoreRepository,
                feedbackRepository, currentUserProvider, CLOCK);
        givenDefaultState();
    }

    @Test
    void ownerReadsSelfEvaluation() {
        SelfEvaluationResponse response = service.getMySelfEvaluation(EVALUATION_ID);

        assertEquals(EVALUATION_ID, response.evaluationId());
        assertEquals(CYCLE_ID, response.evaluationCycleId());
        assertEquals(TEMPLATE_ID, response.evaluationTemplateId());
        assertEquals(EvaluationStatus.DRAFT, response.evaluationStatus());
    }

    @Test
    void anotherEmployeeCannotRead() {
        givenCurrentEmployee(99L);
        assertError(EvaluationErrorCode.EVALUATION_NOT_OWNER,
                () -> service.getMySelfEvaluation(EVALUATION_ID));
    }

    @Test
    void managerEvaluationCannotBeRead() {
        when(evaluation.getEvaluationType()).thenReturn(EvaluationType.MANAGER);
        assertError(EvaluationErrorCode.EVALUATION_NOT_OWNER,
                () -> service.getMySelfEvaluation(EVALUATION_ID));
    }

    @Test
    void missingEvaluationIsReported() {
        when(evaluationRepository.findById(99L)).thenReturn(Optional.empty());
        assertError(EvaluationErrorCode.EVALUATION_NOT_FOUND,
                () -> service.getMySelfEvaluation(99L));
    }

    @Test
    void responseKeepsRepositoryItemOrder() {
        EvaluationItem second = item(6L, 2, false);
        when(itemRepository.findByEvaluationTemplateIdOrderByItemOrderAsc(TEMPLATE_ID))
                .thenReturn(List.of(item, second));

        SelfEvaluationResponse response = service.getMySelfEvaluation(EVALUATION_ID);

        assertEquals(List.of(1, 2), response.items().stream().map(i -> i.itemOrder()).toList());
    }

    @Test
    void responseContainsExistingScore() {
        EvaluationScore score = mock(EvaluationScore.class);
        when(score.getEvaluationItemId()).thenReturn(ITEM_ID);
        when(score.getScore()).thenReturn(new BigDecimal("3.5"));
        when(scoreRepository.findByEvaluationId(EVALUATION_ID)).thenReturn(List.of(score));

        assertEquals(new BigDecimal("3.5"),
                service.getMySelfEvaluation(EVALUATION_ID).items().get(0).score());
    }

    @Test
    void responseContainsExistingItemFeedback() {
        EvaluationFeedback feedback = feedback(ITEM_ID, "item feedback");
        when(feedbackRepository.findByEvaluationIdAndFeedbackType(
                EVALUATION_ID, FeedbackType.ITEM)).thenReturn(List.of(feedback));

        assertEquals("item feedback",
                service.getMySelfEvaluation(EVALUATION_ID).items().get(0).itemFeedback());
    }

    @Test
    void responseContainsExistingOverallFeedback() {
        EvaluationFeedback overall = feedback(null, "overall");
        when(feedbackRepository.findByEvaluationIdAndFeedbackType(
                EVALUATION_ID, FeedbackType.OVERALL))
                .thenReturn(List.of(overall));

        assertEquals("overall", service.getMySelfEvaluation(EVALUATION_ID).overallFeedback());
    }

    @Test
    void openDraftCanBeSaved() {
        SelfEvaluationResponse response = service.saveDraft(EVALUATION_ID, request(null, null));
        assertEquals(EvaluationStatus.DRAFT, response.evaluationStatus());
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
    void plannedCycleRejectsSave() {
        when(cycle.getStartDate()).thenReturn(TODAY.plusDays(1));
        assertNotWritable();
    }

    @Test
    void closedCycleRejectsSave() {
        when(cycle.getStartDate()).thenReturn(TODAY.minusDays(10));
        when(cycle.getEndDate()).thenReturn(TODAY.minusDays(1));
        assertNotWritable();
    }

    @ParameterizedTest
    @EnumSource(value = EvaluationStatus.class, names = {"SUBMITTED", "RETURNED", "PUBLISHED"})
    void nonDraftStatusRejectsSave(EvaluationStatus status) {
        when(evaluation.getEvaluationStatus()).thenReturn(status);
        assertNotWritable();
    }

    @Test
    void anotherEmployeeCannotSave() {
        givenCurrentEmployee(99L);
        assertError(EvaluationErrorCode.EVALUATION_NOT_OWNER,
                () -> service.saveDraft(EVALUATION_ID, request(null, null)));
    }

    @Test
    void scoreIsInserted() {
        service.saveDraft(EVALUATION_ID, request(new BigDecimal("3.5"), null));
        verify(scoreRepository).save(any(EvaluationScore.class));
    }

    @Test
    void existingScoreIsUpdated() {
        EvaluationScore existing = mock(EvaluationScore.class);
        when(scoreRepository.findByEvaluationIdAndEvaluationItemId(EVALUATION_ID, ITEM_ID))
                .thenReturn(Optional.of(existing));

        service.saveDraft(EVALUATION_ID, request(new BigDecimal("4.5"), null));

        verify(existing).setScore(new BigDecimal("4.5"));
        verify(scoreRepository, never()).save(any());
    }

    @Test
    void nullDeletesExistingScore() {
        EvaluationScore existing = mock(EvaluationScore.class);
        when(scoreRepository.findByEvaluationIdAndEvaluationItemId(EVALUATION_ID, ITEM_ID))
                .thenReturn(Optional.of(existing));

        service.saveDraft(EVALUATION_ID, request(null, null));

        verify(scoreRepository).delete(existing);
    }

    @Test
    void nullWithoutExistingScoreDoesNothing() {
        service.saveDraft(EVALUATION_ID, request(null, null));
        verify(scoreRepository, never()).save(any());
        verify(scoreRepository, never()).delete(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "1.0", "3", "3.5", "4.5", "5.0"})
    void validScoresAreAccepted(String value) {
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
    void itemFromAnotherTemplateIsRejected() {
        SelfEvaluationDraftRequest request = new SelfEvaluationDraftRequest(
                List.of(new SelfEvaluationItemDraftRequest(99L, null, null)), null);
        assertError(EvaluationErrorCode.EVALUATION_ITEM_MISMATCH,
                () -> service.saveDraft(EVALUATION_ID, request));
    }

    @Test
    void newItemFeedbackIsInsertedAndInvisible() {
        service.saveDraft(EVALUATION_ID, request(null, "feedback"));

        ArgumentCaptor<EvaluationFeedback> captor = ArgumentCaptor.forClass(EvaluationFeedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertEquals(FeedbackType.ITEM, captor.getValue().getFeedbackType());
        assertFalse(captor.getValue().getIsVisibleToEmployee());
    }

    @Test
    void existingItemFeedbackIsUpdated() {
        EvaluationFeedback existing = mock(EvaluationFeedback.class);
        when(feedbackRepository.findByEvaluationIdAndEvaluationItemIdAndFeedbackType(
                EVALUATION_ID, ITEM_ID, FeedbackType.ITEM)).thenReturn(Optional.of(existing));

        service.saveDraft(EVALUATION_ID, request(null, "changed"));

        verify(existing).setFeedbackContent("changed");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "__NULL__"})
    void removedItemFeedbackIsDeleted(String contentToken) {
        EvaluationFeedback existing = mock(EvaluationFeedback.class);
        when(feedbackRepository.findByEvaluationIdAndEvaluationItemIdAndFeedbackType(
                EVALUATION_ID, ITEM_ID, FeedbackType.ITEM)).thenReturn(Optional.of(existing));
        String content = "__NULL__".equals(contentToken) ? null : contentToken;

        service.saveDraft(EVALUATION_ID, request(null, content));

        verify(feedbackRepository).delete(existing);
    }

    @Test
    void itemFeedbackOfOneThousandCharactersIsAccepted() {
        service.saveDraft(EVALUATION_ID, request(null, "a".repeat(1000)));
        verify(feedbackRepository).save(any(EvaluationFeedback.class));
    }

    @Test
    void itemFeedbackOfOneThousandOneCharactersIsRejected() {
        assertFeedbackInvalid(request(null, "a".repeat(1001)));
    }

    @Test
    void newOverallFeedbackIsInsertedWithNullItemAndInvisible() {
        service.saveDraft(EVALUATION_ID, requestWithOverall("overall"));

        ArgumentCaptor<EvaluationFeedback> captor = ArgumentCaptor.forClass(EvaluationFeedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertEquals(FeedbackType.OVERALL, captor.getValue().getFeedbackType());
        assertNull(captor.getValue().getEvaluationItemId());
        assertFalse(captor.getValue().getIsVisibleToEmployee());
    }

    @Test
    void existingOverallFeedbackIsUpdated() {
        EvaluationFeedback existing = mock(EvaluationFeedback.class);
        givenOverallFeedbacks(existing);

        service.saveDraft(EVALUATION_ID, requestWithOverall("changed"));

        verify(existing).setFeedbackContent("changed");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "__NULL__"})
    void removedOverallFeedbackIsDeleted(String contentToken) {
        EvaluationFeedback existing = mock(EvaluationFeedback.class);
        givenOverallFeedbacks(existing);
        String content = "__NULL__".equals(contentToken) ? null : contentToken;

        service.saveDraft(EVALUATION_ID, requestWithOverall(content));

        verify(feedbackRepository).delete(existing);
    }

    @Test
    void overallFeedbackOfTwoThousandCharactersIsAccepted() {
        service.saveDraft(EVALUATION_ID, requestWithOverall("a".repeat(2000)));
        verify(feedbackRepository).save(any(EvaluationFeedback.class));
    }

    @Test
    void overallFeedbackOfTwoThousandOneCharactersIsRejected() {
        assertFeedbackInvalid(requestWithOverall("a".repeat(2001)));
    }

    @Test
    void multipleOverallFeedbacksAreConflictOnRead() {
        givenOverallFeedbacks(mock(EvaluationFeedback.class), mock(EvaluationFeedback.class));
        assertError(EvaluationErrorCode.EVALUATION_FEEDBACK_CONFLICT,
                () -> service.getMySelfEvaluation(EVALUATION_ID));
    }

    @Test
    void multipleOverallFeedbacksAreConflictOnSave() {
        givenOverallFeedbacks(mock(EvaluationFeedback.class), mock(EvaluationFeedback.class));
        assertError(EvaluationErrorCode.EVALUATION_FEEDBACK_CONFLICT,
                () -> service.saveDraft(EVALUATION_ID, request(null, null)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void scoreMayBeNullRegardlessOfRequired(boolean required) {
        when(item.getIsRequired()).thenReturn(required);
        service.saveDraft(EVALUATION_ID, request(null, null));
        verify(scoreRepository, never()).save(any());
    }

    @Test
    void draftStatusAndTotalScoreAreNotChanged() {
        service.saveDraft(EVALUATION_ID, request(new BigDecimal("3.5"), null));
        verify(evaluation, never()).setEvaluationStatus(any());
        verify(evaluation, never()).setTotalScore(any());
    }

    @Test
    void validationCompletesBeforeAnyWrite() {
        SelfEvaluationDraftRequest request = new SelfEvaluationDraftRequest(List.of(
                new SelfEvaluationItemDraftRequest(ITEM_ID, new BigDecimal("3.5"), null),
                new SelfEvaluationItemDraftRequest(99L, new BigDecimal("3.5"), null)), null);

        assertError(EvaluationErrorCode.EVALUATION_ITEM_MISMATCH,
                () -> service.saveDraft(EVALUATION_ID, request));
        verify(scoreRepository, never()).save(any());
        verify(feedbackRepository, never()).save(any());
        verify(evaluation, never()).setLastDraftSavedAt(any());
    }

    @Test
    void saveDraftDefinesTransactionBoundary() throws NoSuchMethodException {
        Transactional annotation = SelfEvaluationServiceImpl.class
                .getMethod("saveDraft", Long.class, SelfEvaluationDraftRequest.class)
                .getAnnotation(Transactional.class);
        assertNotNull(annotation);
    }

    private void givenDefaultState() {
        lenient().when(evaluationRepository.findById(EVALUATION_ID))
                .thenReturn(Optional.of(evaluation));
        lenient().when(evaluation.getEvaluationId()).thenReturn(EVALUATION_ID);
        lenient().when(evaluation.getEvaluationCycleId()).thenReturn(CYCLE_ID);
        lenient().when(evaluation.getEvaluationTemplateId()).thenReturn(TEMPLATE_ID);
        lenient().when(evaluation.getEvaluationType()).thenReturn(EvaluationType.SELF);
        lenient().when(evaluation.getEvaluationStatus()).thenReturn(EvaluationStatus.DRAFT);
        lenient().when(evaluation.getTargetEmployeeId()).thenReturn(EMPLOYEE_ID);
        lenient().when(evaluation.getEvaluatorEmployeeId()).thenReturn(EMPLOYEE_ID);
        givenCurrentEmployee(EMPLOYEE_ID);
        lenient().when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
        lenient().when(cycle.getStartDate()).thenReturn(TODAY.minusDays(1));
        lenient().when(cycle.getEndDate()).thenReturn(TODAY.plusDays(1));
        lenient().when(item.getEvaluationItemId()).thenReturn(ITEM_ID);
        lenient().when(item.getEvaluationTemplateId()).thenReturn(TEMPLATE_ID);
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

    private void givenCurrentEmployee(Long employeeId) {
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUserContext(
                10L, employeeId, Set.of(RoleType.EMPLOYEE), 20L, 1, null));
    }

    private EvaluationItem item(Long id, int order, boolean required) {
        EvaluationItem result = mock(EvaluationItem.class);
        when(result.getEvaluationItemId()).thenReturn(id);
        when(result.getItemOrder()).thenReturn(order);
        when(result.getIsRequired()).thenReturn(required);
        return result;
    }

    private EvaluationFeedback feedback(Long itemId, String content) {
        EvaluationFeedback result = mock(EvaluationFeedback.class);
        lenient().when(result.getEvaluationItemId()).thenReturn(itemId);
        when(result.getFeedbackContent()).thenReturn(content);
        return result;
    }

    private SelfEvaluationDraftRequest request(BigDecimal score, String itemFeedback) {
        return new SelfEvaluationDraftRequest(List.of(
                new SelfEvaluationItemDraftRequest(ITEM_ID, score, itemFeedback)), null);
    }

    private SelfEvaluationDraftRequest requestWithOverall(String overallFeedback) {
        return new SelfEvaluationDraftRequest(List.of(), overallFeedback);
    }

    private void givenOverallFeedbacks(EvaluationFeedback... feedbacks) {
        when(feedbackRepository.findByEvaluationIdAndFeedbackType(
                EVALUATION_ID, FeedbackType.OVERALL)).thenReturn(List.of(feedbacks));
    }

    private void assertNotWritable() {
        assertError(EvaluationErrorCode.EVALUATION_NOT_WRITABLE,
                () -> service.saveDraft(EVALUATION_ID, request(null, null)));
    }

    private void assertFeedbackInvalid(SelfEvaluationDraftRequest request) {
        assertError(EvaluationErrorCode.EVALUATION_FEEDBACK_INVALID,
                () -> service.saveDraft(EVALUATION_ID, request));
        verify(feedbackRepository, never()).save(any());
    }

    private void assertError(EvaluationErrorCode expected, Runnable action) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertSame(expected, exception.getErrorCode());
    }
}
