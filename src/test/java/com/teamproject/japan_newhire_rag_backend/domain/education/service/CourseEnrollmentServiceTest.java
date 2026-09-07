package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseEnrollmentCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseEnrollmentCreateResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseModule;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.LearningProgress;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.AssignmentTargetType;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.CoursePublicationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseAssignmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseModuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.LearningProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@ExtendWith(MockitoExtension.class)
class CourseEnrollmentServiceTest {

    private static final Long COURSE_ID = 10L;
    private static final LocalDate START_DATE =
            LocalDate.of(2026, 9, 1);
    private static final LocalDate DUE_DATE =
            LocalDate.of(2026, 9, 30);

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseModuleRepository courseModuleRepository;

    @Mock
    private CourseAssignmentRepository courseAssignmentRepository;

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private LearningProgressRepository learningProgressRepository;

    @Mock
    private OrganizationQueryService organizationQueryService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private CourseEnrollmentService courseEnrollmentService;

    @BeforeEach
    void setUp() {
        courseEnrollmentService = new CourseEnrollmentService(
                courseRepository,
                courseModuleRepository,
                courseAssignmentRepository,
                courseEnrollmentRepository,
                learningProgressRepository,
                organizationQueryService,
                currentUserProvider);
    }

    @Test
    void jobGradeEmployeesAreAssigned() {
        Course course = publicCourse();
        CourseModule module = module(course, 1);
        stubAssignableCourse(course);

        when(organizationQueryService
                .findValidEmployeeIdsByJobGradeIds(List.of(900L)))
                .thenReturn(List.of(32L, 31L));

        when(courseEnrollmentRepository
                .findAllByCourse_CourseIdAndEmployeeIdInAndEnrollmentRound(
                        COURSE_ID,
                        List.of(31L, 32L),
                        "1"))
                .thenReturn(List.of());

        when(courseModuleRepository
                .findAllByCourse_CourseIdAndActiveTrueOrderByModuleOrderAsc(
                        COURSE_ID))
                .thenReturn(List.of(module));

        CourseEnrollmentCreateResponse response =
                courseEnrollmentService.createEnrollments(
                        COURSE_ID,
                        new CourseEnrollmentCreateRequest(
                                AssignmentTargetType.JOB_GRADE,
                                null,
                                null,
                                900L,
                                "1",
                                START_DATE,
                                DUE_DATE));

        assertThat(response.assignedCount()).isEqualTo(2);
        assertThat(response.duplicateCount()).isZero();

        verify(organizationQueryService)
                .findValidEmployeeIdsByJobGradeIds(List.of(900L));
        verify(courseAssignmentRepository)
                .save(org.mockito.ArgumentMatchers.any());
        verify(courseEnrollmentRepository)
                .saveAll(org.mockito.ArgumentMatchers.any());
     }

     @Test
     void newHireEmployeesAreAssignedWithoutTargetId() {
        Course course = publicCourse();
        CourseModule module = module(course, 1);
        stubAssignableCourse(course);

        when(organizationQueryService.findValidNewHireEmployeeIds())
                .thenReturn(List.of(42L, 41L));

        when(courseEnrollmentRepository
                .findAllByCourse_CourseIdAndEmployeeIdInAndEnrollmentRound(
                        COURSE_ID,
                        List.of(41L, 42L),
                        "1"))
                .thenReturn(List.of());

        when(courseModuleRepository
                .findAllByCourse_CourseIdAndActiveTrueOrderByModuleOrderAsc(
                        COURSE_ID))
                .thenReturn(List.of(module));

        CourseEnrollmentCreateResponse response =
                courseEnrollmentService.createEnrollments(
                        COURSE_ID,
                        new CourseEnrollmentCreateRequest(
                                AssignmentTargetType.NEW_HIRE,
                                null,
                                null,
                                null,
                                "1",
                                START_DATE,
                                DUE_DATE));

        assertThat(response.assignedCount()).isEqualTo(2);
        assertThat(response.duplicateCount()).isZero();

        verify(organizationQueryService)
                .findValidNewHireEmployeeIds();
     }

     @Test
     void courseWithoutActiveRequiredModuleCannotBeAssigned() {
        Course course = publicCourse();

        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(RoleType.HR_MANAGER));

        when(courseRepository
                .findByCourseIdAndDeletedAtIsNull(COURSE_ID))
                .thenReturn(Optional.of(course));

        when(courseModuleRepository
                .existsByCourse_CourseIdAndRequiredTrueAndActiveTrue(
                        COURSE_ID))
                .thenReturn(false);

        assertThatThrownBy(() ->
                courseEnrollmentService.createEnrollments(
                        COURSE_ID,
                        employeeRequest(20L)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CONFLICT));

        verifyNoInteractions(organizationQueryService);
        verify(courseAssignmentRepository, never())
                .save(org.mockito.ArgumentMatchers.any());
     }

     @Test
     void invalidEmployeeIsExcludedFromAssignment() {
        Course course = publicCourse();
        stubAssignableCourse(course);

        when(organizationQueryService.isValidEmployee(20L))
                .thenReturn(false);

        CourseEnrollmentCreateResponse response =
                courseEnrollmentService.createEnrollments(
                        COURSE_ID,
                        employeeRequest(20L));

        assertThat(response.assignedCount()).isZero();
        assertThat(response.duplicateCount()).isZero();
        assertThat(response.duplicateEmployeeIds()).isEmpty();

        verify(courseEnrollmentRepository, never())
                .findAllByCourse_CourseIdAndEmployeeIdInAndEnrollmentRound(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());

        verify(courseAssignmentRepository, never())
                .save(org.mockito.ArgumentMatchers.any());

        verify(learningProgressRepository, never())
                .saveAll(org.mockito.ArgumentMatchers.any());
     }

    @Test
    @SuppressWarnings("unchecked")
    void departmentAssignmentExcludesDuplicatesAndCreatesProgresses() {
        Course course = publicCourse();
        CourseModule firstModule = module(course, 1);
        CourseModule secondModule = module(course, 2);

        stubAssignableCourse(course);

        when(organizationQueryService
                .findValidEmployeeIdsByDepartmentIds(List.of(100L)))
                .thenReturn(List.of(5L, 1L, 4L, 2L, 3L));

        when(courseEnrollmentRepository
                .findAllByCourse_CourseIdAndEmployeeIdInAndEnrollmentRound(
                        COURSE_ID,
                        List.of(1L, 2L, 3L, 4L, 5L),
                        "1"))
                .thenReturn(List.of(
                        existingEnrollment(course, 1L),
                        existingEnrollment(course, 2L)));

        when(courseModuleRepository
                .findAllByCourse_CourseIdAndActiveTrueOrderByModuleOrderAsc(
                        COURSE_ID))
                .thenReturn(List.of(firstModule, secondModule));

        CourseEnrollmentCreateResponse response =
                courseEnrollmentService.createEnrollments(
                        COURSE_ID,
                        departmentRequest());

        assertThat(response.assignedCount()).isEqualTo(3);
        assertThat(response.duplicateCount()).isEqualTo(2);
        assertThat(response.duplicateEmployeeIds())
                .containsExactly(1L, 2L);

        verify(courseAssignmentRepository)
                .save(org.mockito.ArgumentMatchers.any(
                        CourseAssignment.class));

        ArgumentCaptor<Iterable<CourseEnrollment>> enrollmentCaptor =
                ArgumentCaptor.forClass(Iterable.class);

        verify(courseEnrollmentRepository)
                .saveAll(enrollmentCaptor.capture());

        List<CourseEnrollment> savedEnrollments =
                toList(enrollmentCaptor.getValue());

        assertThat(savedEnrollments)
                .extracting(CourseEnrollment::getEmployeeId)
                .containsExactly(3L, 4L, 5L);

        assertThat(savedEnrollments)
                .allSatisfy(enrollment -> {
                    assertThat(enrollment.getEnrollmentStatus().name())
                            .isEqualTo("NOT_STARTED");
                    assertThat(enrollment.getProgressRate())
                            .isEqualByComparingTo("0");
                });

        ArgumentCaptor<Iterable<LearningProgress>> progressCaptor =
                ArgumentCaptor.forClass(Iterable.class);

        verify(learningProgressRepository)
                .saveAll(progressCaptor.capture());

        List<LearningProgress> savedProgresses =
                toList(progressCaptor.getValue());

        assertThat(savedProgresses).hasSize(6);
        assertThat(savedProgresses)
                .allSatisfy(progress ->
                        assertThat(progress.getCompletionStatus().name())
                                .isEqualTo("NOT_STARTED"));
    }

    @Test
    void allDuplicateEmployeesCreateNoNewData() {
        Course course = publicCourse();
        stubAssignableCourse(course);

        when(organizationQueryService
                .findValidEmployeeIdsByDepartmentIds(List.of(100L)))
                .thenReturn(List.of(1L, 2L));

        when(courseEnrollmentRepository
                .findAllByCourse_CourseIdAndEmployeeIdInAndEnrollmentRound(
                        COURSE_ID,
                        List.of(1L, 2L),
                        "1"))
                .thenReturn(List.of(
                        existingEnrollment(course, 1L),
                        existingEnrollment(course, 2L)));

        CourseEnrollmentCreateResponse response =
                courseEnrollmentService.createEnrollments(
                        COURSE_ID,
                        departmentRequest());

        assertThat(response.assignedCount()).isZero();
        assertThat(response.duplicateCount()).isEqualTo(2);
        assertThat(response.duplicateEmployeeIds())
                .containsExactly(1L, 2L);

        verify(courseAssignmentRepository, never())
                .save(org.mockito.ArgumentMatchers.any());

        verify(courseEnrollmentRepository, never())
                .saveAll(org.mockito.ArgumentMatchers.any());

        verify(learningProgressRepository, never())
                .saveAll(org.mockito.ArgumentMatchers.any());

        verify(courseModuleRepository, never())
                .findAllByCourse_CourseIdAndActiveTrueOrderByModuleOrderAsc(
                        COURSE_ID);
    }

    @Test
    void validEmployeeAssignmentCreatesOneEnrollment() {
        Course course = publicCourse();
        CourseModule module = module(course, 1);
        stubAssignableCourse(course);

        when(organizationQueryService.isValidEmployee(20L))
                .thenReturn(true);

        when(courseEnrollmentRepository
                .findAllByCourse_CourseIdAndEmployeeIdInAndEnrollmentRound(
                        COURSE_ID,
                        List.of(20L),
                        "1"))
                .thenReturn(List.of());

        when(courseModuleRepository
                .findAllByCourse_CourseIdAndActiveTrueOrderByModuleOrderAsc(
                        COURSE_ID))
                .thenReturn(List.of(module));

        CourseEnrollmentCreateResponse response =
                courseEnrollmentService.createEnrollments(
                        COURSE_ID,
                        employeeRequest(20L));

        assertThat(response.assignedCount()).isEqualTo(1);
        assertThat(response.duplicateCount()).isZero();
        assertThat(response.duplicateEmployeeIds()).isEmpty();

        verify(courseAssignmentRepository)
                .save(org.mockito.ArgumentMatchers.any());
        verify(courseEnrollmentRepository)
                .saveAll(org.mockito.ArgumentMatchers.any());
        verify(learningProgressRepository)
                .saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void invalidTargetTypeAndIdCombinationIsRejected() {
        Course course = publicCourse();
        stubAssignableCourse(course);

        CourseEnrollmentCreateRequest request =
                new CourseEnrollmentCreateRequest(
                        AssignmentTargetType.EMPLOYEE,
                        null,
                        100L,
                        null,
                        "1",
                        START_DATE,
                        DUE_DATE);

        assertThatThrownBy(() ->
                courseEnrollmentService.createEnrollments(
                        COURSE_ID,
                        request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_REQUEST));

        verifyNoInteractions(organizationQueryService);
        verify(courseAssignmentRepository, never())
                .save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void draftCourseCannotBeAssigned() {
        Course course = draftCourse();

        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository
                .findByCourseIdAndDeletedAtIsNull(COURSE_ID))
                .thenReturn(Optional.of(course));

        assertThatThrownBy(() ->
                courseEnrollmentService.createEnrollments(
                        COURSE_ID,
                        employeeRequest(20L)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CONFLICT));

        verifyNoInteractions(organizationQueryService);
        verify(courseAssignmentRepository, never())
                .save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void missingOrDeletedCourseCannotBeAssigned() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository
                .findByCourseIdAndDeletedAtIsNull(COURSE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                courseEnrollmentService.createEnrollments(
                        COURSE_ID,
                        employeeRequest(20L)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        verifyNoInteractions(organizationQueryService);
    }

    @Test
    void dueDateBeforeStartDateIsRejected() {
        Course course = publicCourse();
        stubAssignableCourse(course);

        CourseEnrollmentCreateRequest request =
                new CourseEnrollmentCreateRequest(
                        AssignmentTargetType.EMPLOYEE,
                        20L,
                        null,
                        null,
                        "1",
                        LocalDate.of(2026, 9, 30),
                        LocalDate.of(2026, 9, 1));

        assertThatThrownBy(() ->
                courseEnrollmentService.createEnrollments(
                        COURSE_ID,
                        request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_REQUEST));

        verifyNoInteractions(organizationQueryService);
    }

    @Test
    void employeeCannotAssignCourse() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(RoleType.EMPLOYEE));

        assertThatThrownBy(() ->
                courseEnrollmentService.createEnrollments(
                        COURSE_ID,
                        employeeRequest(20L)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.FORBIDDEN));

        verifyNoInteractions(
                courseRepository,
                organizationQueryService,
                courseAssignmentRepository);
    }

    private void stubAssignableCourse(Course course) {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(RoleType.HR_MANAGER));

        when(courseRepository
                .findByCourseIdAndDeletedAtIsNull(COURSE_ID))
                .thenReturn(Optional.of(course));

        when(courseModuleRepository
                .existsByCourse_CourseIdAndRequiredTrueAndActiveTrue(
                        COURSE_ID))
                .thenReturn(true);
    }

    private Course publicCourse() {
        Course course = draftCourse();
        course.changePublicationStatus(
                CoursePublicationStatus.PUBLIC);
        return course;
    }

    private Course draftCourse() {
        return Course.create(
                "New hire fundamentals",
                "Company onboarding basics",
                true,
                START_DATE,
                DUE_DATE,
                7L);
    }

    private CourseModule module(Course course, int order) {
        return CourseModule.create(
                course,
                "Module " + order,
                "Module content " + order,
                null,
                order,
                true);
    }

    private CourseEnrollment existingEnrollment(
            Course course,
            Long employeeId
    ) {
        CourseAssignment assignment = CourseAssignment.create(
                course,
                AssignmentTargetType.DEPARTMENT,
                null,
                100L,
                null,
                "1",
                START_DATE,
                DUE_DATE,
                7L);

        return CourseEnrollment.create(
                course,
                employeeId,
                assignment,
                "1",
                START_DATE,
                DUE_DATE);
    }

    private CourseEnrollmentCreateRequest departmentRequest() {
        return new CourseEnrollmentCreateRequest(
                AssignmentTargetType.DEPARTMENT,
                null,
                100L,
                null,
                "1",
                START_DATE,
                DUE_DATE);
    }

    private CourseEnrollmentCreateRequest employeeRequest(
            Long employeeId
    ) {
        return new CourseEnrollmentCreateRequest(
                AssignmentTargetType.EMPLOYEE,
                employeeId,
                null,
                null,
                "1",
                START_DATE,
                DUE_DATE);
    }

    private CurrentUserContext currentUser(RoleType role) {
        return new CurrentUserContext(
                7L,
                70L,
                Set.of(role),
                700L,
                1,
                EmployeeType.GENERAL);
    }

    private <T> List<T> toList(Iterable<T> values) {
        return StreamSupport.stream(
                        values.spliterator(),
                        false)
                .toList();
    }
}