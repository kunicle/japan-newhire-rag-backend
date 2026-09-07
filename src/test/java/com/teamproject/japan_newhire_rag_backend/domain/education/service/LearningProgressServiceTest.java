package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.LearningProgressUpdateResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseModule;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.LearningProgress;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.LearningCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.LearningProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseModuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;

@ExtendWith(MockitoExtension.class)
class LearningProgressServiceTest {

    private static final Long EMPLOYEE_ID = 10L;
    private static final Long PROGRESS_ID = 1000L;

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-10T09:00:00Z"),
            ZoneOffset.UTC);

    private static final LocalDate TODAY =
            LocalDate.now(FIXED_CLOCK);

    private static final LocalDateTime NOW =
            LocalDateTime.now(FIXED_CLOCK);

    @Mock
    private LearningProgressRepository learningProgressRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private CourseModuleRepository courseModuleRepository;

    private LearningProgressService learningProgressService;

    @BeforeEach
    void setUp() {
        learningProgressService = new LearningProgressService(
                learningProgressRepository,
                courseModuleRepository,
                currentUserProvider,
                FIXED_CLOCK);
    }

    @Test
    void firstModuleStartChangesProgressAndEnrollmentToInProgress() {
        stubCurrentUser();

        CourseEnrollment enrollment = enrollment(
                EMPLOYEE_ID,
                EnrollmentStatus.NOT_STARTED,
                TODAY.plusDays(1));

        LearningProgress progress = progress(
                enrollment,
                LearningCompletionStatus.NOT_STARTED);

        when(learningProgressRepository
                .findByLearningProgressId(PROGRESS_ID))
                .thenReturn(Optional.of(progress));

        LearningProgressUpdateResponse response =
                learningProgressService.startProgress(PROGRESS_ID);

        assertThat(progress.getCompletionStatus())
                .isEqualTo(LearningCompletionStatus.IN_PROGRESS);
        assertThat(progress.getStartedAt()).isEqualTo(NOW);

        assertThat(enrollment.getEnrollmentStatus())
                .isEqualTo(EnrollmentStatus.IN_PROGRESS);

        assertThat(response.completionStatus())
                .isEqualTo(LearningCompletionStatus.IN_PROGRESS);
        assertThat(response.enrollmentStatus())
                .isEqualTo(EnrollmentStatus.IN_PROGRESS);
        assertThat(response.startedAt()).isEqualTo(NOW);
    }

    @Test
    void startingAfterDueDateChangesEnrollmentToOverdue() {
        stubCurrentUser();

        CourseEnrollment enrollment = enrollment(
                EMPLOYEE_ID,
                EnrollmentStatus.NOT_STARTED,
                TODAY.minusDays(1));

        LearningProgress progress = progress(
                enrollment,
                LearningCompletionStatus.NOT_STARTED);

        when(learningProgressRepository
                .findByLearningProgressId(PROGRESS_ID))
                .thenReturn(Optional.of(progress));

        LearningProgressUpdateResponse response =
                learningProgressService.startProgress(PROGRESS_ID);

        assertThat(progress.getCompletionStatus())
                .isEqualTo(LearningCompletionStatus.IN_PROGRESS);
        assertThat(progress.getStartedAt()).isEqualTo(NOW);

        assertThat(enrollment.getEnrollmentStatus())
                .isEqualTo(EnrollmentStatus.OVERDUE);
        assertThat(response.enrollmentStatus())
                .isEqualTo(EnrollmentStatus.OVERDUE);
    }

    @Test
    void repeatedStartKeepsOriginalStartedAt() {
        stubCurrentUser();

        LocalDateTime originalStartedAt = NOW.minusHours(2);

        CourseEnrollment enrollment = enrollment(
                EMPLOYEE_ID,
                EnrollmentStatus.IN_PROGRESS,
                TODAY.plusDays(1));

        LearningProgress progress = progress(
                enrollment,
                LearningCompletionStatus.IN_PROGRESS);

        set(progress, "startedAt", originalStartedAt);

        when(learningProgressRepository
                .findByLearningProgressId(PROGRESS_ID))
                .thenReturn(Optional.of(progress));

        LearningProgressUpdateResponse response =
                learningProgressService.startProgress(PROGRESS_ID);

        assertThat(progress.getCompletionStatus())
                .isEqualTo(LearningCompletionStatus.IN_PROGRESS);
        assertThat(progress.getStartedAt())
                .isEqualTo(originalStartedAt);
        assertThat(response.startedAt())
                .isEqualTo(originalStartedAt);
    }

    @Test
    void startingCompletedProgressKeepsCompletionData() {
        stubCurrentUser();

        LocalDateTime originalStartedAt = NOW.minusDays(2);
        LocalDateTime originalCompletedAt = NOW.minusDays(1);

        CourseEnrollment enrollment = enrollment(
                EMPLOYEE_ID,
                EnrollmentStatus.COMPLETED,
                TODAY.minusDays(5));

        LearningProgress progress = progress(
                enrollment,
                LearningCompletionStatus.COMPLETED);

        set(progress, "startedAt", originalStartedAt);
        set(progress, "completedAt", originalCompletedAt);

        when(learningProgressRepository
                .findByLearningProgressId(PROGRESS_ID))
                .thenReturn(Optional.of(progress));

        LearningProgressUpdateResponse response =
                learningProgressService.startProgress(PROGRESS_ID);

        assertThat(progress.getCompletionStatus())
                .isEqualTo(LearningCompletionStatus.COMPLETED);
        assertThat(progress.getStartedAt())
                .isEqualTo(originalStartedAt);
        assertThat(progress.getCompletedAt())
                .isEqualTo(originalCompletedAt);

        assertThat(enrollment.getEnrollmentStatus())
                .isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(response.enrollmentStatus())
                .isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    void rejectsAnotherEmployeesProgress() {
        stubCurrentUser();

        CourseEnrollment enrollment = enrollment(
                999L,
                EnrollmentStatus.NOT_STARTED,
                TODAY.plusDays(1));

        LearningProgress progress = progress(
                enrollment,
                LearningCompletionStatus.NOT_STARTED);

        when(learningProgressRepository
                .findByLearningProgressId(PROGRESS_ID))
                .thenReturn(Optional.of(progress));

        assertThatThrownBy(() ->
                learningProgressService.startProgress(PROGRESS_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(ErrorCode.FORBIDDEN));

        assertThat(progress.getCompletionStatus())
                .isEqualTo(LearningCompletionStatus.NOT_STARTED);
    }

    @Test
    void throwsNotFoundWhenProgressDoesNotExist() {
        stubCurrentUser();

        when(learningProgressRepository
                .findByLearningProgressId(PROGRESS_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                learningProgressService.startProgress(PROGRESS_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(
                                        ErrorCode.RESOURCE_NOT_FOUND));
    }

        @Test
        void completingOneOfFourRequiredModulesUpdatesProgressToTwentyFivePercent() {
        stubCurrentUser();

        CourseEnrollment enrollment = enrollment(
                EMPLOYEE_ID,
                EnrollmentStatus.IN_PROGRESS,
                TODAY.plusDays(1));

        LearningProgress progress = progress(
                enrollment,
                LearningCompletionStatus.NOT_STARTED);

        when(learningProgressRepository
                .findByLearningProgressId(PROGRESS_ID))
                .thenReturn(Optional.of(progress));

        when(courseModuleRepository
                .countByCourse_CourseIdAndRequiredTrueAndActiveTrue(50L))
                .thenReturn(4L);

        when(learningProgressRepository
                .countByCourseEnrollment_CourseEnrollmentIdAndCourseModule_RequiredTrueAndCourseModule_ActiveTrueAndCompletionStatus(
                        100L,
                        LearningCompletionStatus.COMPLETED))
                .thenReturn(1L);

        LearningProgressUpdateResponse response =
                learningProgressService.completeProgress(PROGRESS_ID);

        assertThat(progress.getCompletionStatus())
                .isEqualTo(LearningCompletionStatus.COMPLETED);
        assertThat(progress.getStartedAt()).isEqualTo(NOW);
        assertThat(progress.getCompletedAt()).isEqualTo(NOW);

        assertThat(enrollment.getProgressRate())
                .isEqualByComparingTo("25.00");
        assertThat(enrollment.getEnrollmentStatus())
                .isEqualTo(EnrollmentStatus.IN_PROGRESS);
        assertThat(enrollment.getCompletedAt()).isNull();

        assertThat(response.progressRate())
                .isEqualByComparingTo("25.00");
        assertThat(response.enrollmentStatus())
                .isEqualTo(EnrollmentStatus.IN_PROGRESS);
        }

        @Test
        void completingOptionalModuleDoesNotIncreaseRequiredProgressRate() {
        stubCurrentUser();

        CourseEnrollment enrollment = enrollment(
                EMPLOYEE_ID,
                EnrollmentStatus.IN_PROGRESS,
                TODAY.plusDays(1));

        set(enrollment, "progressRate", new BigDecimal("25.00"));

        LearningProgress progress = progress(
                enrollment,
                LearningCompletionStatus.NOT_STARTED);

        set(progress.getCourseModule(), "required", false);

        when(learningProgressRepository
                .findByLearningProgressId(PROGRESS_ID))
                .thenReturn(Optional.of(progress));

        when(courseModuleRepository
                .countByCourse_CourseIdAndRequiredTrueAndActiveTrue(50L))
                .thenReturn(4L);

        // 선택 모듈은 완료됐지만 완료된 필수 모듈 수는 여전히 1개다.
        when(learningProgressRepository
                .countByCourseEnrollment_CourseEnrollmentIdAndCourseModule_RequiredTrueAndCourseModule_ActiveTrueAndCompletionStatus(
                        100L,
                        LearningCompletionStatus.COMPLETED))
                .thenReturn(1L);

        LearningProgressUpdateResponse response =
                learningProgressService.completeProgress(PROGRESS_ID);

        assertThat(progress.getCompletionStatus())
                .isEqualTo(LearningCompletionStatus.COMPLETED);
        assertThat(enrollment.getProgressRate())
                .isEqualByComparingTo("25.00");
        assertThat(response.progressRate())
                .isEqualByComparingTo("25.00");
        assertThat(enrollment.getEnrollmentStatus())
                .isEqualTo(EnrollmentStatus.IN_PROGRESS);
        }

        @Test
        void completingLastRequiredModuleCompletesEnrollment() {
        stubCurrentUser();

        CourseEnrollment enrollment = enrollment(
                EMPLOYEE_ID,
                EnrollmentStatus.IN_PROGRESS,
                TODAY.plusDays(1));

        set(enrollment, "progressRate", new BigDecimal("75.00"));

        LearningProgress progress = progress(
                enrollment,
                LearningCompletionStatus.NOT_STARTED);

        when(learningProgressRepository
                .findByLearningProgressId(PROGRESS_ID))
                .thenReturn(Optional.of(progress));

        when(courseModuleRepository
                .countByCourse_CourseIdAndRequiredTrueAndActiveTrue(50L))
                .thenReturn(4L);

        when(learningProgressRepository
                .countByCourseEnrollment_CourseEnrollmentIdAndCourseModule_RequiredTrueAndCourseModule_ActiveTrueAndCompletionStatus(
                        100L,
                        LearningCompletionStatus.COMPLETED))
                .thenReturn(4L);

        LearningProgressUpdateResponse response =
                learningProgressService.completeProgress(PROGRESS_ID);

        assertThat(enrollment.getProgressRate())
                .isEqualByComparingTo("100.00");
        assertThat(enrollment.getEnrollmentStatus())
                .isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(enrollment.getCompletedAt()).isEqualTo(NOW);

        assertThat(response.progressRate())
                .isEqualByComparingTo("100.00");
        assertThat(response.enrollmentStatus())
                .isEqualTo(EnrollmentStatus.COMPLETED);
        }

        @Test
        void completingLastRequiredModuleChangesOverdueToCompleted() {
        stubCurrentUser();

        CourseEnrollment enrollment = enrollment(
                EMPLOYEE_ID,
                EnrollmentStatus.OVERDUE,
                TODAY.minusDays(1));

        LearningProgress progress = progress(
                enrollment,
                LearningCompletionStatus.NOT_STARTED);

        when(learningProgressRepository
                .findByLearningProgressId(PROGRESS_ID))
                .thenReturn(Optional.of(progress));

        when(courseModuleRepository
                .countByCourse_CourseIdAndRequiredTrueAndActiveTrue(50L))
                .thenReturn(1L);

        when(learningProgressRepository
                .countByCourseEnrollment_CourseEnrollmentIdAndCourseModule_RequiredTrueAndCourseModule_ActiveTrueAndCompletionStatus(
                        100L,
                        LearningCompletionStatus.COMPLETED))
                .thenReturn(1L);

        LearningProgressUpdateResponse response =
                learningProgressService.completeProgress(PROGRESS_ID);

        assertThat(progress.getCompletionStatus())
                .isEqualTo(LearningCompletionStatus.COMPLETED);
        assertThat(enrollment.getEnrollmentStatus())
                .isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(enrollment.getProgressRate())
                .isEqualByComparingTo("100.00");
        assertThat(enrollment.getCompletedAt()).isEqualTo(NOW);
        assertThat(response.enrollmentStatus())
                .isEqualTo(EnrollmentStatus.COMPLETED);
        }

        @Test
        void repeatedCompletionKeepsOriginalCompletionTimeAndProgress() {
        stubCurrentUser();

        LocalDateTime originalStartedAt = NOW.minusDays(2);
        LocalDateTime originalCompletedAt = NOW.minusDays(1);

        CourseEnrollment enrollment = enrollment(
                EMPLOYEE_ID,
                EnrollmentStatus.IN_PROGRESS,
                TODAY.plusDays(1));

        set(enrollment, "progressRate", new BigDecimal("25.00"));

        LearningProgress progress = progress(
                enrollment,
                LearningCompletionStatus.COMPLETED);

        set(progress, "startedAt", originalStartedAt);
        set(progress, "completedAt", originalCompletedAt);

        when(learningProgressRepository
                .findByLearningProgressId(PROGRESS_ID))
                .thenReturn(Optional.of(progress));

        LearningProgressUpdateResponse response =
                learningProgressService.completeProgress(PROGRESS_ID);

        assertThat(progress.getStartedAt())
                .isEqualTo(originalStartedAt);
        assertThat(progress.getCompletedAt())
                .isEqualTo(originalCompletedAt);
        assertThat(enrollment.getProgressRate())
                .isEqualByComparingTo("25.00");

        assertThat(response.startedAt())
                .isEqualTo(originalStartedAt);
        assertThat(response.completedAt())
                .isEqualTo(originalCompletedAt);
        assertThat(response.progressRate())
                .isEqualByComparingTo("25.00");

        verify(courseModuleRepository, never())
                .countByCourse_CourseIdAndRequiredTrueAndActiveTrue(
                        any());

        verify(learningProgressRepository, never())
                .countByCourseEnrollment_CourseEnrollmentIdAndCourseModule_RequiredTrueAndCourseModule_ActiveTrueAndCompletionStatus(
                        any(),
                        any());
        }

    @Test
    void rejectsInvalidProgressIdBeforeAuthentication() {
        assertThatThrownBy(() ->
                learningProgressService.startProgress(0L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_REQUEST));

        verifyNoInteractions(
                currentUserProvider,
                learningProgressRepository);
    }

    private void stubCurrentUser() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(
                        1L,
                        EMPLOYEE_ID,
                        Set.of(RoleType.EMPLOYEE),
                        null,
                        null,
                        null));
    }

    private CourseEnrollment enrollment(
            Long employeeId,
            EnrollmentStatus status,
            LocalDate dueDate
    ) {
        Course course = newEntity(Course.class);
        set(course, "courseId", 50L);

        CourseEnrollment enrollment =
            newEntity(CourseEnrollment.class);

        set(enrollment, "courseEnrollmentId", 100L);
        set(enrollment, "course", course);
        set(enrollment, "employeeId", employeeId);
        set(enrollment, "enrollmentStatus", status);
        set(enrollment, "progressRate", BigDecimal.ZERO);
        set(enrollment, "enrollmentStartDate",
                TODAY.minusDays(10));
        set(enrollment, "enrollmentDueDate", dueDate);

        return enrollment;
    }

    private LearningProgress progress(
            CourseEnrollment enrollment,
            LearningCompletionStatus status
    ) {
        CourseModule module = newEntity(CourseModule.class);

        set(module, "courseModuleId", 200L);
        set(module, "moduleOrder", 1);
        set(module, "required", true);
        set(module, "active", true);

        LearningProgress progress =
                newEntity(LearningProgress.class);

        set(progress, "learningProgressId", PROGRESS_ID);
        set(progress, "courseEnrollment", enrollment);
        set(progress, "courseModule", module);
        set(progress, "completionStatus", status);

        return progress;
    }

    private static <T> T newEntity(Class<T> type) {
        try {
            Constructor<T> constructor =
                    type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Could not create test entity: "
                            + type.getSimpleName(),
                    exception);
        }
    }

    private static void set(
            Object target,
            String fieldName,
            Object value
    ) {
        ReflectionTestUtils.setField(
                target,
                fieldName,
                value);
    }
}