package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.internal.AuditLogRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.internal.AuditLogRecordService;
import com.teamproject.japan_newhire_rag_backend.evaluation.Evaluation;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationFeedback;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationFeedbackRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationPublishHistory;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationPublishHistoryRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@ExtendWith(MockitoExtension.class)
class EvaluationPublishServiceImplTest {

    private static final Long CYCLE_ID = 1L;
    private static final Long TARGET_ID = 2L;
    private static final Long SELF_ID = 3L;
    private static final Long MANAGER_ID = 4L;
    private static final Long ACTOR_ID = 5L;
    private static final Long SELF_FEEDBACK_ID = 101L;
    private static final Long SELECTED_MANAGER_FEEDBACK_ID = 201L;
    private static final Long HIDDEN_MANAGER_FEEDBACK_ID = 202L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final LocalDateTime PUBLISHED_AT = LocalDateTime.of(2026, 8, 20, 3, 4, 5);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-20T03:04:05Z"), ZoneOffset.UTC);

    @Mock private EvaluationRepository evaluationRepository;
    @Mock private EvaluationCycleRepository cycleRepository;
    @Mock private EvaluationFeedbackRepository feedbackRepository;
    @Mock private EvaluationPublishHistoryRepository historyRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private AuditLogRecordService auditLogRecordService;
    @Mock private Evaluation self;
    @Mock private Evaluation manager;
    @Mock private EvaluationCycle cycle;
    @Mock private EvaluationFeedback selfFeedback;
    @Mock private EvaluationFeedback selectedManagerFeedback;
    @Mock private EvaluationFeedback hiddenManagerFeedback;

    private EvaluationPublishServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EvaluationPublishServiceImpl(
                evaluationRepository, cycleRepository, feedbackRepository, historyRepository,
                currentUserProvider, auditLogRecordService, CLOCK);
        givenRoles(RoleType.HR_MANAGER);
        givenEvaluation(self, SELF_ID, EvaluationType.SELF);
        givenEvaluation(manager, MANAGER_ID, EvaluationType.MANAGER);
        lenient().when(evaluationRepository.findById(SELF_ID)).thenReturn(Optional.of(self));
        lenient().when(evaluationRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        lenient().when(evaluationRepository.findByEvaluationCycleIdAndTargetEmployeeId(
                CYCLE_ID, TARGET_ID)).thenReturn(List.of(self, manager));
        lenient().when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
        lenient().when(cycle.getEndDate()).thenReturn(TODAY.minusDays(1));
        lenient().when(cycle.getPlannedPublishDate()).thenReturn(TODAY.plusDays(10));
        lenient().when(cycle.getCycleStatus()).thenReturn(EvaluationCycleStatus.OPEN);
        givenFeedback(selfFeedback, SELF_FEEDBACK_ID, SELF_ID, false, "self content");
        givenFeedback(selectedManagerFeedback, SELECTED_MANAGER_FEEDBACK_ID,
                MANAGER_ID, false, "selected content");
        givenFeedback(hiddenManagerFeedback, HIDDEN_MANAGER_FEEDBACK_ID,
                MANAGER_ID, true, "hidden content");
        lenient().when(feedbackRepository.findByEvaluationId(SELF_ID))
                .thenReturn(List.of(selfFeedback));
        lenient().when(feedbackRepository.findByEvaluationId(MANAGER_ID))
                .thenReturn(List.of(selectedManagerFeedback, hiddenManagerFeedback));
    }

    @Test
    void hrManagerPublishesSetFromSelfEntry() {
        var response = service.publish(SELF_ID, request(null, SELECTED_MANAGER_FEEDBACK_ID));

        assertEquals(SELF_ID, response.selfEvaluationId());
        assertEquals(MANAGER_ID, response.managerEvaluationId());
        assertEquals(EvaluationStatus.PUBLISHED, response.selfStatus());
        assertFalse(response.idempotent());
    }

    @Test
    void hrManagerWithSystemAdminPublishesFromManagerEntry() {
        givenRoles(RoleType.HR_MANAGER, RoleType.SYSTEM_ADMIN);
        assertEquals(TARGET_ID,
                service.publish(MANAGER_ID, request(null, SELECTED_MANAGER_FEEDBACK_ID))
                        .targetEmployeeId());
    }

    @ParameterizedTest
    @MethodSource("deniedRoles")
    void roleWithoutHrManagerIsDenied(Set<RoleType> roles) {
        givenRoles(roles.toArray(RoleType[]::new));
        assertError(EvaluationErrorCode.EVALUATION_ACCESS_DENIED,
                () -> service.publish(SELF_ID, request(null)));
    }

    static List<Set<RoleType>> deniedRoles() {
        return List.of(
                Set.of(RoleType.SYSTEM_ADMIN),
                Set.of(RoleType.MANAGER),
                Set.of(RoleType.EMPLOYEE));
    }

    @Test
    void missingEntryEvaluationIsReported() {
        when(evaluationRepository.findById(99L)).thenReturn(Optional.empty());
        assertError(EvaluationErrorCode.EVALUATION_NOT_FOUND,
                () -> service.publish(99L, request(null)));
    }

    @Test
    void incompleteOrDuplicateSetIsConflict() {
        when(evaluationRepository.findByEvaluationCycleIdAndTargetEmployeeId(CYCLE_ID, TARGET_ID))
                .thenReturn(List.of(self));
        assertConflict();
        Evaluation duplicateSelf = mock(Evaluation.class);
        when(duplicateSelf.getEvaluationType()).thenReturn(EvaluationType.SELF);
        when(evaluationRepository.findByEvaluationCycleIdAndTargetEmployeeId(CYCLE_ID, TARGET_ID))
                .thenReturn(List.of(self, manager, duplicateSelf));
        assertConflict();
    }

    @Test
    void plannedAndOpenCyclesAreNotPublishable() {
        when(cycle.getEndDate()).thenReturn(TODAY.plusDays(1));
        assertNotPublishable();
        when(cycle.getEndDate()).thenReturn(TODAY);
        assertNotPublishable();
    }

    @Test
    void closedCyclePublishesDespiteFuturePlannedDateAndSnapshot() {
        service.publish(SELF_ID, request(null, SELECTED_MANAGER_FEEDBACK_ID));
        verify(cycle, never()).getPlannedPublishDate();
        verify(cycle, never()).getCycleStatus();
    }

    @ParameterizedTest
    @EnumSource(value = EvaluationStatus.class, names = {"DRAFT", "RETURNED"})
    void nonSubmittedStatusIsNotPublishable(EvaluationStatus status) {
        when(self.getEvaluationStatus()).thenReturn(status);
        assertNotPublishable();
    }

    @Test
    void mixedPublishedAndSubmittedIsConflict() {
        when(self.getEvaluationStatus()).thenReturn(EvaluationStatus.PUBLISHED);
        assertConflict();
    }

    @Test
    void bothPublishedIsIdempotentAndDoesNotWrite() {
        when(self.getEvaluationStatus()).thenReturn(EvaluationStatus.PUBLISHED);
        when(manager.getEvaluationStatus()).thenReturn(EvaluationStatus.PUBLISHED);
        when(self.getPublishedAt()).thenReturn(PUBLISHED_AT);
        when(selectedManagerFeedback.getIsVisibleToEmployee()).thenReturn(true);

        var response = service.publish(SELF_ID, request("ignored", HIDDEN_MANAGER_FEEDBACK_ID));

        assertTrue(response.idempotent());
        assertEquals(List.of(SELECTED_MANAGER_FEEDBACK_ID, HIDDEN_MANAGER_FEEDBACK_ID),
                response.visibleManagerFeedbackIds());
        verify(self, never()).setEvaluationStatus(any());
        verify(manager, never()).setEvaluationStatus(any());
        verify(historyRepository, never()).saveAll(any());
        verify(auditLogRecordService, never()).record(any());
    }

    @Test
    void feedbackVisibilityIsReplacedWithoutChangingContent() {
        service.publish(SELF_ID, request(null, SELECTED_MANAGER_FEEDBACK_ID));

        verify(selfFeedback).setIsVisibleToEmployee(true);
        verify(selectedManagerFeedback).setIsVisibleToEmployee(true);
        verify(hiddenManagerFeedback).setIsVisibleToEmployee(false);
        verify(selfFeedback, never()).setFeedbackContent(any());
        verify(selectedManagerFeedback, never()).setFeedbackContent(any());
        verify(hiddenManagerFeedback, never()).setFeedbackContent(any());
    }

    @Test
    void invalidMissingSelfOrDuplicateFeedbackIdIsRejectedBeforeChanges() {
        assertFeedbackInvalid(request(null, 999L));
        assertFeedbackInvalid(request(null, SELF_FEEDBACK_ID));
        assertFeedbackInvalid(new EvaluationPublishRequest(
                null, List.of(SELECTED_MANAGER_FEEDBACK_ID, SELECTED_MANAGER_FEEDBACK_ID)));
        verify(self, never()).setEvaluationStatus(any());
    }

    @Test
    void statusAndSamePublishedAtChangeWhileOtherValuesRemainUntouched() {
        service.publish(SELF_ID, request(null));

        verify(self).setEvaluationStatus(EvaluationStatus.PUBLISHED);
        verify(manager).setEvaluationStatus(EvaluationStatus.PUBLISHED);
        verify(self).setPublishedAt(PUBLISHED_AT);
        verify(manager).setPublishedAt(PUBLISHED_AT);
        verify(self, never()).setSubmittedAt(any());
        verify(self, never()).setLastDraftSavedAt(any());
        verify(self, never()).setTotalScore(any());
    }

    @Test
    void historiesContainActorStatusesReasonAndSameTimestamp() {
        service.publish(SELF_ID, request("reason", SELECTED_MANAGER_FEEDBACK_ID));

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(historyRepository).saveAll(captor.capture());
        List<EvaluationPublishHistory> histories = new ArrayList<>();
        captor.getValue().forEach(value -> histories.add((EvaluationPublishHistory) value));
        assertEquals(2, histories.size());
        assertEquals(List.of(SELF_ID, MANAGER_ID),
                histories.stream().map(EvaluationPublishHistory::getEvaluationId).toList());
        histories.forEach(history -> {
            assertEquals(ACTOR_ID, history.getPublishedBy());
            assertEquals(EvaluationStatus.SUBMITTED, history.getPreviousStatus());
            assertEquals(EvaluationStatus.PUBLISHED, history.getPublishedStatus());
            assertEquals("reason", history.getPublishReason());
            assertEquals(PUBLISHED_AT, history.getPublishedAt());
        });
    }

    @Test
    void nullAndFiveHundredCharacterReasonsAreAllowedButFiveHundredOneIsRejected() {
        service.publish(SELF_ID, request(null));
        service.publish(SELF_ID, request("a".repeat(500)));
        assertError(EvaluationErrorCode.EVALUATION_NOT_PUBLISHABLE,
                () -> service.publish(SELF_ID, request("a".repeat(501))));
    }

    @Test
    void auditUsesOfficialContractWithoutSensitiveContents() {
        service.publish(SELF_ID, request(null, SELECTED_MANAGER_FEEDBACK_ID));

        ArgumentCaptor<AuditLogRecordCommand> captor =
                ArgumentCaptor.forClass(AuditLogRecordCommand.class);
        verify(auditLogRecordService).record(captor.capture());
        AuditLogRecordCommand command = captor.getValue();
        assertEquals(ACTOR_ID, command.actorUserId());
        assertEquals(AuditActionType.EVALUATION_RESULT_PUBLISHED, command.actionType());
        assertEquals(SELF_ID, command.targetId());
        assertEquals(Set.of("cycleId", "targetEmployeeId", "selfEvaluationId",
                        "managerEvaluationId", "visibleManagerFeedbackIds"),
                command.changedValue().keySet());
        assertEquals(List.of(SELECTED_MANAGER_FEEDBACK_ID),
                command.changedValue().get("visibleManagerFeedbackIds"));
        assertFalse(command.changedValue().containsValue("selected content"));
    }

    @Test
    void historyAndAuditFailuresPropagateFromTransactionalMethod() throws Exception {
        RuntimeException historyFailure = new RuntimeException("history failed");
        when(historyRepository.saveAll(any())).thenThrow(historyFailure);
        assertSame(historyFailure, assertThrows(RuntimeException.class,
                () -> service.publish(SELF_ID, request(null))));
        verify(auditLogRecordService, never()).record(any());

        doReturn(List.of()).when(historyRepository).saveAll(any());
        RuntimeException auditFailure = new RuntimeException("audit failed");
        doThrow(auditFailure).when(auditLogRecordService).record(any());
        assertSame(auditFailure, assertThrows(RuntimeException.class,
                () -> service.publish(SELF_ID, request(null))));
        assertTrue(EvaluationPublishServiceImpl.class
                .getMethod("publish", Long.class, EvaluationPublishRequest.class)
                .isAnnotationPresent(Transactional.class));
    }

    private void givenEvaluation(Evaluation evaluation, Long id, EvaluationType type) {
        lenient().when(evaluation.getEvaluationId()).thenReturn(id);
        lenient().when(evaluation.getEvaluationCycleId()).thenReturn(CYCLE_ID);
        lenient().when(evaluation.getTargetEmployeeId()).thenReturn(TARGET_ID);
        lenient().when(evaluation.getEvaluationType()).thenReturn(type);
        lenient().when(evaluation.getEvaluationStatus()).thenReturn(EvaluationStatus.SUBMITTED);
        lenient().when(evaluation.getTotalScore()).thenReturn((BigDecimal) null);
    }

    private void givenFeedback(
            EvaluationFeedback feedback,
            Long id,
            Long evaluationId,
            boolean visible,
            String content
    ) {
        lenient().when(feedback.getEvaluationFeedbackId()).thenReturn(id);
        lenient().when(feedback.getEvaluationId()).thenReturn(evaluationId);
        lenient().when(feedback.getIsVisibleToEmployee()).thenReturn(visible);
        lenient().when(feedback.getFeedbackContent()).thenReturn(content);
    }

    private void givenRoles(RoleType... roles) {
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUserContext(
                ACTOR_ID, 10L, Set.of(roles), 20L, 1, null));
    }

    private EvaluationPublishRequest request(String reason, Long... visibleIds) {
        return new EvaluationPublishRequest(reason, List.of(visibleIds));
    }

    private void assertNotPublishable() {
        assertError(EvaluationErrorCode.EVALUATION_NOT_PUBLISHABLE,
                () -> service.publish(SELF_ID, request(null)));
    }

    private void assertConflict() {
        assertError(EvaluationErrorCode.EVALUATION_PUBLISH_CONFLICT,
                () -> service.publish(SELF_ID, request(null)));
    }

    private void assertFeedbackInvalid(EvaluationPublishRequest request) {
        assertError(EvaluationErrorCode.EVALUATION_FEEDBACK_INVALID,
                () -> service.publish(SELF_ID, request));
    }

    private void assertError(EvaluationErrorCode expected, Runnable action) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertSame(expected, exception.getErrorCode());
    }
}
