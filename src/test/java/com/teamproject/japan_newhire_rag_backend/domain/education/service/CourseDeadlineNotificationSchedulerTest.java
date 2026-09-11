package com.teamproject.japan_newhire_rag_backend.domain.education.service;

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
import org.mockito.ArgumentCaptor;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.api.NotificationCommandService;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.api.NotificationSendCommand;

class CourseDeadlineNotificationSchedulerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 10);

    private final CourseEnrollmentRepository enrollmentRepository =
            mock(CourseEnrollmentRepository.class);
    private final OrganizationQueryService organizationQueryService =
            mock(OrganizationQueryService.class);
    private final NotificationCommandService notificationCommandService =
            mock(NotificationCommandService.class);

    private CourseDeadlineNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new CourseDeadlineNotificationScheduler(
                enrollmentRepository,
                organizationQueryService,
                notificationCommandService,
                Clock.fixed(Instant.parse("2026-09-10T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void sendsToIncompleteEnrollmentsDueTomorrow() {
        Course course = mock(Course.class);
        CourseEnrollment enrollment = mock(CourseEnrollment.class);
        when(enrollment.getEmployeeId()).thenReturn(10L);
        when(enrollment.getCourseEnrollmentId()).thenReturn(30L);
        when(enrollment.getEnrollmentDueDate()).thenReturn(TODAY.plusDays(1));
        when(enrollment.getCourse()).thenReturn(course);
        when(course.getCourseName()).thenReturn("필수 교육");
        when(enrollmentRepository.findAllByEnrollmentDueDateAndEnrollmentStatusNot(
                TODAY.plusDays(1), EnrollmentStatus.COMPLETED)).thenReturn(List.of(enrollment));
        when(organizationQueryService.findAppUserIdsByEmployeeIds(List.of(10L)))
                .thenReturn(Map.of(10L, 100L));

        scheduler.sendDeadlineImminentNotifications(TODAY);

        verify(notificationCommandService).send(new NotificationSendCommand(
                100L, "COURSE_DEADLINE_IMMINENT", "교육 마감이 임박했습니다",
                "'필수 교육' 교육 마감일은 2026-09-11입니다.",
                "COURSE_ENROLLMENT", 30L));
    }

    @Test
    void queriesOnlyIncompleteEnrollmentsForTomorrow() {
        when(enrollmentRepository.findAllByEnrollmentDueDateAndEnrollmentStatusNot(
                TODAY.plusDays(1), EnrollmentStatus.COMPLETED)).thenReturn(List.of());
        when(organizationQueryService.findAppUserIdsByEmployeeIds(List.of()))
                .thenReturn(Map.of());

        scheduler.sendDeadlineImminentNotifications(TODAY);

        verify(enrollmentRepository).findAllByEnrollmentDueDateAndEnrollmentStatusNot(
                TODAY.plusDays(1), EnrollmentStatus.COMPLETED);
        verify(notificationCommandService, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotSendWhenNoEnrollmentMatchesTheScheduledDate() {
        LocalDate anotherDay = TODAY.plusDays(3);
        when(enrollmentRepository.findAllByEnrollmentDueDateAndEnrollmentStatusNot(
                anotherDay.plusDays(1), EnrollmentStatus.COMPLETED)).thenReturn(List.of());
        when(organizationQueryService.findAppUserIdsByEmployeeIds(List.of()))
                .thenReturn(Map.of());

        scheduler.sendDeadlineImminentNotifications(anotherDay);

        verify(notificationCommandService, never()).send(org.mockito.ArgumentMatchers.any());
    }
}
