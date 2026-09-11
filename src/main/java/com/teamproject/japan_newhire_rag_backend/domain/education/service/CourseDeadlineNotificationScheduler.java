package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.api.NotificationCommandService;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.api.NotificationSendCommand;

@Service
public class CourseDeadlineNotificationScheduler {

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final OrganizationQueryService organizationQueryService;
    private final NotificationCommandService notificationCommandService;
    private final Clock clock;

    public CourseDeadlineNotificationScheduler(
            CourseEnrollmentRepository courseEnrollmentRepository,
            OrganizationQueryService organizationQueryService,
            NotificationCommandService notificationCommandService,
            Clock clock
    ) {
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.organizationQueryService = organizationQueryService;
        this.notificationCommandService = notificationCommandService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Tokyo")
    @Transactional
    public void sendDeadlineImminentNotifications() {
        sendDeadlineImminentNotifications(LocalDate.now(clock));
    }

    void sendDeadlineImminentNotifications(LocalDate today) {
        List<CourseEnrollment> enrollments = courseEnrollmentRepository
                .findAllByEnrollmentDueDateAndEnrollmentStatusNot(
                        today.plusDays(1), EnrollmentStatus.COMPLETED);
        Map<Long, Long> appUserIds = organizationQueryService
                .findAppUserIdsByEmployeeIds(enrollments.stream()
                        .map(CourseEnrollment::getEmployeeId)
                        .distinct()
                        .toList());

        enrollments.forEach(enrollment -> {
            Long appUserId = appUserIds.get(enrollment.getEmployeeId());
            if (appUserId == null) {
                return;
            }
            notificationCommandService.send(new NotificationSendCommand(
                    appUserId,
                    "COURSE_DEADLINE_IMMINENT",
                    "교육 마감이 임박했습니다",
                    "'" + enrollment.getCourse().getCourseName()
                            + "' 교육 마감일은 " + enrollment.getEnrollmentDueDate() + "입니다.",
                    "COURSE_ENROLLMENT",
                    enrollment.getCourseEnrollmentId()));
        });
    }
}
