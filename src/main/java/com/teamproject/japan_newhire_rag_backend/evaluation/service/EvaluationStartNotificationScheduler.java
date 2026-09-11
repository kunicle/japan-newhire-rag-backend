package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.api.NotificationCommandService;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.api.NotificationSendCommand;
import com.teamproject.japan_newhire_rag_backend.evaluation.Evaluation;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;

@Service
public class EvaluationStartNotificationScheduler {

    private final EvaluationCycleRepository evaluationCycleRepository;
    private final EvaluationRepository evaluationRepository;
    private final OrganizationQueryService organizationQueryService;
    private final NotificationCommandService notificationCommandService;
    private final Clock clock;

    public EvaluationStartNotificationScheduler(
            EvaluationCycleRepository evaluationCycleRepository,
            EvaluationRepository evaluationRepository,
            OrganizationQueryService organizationQueryService,
            NotificationCommandService notificationCommandService,
            Clock clock
    ) {
        this.evaluationCycleRepository = evaluationCycleRepository;
        this.evaluationRepository = evaluationRepository;
        this.organizationQueryService = organizationQueryService;
        this.notificationCommandService = notificationCommandService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Tokyo")
    @Transactional
    public void sendEvaluationStartedNotifications() {
        sendEvaluationStartedNotifications(LocalDate.now(clock));
    }

    void sendEvaluationStartedNotifications(LocalDate today) {
        evaluationCycleRepository.findAllByStartDateAndDeletedAtIsNull(today)
                .forEach(this::sendForCycle);
    }

    private void sendForCycle(EvaluationCycle cycle) {
        List<Long> employeeIds = evaluationRepository
                .findByEvaluationCycleId(cycle.getEvaluationCycleId())
                .stream()
                .filter(evaluation -> evaluation.getEvaluationType() == EvaluationType.SELF)
                .map(Evaluation::getTargetEmployeeId)
                .filter(employeeId -> employeeId != null)
                .distinct()
                .sorted()
                .toList();
        Map<Long, Long> appUserIds = organizationQueryService
                .findAppUserIdsByEmployeeIds(employeeIds);

        employeeIds.forEach(employeeId -> {
            Long appUserId = appUserIds.get(employeeId);
            if (appUserId == null) {
                return;
            }
            notificationCommandService.send(new NotificationSendCommand(
                    appUserId,
                    "EVALUATION_STARTED",
                    "평가가 시작되었습니다",
                    "'" + cycle.getCycleName() + "' 인사평가가 시작되었습니다.",
                    "EVALUATION_CYCLE",
                    cycle.getEvaluationCycleId()));
        });
    }
}
