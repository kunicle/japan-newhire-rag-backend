package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.api.NotificationCommandService;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.api.NotificationSendCommand;
import com.teamproject.japan_newhire_rag_backend.evaluation.Evaluation;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;

class EvaluationStartNotificationSchedulerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 10);

    private final EvaluationCycleRepository cycleRepository = mock(EvaluationCycleRepository.class);
    private final EvaluationRepository evaluationRepository = mock(EvaluationRepository.class);
    private final OrganizationQueryService organizationQueryService = mock(OrganizationQueryService.class);
    private final NotificationCommandService notificationCommandService = mock(NotificationCommandService.class);

    private EvaluationStartNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new EvaluationStartNotificationScheduler(
                cycleRepository, evaluationRepository, organizationQueryService,
                notificationCommandService,
                Clock.fixed(Instant.parse("2026-09-10T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void notifiesDistinctSelfEvaluationTargetsForStartedCycle() {
        EvaluationCycle cycle = mock(EvaluationCycle.class);
        Evaluation selfFirst = evaluation(10L, EvaluationType.SELF);
        Evaluation selfDuplicate = evaluation(10L, EvaluationType.SELF);
        Evaluation selfSecond = evaluation(20L, EvaluationType.SELF);
        Evaluation manager = evaluation(10L, EvaluationType.MANAGER);
        when(cycle.getEvaluationCycleId()).thenReturn(7L);
        when(cycle.getCycleName()).thenReturn("2026 하반기 평가");
        when(cycleRepository.findAllByStartDateAndDeletedAtIsNull(TODAY))
                .thenReturn(List.of(cycle));
        when(evaluationRepository.findByEvaluationCycleId(7L))
                .thenReturn(List.of(selfFirst, selfDuplicate, selfSecond, manager));
        when(organizationQueryService.findAppUserIdsByEmployeeIds(List.of(10L, 20L)))
                .thenReturn(Map.of(10L, 100L, 20L, 200L));

        scheduler.sendEvaluationStartedNotifications(TODAY);

        verify(notificationCommandService).send(new NotificationSendCommand(
                100L, "EVALUATION_STARTED", "평가가 시작되었습니다",
                "'2026 하반기 평가' 인사평가가 시작되었습니다.", "EVALUATION_CYCLE", 7L));
        verify(notificationCommandService).send(new NotificationSendCommand(
                200L, "EVALUATION_STARTED", "평가가 시작되었습니다",
                "'2026 하반기 평가' 인사평가가 시작되었습니다.", "EVALUATION_CYCLE", 7L));
    }

    @Test
    void doesNotSendWhenNoCycleStartsToday() {
        when(cycleRepository.findAllByStartDateAndDeletedAtIsNull(TODAY)).thenReturn(List.of());

        scheduler.sendEvaluationStartedNotifications(TODAY);

        verify(notificationCommandService, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void queriesOnlyCyclesStartingOnTheGivenDate() {
        LocalDate anotherDay = TODAY.plusDays(1);
        when(cycleRepository.findAllByStartDateAndDeletedAtIsNull(anotherDay))
                .thenReturn(List.of());

        scheduler.sendEvaluationStartedNotifications(anotherDay);

        verify(cycleRepository).findAllByStartDateAndDeletedAtIsNull(anotherDay);
        verify(notificationCommandService, never()).send(org.mockito.ArgumentMatchers.any());
    }

    private Evaluation evaluation(Long targetEmployeeId, EvaluationType type) {
        Evaluation evaluation = mock(Evaluation.class);
        when(evaluation.getTargetEmployeeId()).thenReturn(targetEmployeeId);
        when(evaluation.getEvaluationType()).thenReturn(type);
        return evaluation;
    }
}
