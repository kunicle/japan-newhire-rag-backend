package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
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
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationResultResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@ExtendWith(MockitoExtension.class)
class EvaluationResultServiceImplTest {

    private static final Long CYCLE_ID = 1L;
    private static final Long EMPLOYEE_ID = 2L;
    private static final Long SELF_ID = 3L;
    private static final Long MANAGER_ID = 4L;
    private static final Long SELF_TEMPLATE_ID = 5L;
    private static final Long MANAGER_TEMPLATE_ID = 6L;

    @Mock private EvaluationRepository evaluationRepository;
    @Mock private EvaluationCycleRepository cycleRepository;
    @Mock private EvaluationItemRepository itemRepository;
    @Mock private EvaluationScoreRepository scoreRepository;
    @Mock private EvaluationFeedbackRepository feedbackRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private EvaluationCycle cycle;
    @Mock private Evaluation self;
    @Mock private Evaluation manager;

    private EvaluationResultServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EvaluationResultServiceImpl(
                evaluationRepository, cycleRepository, itemRepository,
                scoreRepository, feedbackRepository, currentUserProvider);
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(EMPLOYEE_ID));
        lenient().when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
        lenient().when(cycle.getEvaluationCycleId()).thenReturn(CYCLE_ID);
        lenient().when(cycle.getCycleName()).thenReturn("2026 Review");
        lenient().when(cycle.getStartDate()).thenReturn(LocalDate.of(2026, 1, 1));
        lenient().when(cycle.getEndDate()).thenReturn(LocalDate.of(2026, 6, 30));
        lenient().when(cycle.getPlannedPublishDate()).thenReturn(LocalDate.of(2026, 7, 10));
        givenEvaluation(self, SELF_ID, SELF_TEMPLATE_ID, EMPLOYEE_ID,
                EvaluationType.SELF, EvaluationStatus.PUBLISHED);
        givenEvaluation(manager, MANAGER_ID, MANAGER_TEMPLATE_ID, EMPLOYEE_ID,
                EvaluationType.MANAGER, EvaluationStatus.PUBLISHED);
        lenient().when(evaluationRepository
                .findByEvaluationCycleIdAndTargetEmployeeIdAndEvaluationTypeIn(
                        CYCLE_ID, EMPLOYEE_ID,
                        List.of(EvaluationType.SELF, EvaluationType.MANAGER)))
                .thenReturn(List.of(self, manager));
        lenient().when(itemRepository.findByEvaluationTemplateIdOrderByItemOrderAsc(
                SELF_TEMPLATE_ID)).thenReturn(List.of());
        lenient().when(itemRepository.findByEvaluationTemplateIdOrderByItemOrderAsc(
                MANAGER_TEMPLATE_ID)).thenReturn(List.of());
        lenient().when(scoreRepository.findByEvaluationId(SELF_ID)).thenReturn(List.of());
        lenient().when(scoreRepository.findByEvaluationId(MANAGER_ID)).thenReturn(List.of());
        lenient().when(feedbackRepository.findByEvaluationIdAndIsVisibleToEmployeeTrue(
                SELF_ID)).thenReturn(List.of());
        lenient().when(feedbackRepository.findByEvaluationIdAndIsVisibleToEmployeeTrue(
                MANAGER_ID)).thenReturn(List.of());
    }

    @Test
    void returnsPublishedSelfAndManagerResultForCurrentEmployee() {
        EvaluationItem selfItem = item(11L, 1, "Self item");
        EvaluationItem optionalManagerItem = item(22L, 2, "Manager item");
        EvaluationScore selfScore = score(SELF_ID, 11L, "4.5");
        EvaluationFeedback selfItemFeedback = feedback(
                SELF_ID, 11L, FeedbackType.ITEM, "self visible");
        EvaluationFeedback selfOverall = feedback(
                SELF_ID, null, FeedbackType.OVERALL, "self overall");
        EvaluationFeedback managerItemFeedback = feedback(
                MANAGER_ID, 22L, FeedbackType.ITEM, "manager visible");
        EvaluationFeedback managerOverall = feedback(
                MANAGER_ID, null, FeedbackType.OVERALL, "manager overall");
        when(self.getTotalScore()).thenReturn(new BigDecimal("4.25"));
        when(manager.getTotalScore()).thenReturn(null);
        when(itemRepository.findByEvaluationTemplateIdOrderByItemOrderAsc(SELF_TEMPLATE_ID))
                .thenReturn(List.of(selfItem));
        when(itemRepository.findByEvaluationTemplateIdOrderByItemOrderAsc(MANAGER_TEMPLATE_ID))
                .thenReturn(List.of(optionalManagerItem));
        when(scoreRepository.findByEvaluationId(SELF_ID)).thenReturn(List.of(selfScore));
        when(feedbackRepository.findByEvaluationIdAndIsVisibleToEmployeeTrue(SELF_ID))
                .thenReturn(List.of(selfItemFeedback, selfOverall));
        when(feedbackRepository.findByEvaluationIdAndIsVisibleToEmployeeTrue(MANAGER_ID))
                .thenReturn(List.of(managerItemFeedback, managerOverall));

        EvaluationResultResponse result = service.getMyResult(CYCLE_ID);

        assertEquals(CYCLE_ID, result.cycle().cycleId());
        assertEquals("2026 Review", result.cycle().cycleName());
        assertEquals(new BigDecimal("4.25"), result.self().totalScore());
        assertEquals(new BigDecimal("4.5"), result.self().items().get(0).score());
        assertEquals("self visible", result.self().items().get(0).itemFeedback());
        assertEquals("self overall", result.self().overallFeedback());
        assertNull(result.manager().totalScore());
        assertNull(result.manager().items().get(0).score());
        assertEquals("manager visible", result.manager().items().get(0).itemFeedback());
        assertEquals("manager overall", result.manager().overallFeedback());
        verify(feedbackRepository, never()).findByEvaluationId(SELF_ID);
        verify(feedbackRepository, never()).findByEvaluationId(MANAGER_ID);
    }

    @Test
    void rejectsMissingCurrentEmployeeId() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(null));

        assertError(EvaluationErrorCode.EVALUATION_NOT_OWNER,
                () -> service.getMyResult(CYCLE_ID));
        verify(cycleRepository, never()).findById(CYCLE_ID);
    }

    @Test
    void rejectsResultOwnedByAnotherEmployeeEvenForReturnedRows() {
        when(self.getTargetEmployeeId()).thenReturn(999L);

        assertError(EvaluationErrorCode.EVALUATION_NOT_OWNER,
                () -> service.getMyResult(CYCLE_ID));
        verify(scoreRepository, never()).findByEvaluationId(SELF_ID);
    }

    @Test
    void rejectsMissingCycle() {
        when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.empty());

        assertError(EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND,
                () -> service.getMyResult(CYCLE_ID));
    }

    @Test
    void rejectsMissingEvaluationSet() {
        when(evaluationRepository
                .findByEvaluationCycleIdAndTargetEmployeeIdAndEvaluationTypeIn(
                        CYCLE_ID, EMPLOYEE_ID,
                        List.of(EvaluationType.SELF, EvaluationType.MANAGER)))
                .thenReturn(List.of(self));

        assertError(EvaluationErrorCode.EVALUATION_NOT_FOUND,
                () -> service.getMyResult(CYCLE_ID));
    }

    @ParameterizedTest
    @EnumSource(value = EvaluationStatus.class, names = {"DRAFT", "SUBMITTED", "RETURNED"})
    void rejectsUnpublishedResult(EvaluationStatus status) {
        when(self.getEvaluationStatus()).thenReturn(status);
        when(manager.getEvaluationStatus()).thenReturn(status);

        assertError(EvaluationErrorCode.EVALUATION_RESULT_NOT_AVAILABLE,
                () -> service.getMyResult(CYCLE_ID));
    }

    @Test
    void rejectsMixedPublishedAndSubmittedState() {
        when(manager.getEvaluationStatus()).thenReturn(EvaluationStatus.SUBMITTED);

        assertError(EvaluationErrorCode.EVALUATION_PUBLISH_CONFLICT,
                () -> service.getMyResult(CYCLE_ID));
    }

    @Test
    void rejectsDuplicateSelfEvaluation() {
        Evaluation duplicate = mock(Evaluation.class);
        when(duplicate.getEvaluationType()).thenReturn(EvaluationType.SELF);
        when(evaluationRepository
                .findByEvaluationCycleIdAndTargetEmployeeIdAndEvaluationTypeIn(
                        CYCLE_ID, EMPLOYEE_ID,
                        List.of(EvaluationType.SELF, EvaluationType.MANAGER)))
                .thenReturn(List.of(self, duplicate, manager));

        assertError(EvaluationErrorCode.EVALUATION_PUBLISH_CONFLICT,
                () -> service.getMyResult(CYCLE_ID));
    }

    @Test
    void rejectsMultipleVisibleOverallFeedback() {
        EvaluationFeedback first = feedback(
                SELF_ID, null, FeedbackType.OVERALL, "one");
        EvaluationFeedback second = feedback(
                SELF_ID, null, FeedbackType.OVERALL, "two");
        when(feedbackRepository.findByEvaluationIdAndIsVisibleToEmployeeTrue(SELF_ID))
                .thenReturn(List.of(first, second));

        assertError(EvaluationErrorCode.EVALUATION_FEEDBACK_CONFLICT,
                () -> service.getMyResult(CYCLE_ID));
    }

    @Test
    void rejectsMultipleVisibleItemFeedbackForSameItem() {
        EvaluationFeedback first = feedback(
                SELF_ID, 11L, FeedbackType.ITEM, "one");
        EvaluationFeedback second = feedback(
                SELF_ID, 11L, FeedbackType.ITEM, "two");
        when(feedbackRepository.findByEvaluationIdAndIsVisibleToEmployeeTrue(SELF_ID))
                .thenReturn(List.of(first, second));

        assertError(EvaluationErrorCode.EVALUATION_FEEDBACK_CONFLICT,
                () -> service.getMyResult(CYCLE_ID));
    }

    @Test
    void keepsRepositoryItemOrderAndDoesNotMutateEntities() {
        EvaluationItem second = item(12L, 2, "second");
        EvaluationItem first = item(11L, 1, "first");
        when(itemRepository.findByEvaluationTemplateIdOrderByItemOrderAsc(SELF_TEMPLATE_ID))
                .thenReturn(List.of(first, second));

        EvaluationResultResponse result = service.getMyResult(CYCLE_ID);

        assertEquals(List.of(1, 2), result.self().items().stream()
                .map(item -> item.itemOrder()).toList());
        verify(self, never()).setEvaluationStatus(org.mockito.ArgumentMatchers.any());
        verify(self, never()).setTotalScore(org.mockito.ArgumentMatchers.any());
        verify(manager, never()).setEvaluationStatus(org.mockito.ArgumentMatchers.any());
        verify(manager, never()).setTotalScore(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void serviceUsesReadOnlyTransaction() {
        Transactional transactional = EvaluationResultServiceImpl.class
                .getAnnotation(Transactional.class);

        assertFalse(transactional == null);
        assertEquals(true, transactional.readOnly());
    }

    private void givenEvaluation(
            Evaluation evaluation,
            Long evaluationId,
            Long templateId,
            Long targetEmployeeId,
            EvaluationType type,
            EvaluationStatus status
    ) {
        lenient().when(evaluation.getEvaluationId()).thenReturn(evaluationId);
        lenient().when(evaluation.getEvaluationTemplateId()).thenReturn(templateId);
        lenient().when(evaluation.getTargetEmployeeId()).thenReturn(targetEmployeeId);
        lenient().when(evaluation.getEvaluationType()).thenReturn(type);
        lenient().when(evaluation.getEvaluationStatus()).thenReturn(status);
    }

    private CurrentUserContext currentUser(Long employeeId) {
        return new CurrentUserContext(10L, employeeId, Set.of(), null, null, null);
    }

    private EvaluationItem item(Long itemId, Integer order, String name) {
        EvaluationItem item = mock(EvaluationItem.class);
        when(item.getEvaluationItemId()).thenReturn(itemId);
        when(item.getItemOrder()).thenReturn(order);
        when(item.getItemName()).thenReturn(name);
        when(item.getItemDescription()).thenReturn(name + " description");
        when(item.getWeight()).thenReturn(new BigDecimal("50.00"));
        when(item.getIsRequired()).thenReturn(true);
        return item;
    }

    private EvaluationScore score(Long evaluationId, Long itemId, String value) {
        EvaluationScore score = mock(EvaluationScore.class);
        lenient().when(score.getEvaluationId()).thenReturn(evaluationId);
        when(score.getEvaluationItemId()).thenReturn(itemId);
        when(score.getScore()).thenReturn(new BigDecimal(value));
        return score;
    }

    private EvaluationFeedback feedback(
            Long evaluationId,
            Long itemId,
            FeedbackType type,
            String content
    ) {
        EvaluationFeedback feedback = mock(EvaluationFeedback.class);
        lenient().when(feedback.getEvaluationId()).thenReturn(evaluationId);
        lenient().when(feedback.getEvaluationItemId()).thenReturn(itemId);
        when(feedback.getFeedbackType()).thenReturn(type);
        lenient().when(feedback.getFeedbackContent()).thenReturn(content);
        lenient().when(feedback.getIsVisibleToEmployee()).thenReturn(true);
        return feedback;
    }

    private void assertError(EvaluationErrorCode expected, Runnable action) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertEquals(expected, exception.getErrorCode());
    }
}
