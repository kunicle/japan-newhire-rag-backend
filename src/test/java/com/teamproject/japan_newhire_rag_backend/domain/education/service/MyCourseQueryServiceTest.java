package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.MyCourseDetailResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.MyCoursePageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseModule;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.LearningProgress;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.LearningCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.LearningProgressRepository;

@ExtendWith(MockitoExtension.class)
class MyCourseQueryServiceTest {

    private static final Long EMPLOYEE_ID = 10L;

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-10T09:00:00Z"),
            ZoneOffset.UTC);

    private static final LocalDate TODAY =
            LocalDate.now(FIXED_CLOCK);

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private LearningProgressRepository learningProgressRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private MyCourseQueryService myCourseQueryService;

    @BeforeEach
    void setUp() {
        myCourseQueryService = new MyCourseQueryService(
                courseEnrollmentRepository,
                learningProgressRepository,
                currentUserProvider,
                FIXED_CLOCK);
    }

    @Test
    void getsOnlyCurrentEmployeesCoursesWithEffectiveOverdueStatus() {
        stubCurrentUser();
        CourseEnrollment enrollment = enrollment(
                100L,
                EMPLOYEE_ID,
                EnrollmentStatus.IN_PROGRESS,
                TODAY.minusDays(1));

        when(courseEnrollmentRepository.findAllByEmployeeId(
                eq(EMPLOYEE_ID),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(enrollment),
                        PageRequest.of(0, 20),
                        1));

        MyCoursePageResponse response =
                myCourseQueryService.getMyCourses(0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).enrollmentId())
                .isEqualTo(100L);
        assertThat(response.content().get(0).courseName())
                .isEqualTo("New hire course");
        assertThat(response.content().get(0).status())
                .isEqualTo(EnrollmentStatus.OVERDUE);

        // GET에서는 응답 상태만 OVERDUE로 계산하고 DB 상태는 변경하지 않는다.
        assertThat(enrollment.getEnrollmentStatus())
                .isEqualTo(EnrollmentStatus.IN_PROGRESS);

        verify(courseEnrollmentRepository, never())
                .save(any(CourseEnrollment.class));
    }

    @Test
    void returnsEmptyPageWhenEmployeeHasNoCourses() {
        stubCurrentUser();
        when(courseEnrollmentRepository.findAllByEmployeeId(
                eq(EMPLOYEE_ID),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(),
                        PageRequest.of(0, 20),
                        0));

        MyCoursePageResponse response =
                myCourseQueryService.getMyCourses(0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
    }

    @Test
    void getsOwnedCourseDetailWithActiveModulesInRepositoryOrder() {
        stubCurrentUser();
        CourseEnrollment enrollment = enrollment(
                100L,
                EMPLOYEE_ID,
                EnrollmentStatus.NOT_STARTED,
                TODAY.plusDays(5));

        LearningProgress first = progress(
                1000L,
                enrollment,
                module(200L, 1, true),
                LearningCompletionStatus.COMPLETED);

        LearningProgress second = progress(
                1001L,
                enrollment,
                module(201L, 2, false),
                LearningCompletionStatus.NOT_STARTED);

        when(courseEnrollmentRepository
                .findByCourseEnrollmentId(100L))
                .thenReturn(Optional.of(enrollment));

        when(learningProgressRepository
                .findAllByCourseEnrollment_CourseEnrollmentIdAndCourseModule_ActiveTrueOrderByCourseModule_ModuleOrderAsc(
                        100L))
                .thenReturn(List.of(first, second));

        MyCourseDetailResponse response =
                myCourseQueryService.getMyCourse(100L);

        assertThat(response.enrollmentId()).isEqualTo(100L);
        assertThat(response.courseName())
                .isEqualTo("New hire course");
        assertThat(response.status())
                .isEqualTo(EnrollmentStatus.NOT_STARTED);

        assertThat(response.modules())
                .extracting(module -> module.moduleOrder())
                .containsExactly(1, 2);

        assertThat(response.modules())
                .extracting(module -> module.required())
                .containsExactly(true, false);

        assertThat(response.modules().get(0).completionStatus())
                .isEqualTo(LearningCompletionStatus.COMPLETED);
    }

    @Test
    void rejectsAccessToAnotherEmployeesEnrollment() {
        stubCurrentUser();
        CourseEnrollment enrollment = enrollment(
                100L,
                999L,
                EnrollmentStatus.NOT_STARTED,
                TODAY.plusDays(5));

        when(courseEnrollmentRepository
                .findByCourseEnrollmentId(100L))
                .thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() ->
                myCourseQueryService.getMyCourse(100L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(ErrorCode.FORBIDDEN));

        verifyNoInteractions(learningProgressRepository);
    }

    @Test
    void throwsNotFoundWhenEnrollmentDoesNotExist() {
        stubCurrentUser();
        when(courseEnrollmentRepository
                .findByCourseEnrollmentId(404L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                myCourseQueryService.getMyCourse(404L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(
                                        ErrorCode.RESOURCE_NOT_FOUND));

        verifyNoInteractions(learningProgressRepository);
    }

    @Test
    void rejectsInvalidPageRequest() {
        assertThatThrownBy(() ->
                myCourseQueryService.getMyCourses(-1, 20))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_REQUEST));

        verifyNoInteractions(courseEnrollmentRepository);
    }

    private void stubCurrentUser() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(EMPLOYEE_ID));
    }

    private CurrentUserContext currentUser(Long employeeId) {
        return new CurrentUserContext(
                1L,
                employeeId,
                Set.of(RoleType.EMPLOYEE),
                null,
                null,
                null);
    }

    private CourseEnrollment enrollment(
            Long enrollmentId,
            Long employeeId,
            EnrollmentStatus status,
            LocalDate dueDate
    ) {
        Course course = newEntity(Course.class);
        set(course, "courseId", 50L);
        set(course, "courseName", "New hire course");
        set(course, "courseDescription", "Course description");
        set(course, "required", true);

        CourseEnrollment enrollment =
                newEntity(CourseEnrollment.class);

        set(enrollment, "courseEnrollmentId", enrollmentId);
        set(enrollment, "course", course);
        set(enrollment, "employeeId", employeeId);
        set(enrollment, "enrollmentRound", "1");
        set(enrollment, "enrollmentStatus", status);
        set(enrollment, "progressRate", new BigDecimal("25.00"));
        set(enrollment, "enrollmentStartDate",
                TODAY.minusDays(10));
        set(enrollment, "enrollmentDueDate", dueDate);

        return enrollment;
    }

    private CourseModule module(
            Long moduleId,
            int moduleOrder,
            boolean required
    ) {
        CourseModule module = newEntity(CourseModule.class);

        set(module, "courseModuleId", moduleId);
        set(module, "moduleTitle", "Module " + moduleOrder);
        set(module, "moduleContent",
                "Module content " + moduleOrder);
        set(module, "referenceUrl",
                "https://example.com/" + moduleOrder);
        set(module, "moduleOrder", moduleOrder);
        set(module, "required", required);
        set(module, "active", true);

        return module;
    }

    private LearningProgress progress(
            Long progressId,
            CourseEnrollment enrollment,
            CourseModule module,
            LearningCompletionStatus status
    ) {
        LearningProgress progress =
                newEntity(LearningProgress.class);

        set(progress, "learningProgressId", progressId);
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