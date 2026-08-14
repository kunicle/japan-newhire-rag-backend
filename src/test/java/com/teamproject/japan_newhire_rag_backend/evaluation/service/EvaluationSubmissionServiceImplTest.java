package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@ExtendWith(MockitoExtension.class)
class EvaluationSubmissionServiceImplTest {

    private static final Long EVALUATION_ID = 1L;
    private static final Long CYCLE_ID = 2L;
    private static final Long TEMPLATE_ID = 3L;
    private static final Long EMPLOYEE_ID = 4L;
    private static final Long TARGET_ID = 5L;
    private static final Long ITEM_ID = 6L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 12);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-12T03:04:05Z"), ZoneOffset.UTC);

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
    @Mock private EvaluationScore score;

    private EvaluationSubmissionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EvaluationSubmissionServiceImpl(
                evaluationRepository, cycleRepository, itemRepository, scoreRepository,
                feedbackRepository, organizationQueryService, currentUserProvider, CLOCK);
        givenSelfEvaluation();
    }

    @Test
    void selfOwnerSubmitsOpenDraftWithRequiredScore() {
        service.submitSelf(EVALUATION_ID);

        verify(evaluation).setEvaluationStatus(EvaluationStatus.SUBMITTED);
        verify(evaluation).setSubmittedAt(LocalDateTime.of(2026, 8, 12, 3, 4, 5));
    }

    @ParameterizedTest
    @EnumSource(value = EvaluationStatus.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
    void selfCannotSubmitNonDraftStatus(EvaluationStatus status) {
        when(evaluation.getEvaluationStatus()).thenReturn(status);
        assertError(EvaluationErrorCode.EVALUATION_NOT_WRITABLE,
                () -> service.submitSelf(EVALUATION_ID));
        verifyNoTransition();
    }

    @Test
    void selfCannotSubmitPlannedCycle() {
        when(cycle.getStartDate()).thenReturn(TODAY.plusDays(1));
        assertNotWritableSelf();
    }

    @Test
    void selfCannotSubmitClosedCycle() {
        when(cycle.getEndDate()).thenReturn(TODAY.minusDays(1));
        assertNotWritableSelf();
    }

    @Test
    void anotherEmployeeCannotSubmitSelf() {
        givenUser(99L, Set.of());
        assertOwner(() -> service.submitSelf(EVALUATION_ID));
    }

    @Test
    void managerEvaluationCannotUseSelfSubmission() {
        when(evaluation.getEvaluationType()).thenReturn(EvaluationType.MANAGER);
        assertOwner(() -> service.submitSelf(EVALUATION_ID));
    }

    @Test
    void malformedSelfAssignmentCannotBeSubmitted() {
        when(evaluation.getEvaluatorEmployeeId()).thenReturn(99L);
        assertOwner(() -> service.submitSelf(EVALUATION_ID));
    }

    @Test
    void missingEvaluationIsReported() {
        when(evaluationRepository.findById(99L)).thenReturn(Optional.empty());
        assertError(EvaluationErrorCode.EVALUATION_NOT_FOUND, () -> service.submitSelf(99L));
    }

    @Test
    void missingRequiredScoreIsRejected() {
        when(scoreRepository.findByEvaluationId(EVALUATION_ID)).thenReturn(List.of());
        assertError(EvaluationErrorCode.EVALUATION_SCORE_INVALID,
                () -> service.submitSelf(EVALUATION_ID));
        verifyNoTransition();
    }

    @Test
    void optionalScoreMayBeMissing() {
        when(item.getIsRequired()).thenReturn(false);
        when(scoreRepository.findByEvaluationId(EVALUATION_ID)).thenReturn(List.of());
        service.submitSelf(EVALUATION_ID);
        verify(evaluation).setEvaluationStatus(EvaluationStatus.SUBMITTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.9", "5.1", "3.55"})
    void persistedInvalidScoreIsRejected(String value) {
        when(score.getScore()).thenReturn(new BigDecimal(value));
        assertError(EvaluationErrorCode.EVALUATION_SCORE_INVALID,
                () -> service.submitSelf(EVALUATION_ID));
    }

    @Test
    void scoreFromAnotherTemplateIsRejected() {
        when(score.getEvaluationItemId()).thenReturn(99L);
        assertError(EvaluationErrorCode.EVALUATION_ITEM_MISMATCH,
                () -> service.submitSelf(EVALUATION_ID));
    }

    @Test
    void selfDoesNotRequireOverallFeedback() {
        service.submitSelf(EVALUATION_ID);
        verify(feedbackRepository, never()).findByEvaluationIdAndFeedbackType(
                EVALUATION_ID, FeedbackType.OVERALL);
    }

    @Test
    void managerSubmitsWithRoleOwnershipRelationAndOverallFeedback() {
        givenManagerEvaluation("overall");
        service.submitManager(EVALUATION_ID);
        verify(evaluation).setEvaluationStatus(EvaluationStatus.SUBMITTED);
        verify(organizationQueryService).findDirectManagerEmployeeId(TARGET_ID);
    }

    @Test
    void managerRoleIsRequired() {
        givenManagerEvaluation("overall");
        givenUser(EMPLOYEE_ID, Set.of(RoleType.HR_MANAGER));
        assertOwner(() -> service.submitManager(EVALUATION_ID));
    }

    @Test
    void assignedManagerOwnershipIsRequired() {
        givenManagerEvaluation("overall");
        when(evaluation.getEvaluatorEmployeeId()).thenReturn(99L);
        assertOwner(() -> service.submitManager(EVALUATION_ID));
    }

    @Test
    void selfEvaluationCannotUseManagerSubmission() {
        assertOwner(() -> service.submitManager(EVALUATION_ID));
    }

    @Test
    void currentDirectManagerMustMatch() {
        givenManagerEvaluation("overall");
        when(organizationQueryService.findDirectManagerEmployeeId(TARGET_ID)).thenReturn(99L);
        assertError(EvaluationErrorCode.EVALUATION_MANAGER_RELATION_INVALID,
                () -> service.submitManager(EVALUATION_ID));
    }

    @Test
    void duplicateDirectRelationIsConvertedToBusinessException() {
        givenManagerEvaluation("overall");
        when(organizationQueryService.findDirectManagerEmployeeId(TARGET_ID))
                .thenThrow(new IllegalStateException("duplicate"));
        assertError(EvaluationErrorCode.EVALUATION_MANAGER_RELATION_INVALID,
                () -> service.submitManager(EVALUATION_ID));
    }

    @Test
    void managerOverallFeedbackIsRequired() {
        givenManagerEvaluation(null);
        assertError(EvaluationErrorCode.EVALUATION_FEEDBACK_INVALID,
                () -> service.submitManager(EVALUATION_ID));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  \t  "})
    void managerOverallFeedbackCannotBeBlank(String content) {
        givenManagerEvaluation(content);
        assertError(EvaluationErrorCode.EVALUATION_FEEDBACK_INVALID,
                () -> service.submitManager(EVALUATION_ID));
    }

    @Test
    void managerOverallFeedbackAllowsTwoThousandCharacters() {
        givenManagerEvaluation("a".repeat(2000));
        service.submitManager(EVALUATION_ID);
        verify(evaluation).setEvaluationStatus(EvaluationStatus.SUBMITTED);
    }

    @Test
    void managerOverallFeedbackRejectsTwoThousandOneCharacters() {
        givenManagerEvaluation("a".repeat(2001));
        assertError(EvaluationErrorCode.EVALUATION_FEEDBACK_INVALID,
                () -> service.submitManager(EVALUATION_ID));
    }

    @Test
    void duplicateManagerOverallFeedbackIsConflict() {
        givenManagerEvaluation("overall");
        EvaluationFeedback first = feedback("one");
        EvaluationFeedback another = feedback("another");
        when(feedbackRepository.findByEvaluationIdAndFeedbackType(
                EVALUATION_ID, FeedbackType.OVERALL))
                .thenReturn(List.of(first, another));
        assertError(EvaluationErrorCode.EVALUATION_FEEDBACK_CONFLICT,
                () -> service.submitManager(EVALUATION_ID));
    }

    @Test
    void submissionDoesNotChangePublishedAtTotalScoreOrFeedbackVisibility() {
        EvaluationFeedback feedback = givenManagerEvaluation("overall");
        service.submitManager(EVALUATION_ID);
        verify(evaluation, never()).setPublishedAt(org.mockito.ArgumentMatchers.any());
        verify(evaluation, never()).setTotalScore(org.mockito.ArgumentMatchers.any());
        verify(feedback, never()).setIsVisibleToEmployee(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validationFailureDoesNotStartStateTransition() {
        givenManagerEvaluation(" ");
        assertThrows(BusinessException.class, () -> service.submitManager(EVALUATION_ID));
        verifyNoTransition();
    }

    @Test
    void submissionMethodsAreTransactional() throws Exception {
        assertSame(Transactional.class, EvaluationSubmissionServiceImpl.class
                .getMethod("submitSelf", Long.class).getAnnotation(Transactional.class)
                .annotationType());
        assertSame(Transactional.class, EvaluationSubmissionServiceImpl.class
                .getMethod("submitManager", Long.class).getAnnotation(Transactional.class)
                .annotationType());
    }

    private void givenSelfEvaluation() {
        lenient().when(evaluationRepository.findById(EVALUATION_ID))
                .thenReturn(Optional.of(evaluation));
        lenient().when(evaluation.getEvaluationId()).thenReturn(EVALUATION_ID);
        lenient().when(evaluation.getEvaluationCycleId()).thenReturn(CYCLE_ID);
        lenient().when(evaluation.getEvaluationTemplateId()).thenReturn(TEMPLATE_ID);
        lenient().when(evaluation.getTargetEmployeeId()).thenReturn(EMPLOYEE_ID);
        lenient().when(evaluation.getEvaluatorEmployeeId()).thenReturn(EMPLOYEE_ID);
        lenient().when(evaluation.getEvaluationType()).thenReturn(EvaluationType.SELF);
        lenient().when(evaluation.getEvaluationStatus()).thenReturn(EvaluationStatus.DRAFT);
        givenUser(EMPLOYEE_ID, Set.of());
        lenient().when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
        lenient().when(cycle.getStartDate()).thenReturn(TODAY.minusDays(1));
        lenient().when(cycle.getEndDate()).thenReturn(TODAY.plusDays(1));
        lenient().when(item.getEvaluationItemId()).thenReturn(ITEM_ID);
        lenient().when(item.getIsRequired()).thenReturn(true);
        lenient().when(itemRepository.findByEvaluationTemplateIdOrderByItemOrderAsc(TEMPLATE_ID))
                .thenReturn(List.of(item));
        lenient().when(score.getEvaluationItemId()).thenReturn(ITEM_ID);
        lenient().when(score.getScore()).thenReturn(new BigDecimal("3.5"));
        lenient().when(scoreRepository.findByEvaluationId(EVALUATION_ID)).thenReturn(List.of(score));
    }

    private EvaluationFeedback givenManagerEvaluation(String content) {
        lenient().when(evaluation.getTargetEmployeeId()).thenReturn(TARGET_ID);
        lenient().when(evaluation.getEvaluatorEmployeeId()).thenReturn(EMPLOYEE_ID);
        lenient().when(evaluation.getEvaluationType()).thenReturn(EvaluationType.MANAGER);
        givenUser(EMPLOYEE_ID, Set.of(RoleType.MANAGER));
        lenient().when(organizationQueryService.findDirectManagerEmployeeId(TARGET_ID))
                .thenReturn(EMPLOYEE_ID);
        if (content == null) {
            lenient().when(feedbackRepository.findByEvaluationIdAndFeedbackType(
                    EVALUATION_ID, FeedbackType.OVERALL)).thenReturn(List.of());
            return null;
        }
        EvaluationFeedback feedback = feedback(content);
        lenient().when(feedbackRepository.findByEvaluationIdAndFeedbackType(
                EVALUATION_ID, FeedbackType.OVERALL)).thenReturn(List.of(feedback));
        return feedback;
    }

    private EvaluationFeedback feedback(String content) {
        EvaluationFeedback feedback = mock(EvaluationFeedback.class);
        lenient().when(feedback.getFeedbackContent()).thenReturn(content);
        return feedback;
    }

    private void givenUser(Long employeeId, Set<RoleType> roles) {
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(
                new CurrentUserContext(10L, employeeId, roles, 20L, 1, null));
    }

    private void assertNotWritableSelf() {
        assertError(EvaluationErrorCode.EVALUATION_NOT_WRITABLE,
                () -> service.submitSelf(EVALUATION_ID));
        verifyNoTransition();
    }

    private void assertOwner(Runnable action) {
        assertError(EvaluationErrorCode.EVALUATION_NOT_OWNER, action);
        verifyNoTransition();
    }

    private void assertError(EvaluationErrorCode expected, Runnable action) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertSame(expected, exception.getErrorCode());
    }

    private void verifyNoTransition() {
        verify(evaluation, never()).setEvaluationStatus(EvaluationStatus.SUBMITTED);
        verify(evaluation, never()).setSubmittedAt(org.mockito.ArgumentMatchers.any());
    }
}
