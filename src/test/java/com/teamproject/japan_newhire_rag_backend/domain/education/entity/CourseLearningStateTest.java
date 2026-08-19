package com.teamproject.japan_newhire_rag_backend.domain.education.entity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.LearningCompletionStatus;

class CourseLearningStateTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-10T09:00:00Z"),
            ZoneOffset.UTC);

    private static final LocalDate TODAY =
            LocalDate.now(FIXED_CLOCK);

    private static final LocalDateTime NOW =
            LocalDateTime.now(FIXED_CLOCK);

    @Test
    void dueDateTodayIsNotOverdue() {
        CourseEnrollment enrollment = enrollment(
                EnrollmentStatus.NOT_STARTED,
                TODAY);

        EnrollmentStatus effectiveStatus =
                enrollment.getEffectiveStatus(TODAY);

        assertThat(effectiveStatus)
                .isEqualTo(EnrollmentStatus.NOT_STARTED);
        assertThat(enrollment.getEnrollmentStatus())
                .isEqualTo(EnrollmentStatus.NOT_STARTED);
    }

    @Test
    void overdueIsCalculatedWithoutChangingPersistedStatus() {
        CourseEnrollment enrollment = enrollment(
                EnrollmentStatus.IN_PROGRESS,
                TODAY.minusDays(1));

        EnrollmentStatus effectiveStatus =
                enrollment.getEffectiveStatus(TODAY);

        assertThat(effectiveStatus)
                .isEqualTo(EnrollmentStatus.OVERDUE);

        // GET 조회 정책: effective status만 계산하고 저장 상태는 변경하지 않는다.
        assertThat(enrollment.getEnrollmentStatus())
                .isEqualTo(EnrollmentStatus.IN_PROGRESS);
    }

    @Test
    void completedEnrollmentNeverBecomesOverdue() {
        CourseEnrollment enrollment = enrollment(
                EnrollmentStatus.COMPLETED,
                TODAY.minusDays(1));

        assertThat(enrollment.getEffectiveStatus(TODAY))
                .isEqualTo(EnrollmentStatus.COMPLETED);

        assertThat(enrollment.applyOverdueIfNeeded(TODAY))
                .isFalse();

        assertThat(enrollment.getEnrollmentStatus())
                .isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    void startingBeforeDueDateChangesEnrollmentToInProgress() {
        CourseEnrollment enrollment = enrollment(
                EnrollmentStatus.NOT_STARTED,
                TODAY.plusDays(1));

        boolean changed = enrollment.startLearning(TODAY);

        assertThat(changed).isTrue();
        assertThat(enrollment.getEnrollmentStatus())
                .isEqualTo(EnrollmentStatus.IN_PROGRESS);
    }

    @Test
    void startingAfterDueDateKeepsEnrollmentOverdue() {
        CourseEnrollment enrollment = enrollment(
                EnrollmentStatus.NOT_STARTED,
                TODAY.minusDays(1));

        boolean changed = enrollment.startLearning(TODAY);

        assertThat(changed).isFalse();
        assertThat(enrollment.getEnrollmentStatus())
                .isEqualTo(EnrollmentStatus.OVERDUE);
    }

    @Test
    void enrollmentCompletionIsIdempotent() {
        CourseEnrollment enrollment = enrollment(
                EnrollmentStatus.OVERDUE,
                TODAY.minusDays(1));

        boolean firstResult = enrollment.complete(NOW);
        boolean secondResult = enrollment.complete(NOW.plusHours(1));

        assertThat(firstResult).isTrue();
        assertThat(secondResult).isFalse();
        assertThat(enrollment.getEnrollmentStatus())
                .isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(enrollment.getProgressRate())
                .isEqualByComparingTo("100.00");
        assertThat(enrollment.getCompletedAt())
                .isEqualTo(NOW);
    }

    @Test
    void learningProgressStartIsIdempotent() {
        LearningProgress progress = new LearningProgress();

        boolean firstResult = progress.start(NOW);
        boolean secondResult = progress.start(NOW.plusHours(1));

        assertThat(firstResult).isTrue();
        assertThat(secondResult).isFalse();
        assertThat(progress.getCompletionStatus())
                .isEqualTo(LearningCompletionStatus.IN_PROGRESS);
        assertThat(progress.getStartedAt())
                .isEqualTo(NOW);
    }

    @Test
    void learningProgressCompletionIsIdempotent() {
        LearningProgress progress = new LearningProgress();

        boolean firstResult = progress.complete(NOW);
        boolean secondResult = progress.complete(NOW.plusHours(1));

        assertThat(firstResult).isTrue();
        assertThat(secondResult).isFalse();
        assertThat(progress.getCompletionStatus())
                .isEqualTo(LearningCompletionStatus.COMPLETED);
        assertThat(progress.getStartedAt())
                .isEqualTo(NOW);
        assertThat(progress.getCompletedAt())
                .isEqualTo(NOW);
    }

    private CourseEnrollment enrollment(
            EnrollmentStatus status,
            LocalDate dueDate
    ) {
        CourseEnrollment enrollment = new CourseEnrollment();

        ReflectionTestUtils.setField(
                enrollment,
                "enrollmentStatus",
                status);
        ReflectionTestUtils.setField(
                enrollment,
                "enrollmentDueDate",
                dueDate);

        return enrollment;
    }
}