package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.ManagerEducationPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.EmployeeSummary;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;

@ExtendWith(MockitoExtension.class)
class ManagerEducationQueryServiceTest {

    private static final Long MANAGER_EMPLOYEE_ID = 1L;

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-10T00:00:00Z"),
            ZoneId.of("Asia/Tokyo"));

    private static final LocalDate TODAY =
            LocalDate.now(FIXED_CLOCK);

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private OrganizationQueryService organizationQueryService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private ManagerEducationQueryService service;

    @BeforeEach
    void setUp() {
        service = new ManagerEducationQueryService(
                courseEnrollmentRepository,
                organizationQueryService,
                currentUserProvider,
                FIXED_CLOCK);
    }

    @Test
    void managerGetsOnlyManagedEmployeesEducationWithOneBatchLookup() {
        stubManager();

        when(organizationQueryService.findManagedEmployeeIds(
                MANAGER_EMPLOYEE_ID))
                .thenReturn(List.of(10L, 20L));

        CourseEnrollment first = enrollment(
                100L,
                10L,
                "Security basics",
                EnrollmentStatus.IN_PROGRESS,
                TODAY.plusDays(3));

        CourseEnrollment second = enrollment(
                101L,
                20L,
                "Company rules",
                EnrollmentStatus.IN_PROGRESS,
                TODAY.minusDays(1));

        when(courseEnrollmentRepository.findAllByEmployeeIdIn(
                eq(List.of(10L, 20L)),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(first, second),
                        PageRequest.of(0, 20),
                        2));

        when(organizationQueryService.findEmployeeSummaries(
                any()))
                .thenReturn(List.of(
                        employee(10L, "Employee A"),
                        employee(20L, "Employee B")));

        ManagerEducationPageResponse response =
                service.getTeamEducation(0, 20);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content())
                .extracting(item -> item.employeeId())
                .containsExactly(10L, 20L);

        assertThat(response.content().get(0).employeeName())
                .isEqualTo("Employee A");
        assertThat(response.content().get(0).courseName())
                .isEqualTo("Security basics");
        assertThat(response.content().get(0).overdue())
                .isFalse();

        assertThat(response.content().get(1).status())
                .isEqualTo(EnrollmentStatus.OVERDUE);
        assertThat(response.content().get(1).overdue())
                .isTrue();

        verify(organizationQueryService, times(1))
                .findEmployeeSummaries(argThat(
                        employeeIds ->
                                employeeIds.size() == 2
                                        && employeeIds.contains(10L)
                                        && employeeIds.contains(20L)));
    }

    @Test
    void returnsEmptyPageWhenManagerHasNoManagedEmployees() {
        stubManager();

        when(organizationQueryService.findManagedEmployeeIds(
                MANAGER_EMPLOYEE_ID))
                .thenReturn(List.of());

        ManagerEducationPageResponse response =
                service.getTeamEducation(0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isZero();

        verifyNoInteractions(courseEnrollmentRepository);
        verify(organizationQueryService, never())
                .findEmployeeSummaries(any());
    }

    @Test
    void regularEmployeeCannotGetTeamEducation() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(
                        10L,
                        Set.of(RoleType.EMPLOYEE)));

        assertThatThrownBy(() ->
                service.getTeamEducation(0, 20))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(ErrorCode.FORBIDDEN));

        verifyNoInteractions(courseEnrollmentRepository);
        verifyNoInteractions(organizationQueryService);
    }

    @Test
    void managerCannotGetCoursesOutsideManagedRelationship() {
        stubManager();

        when(organizationQueryService.isManagedEmployee(
                MANAGER_EMPLOYEE_ID,
                999L))
                .thenReturn(false);

        assertThatThrownBy(() ->
                service.getEmployeeCourses(999L, 0, 20))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(ErrorCode.FORBIDDEN));

        verifyNoInteractions(courseEnrollmentRepository);
        verify(organizationQueryService, never())
                .findEmployeeSummaries(any());
    }

    @Test
    void managerGetsManagedEmployeesCourses() {
        stubManager();

        when(organizationQueryService.isManagedEmployee(
                MANAGER_EMPLOYEE_ID,
                10L))
                .thenReturn(true);

        CourseEnrollment enrollment = enrollment(
                100L,
                10L,
                "Security basics",
                EnrollmentStatus.NOT_STARTED,
                TODAY.plusDays(5));

        when(courseEnrollmentRepository.findAllByEmployeeId(
                eq(10L),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(enrollment),
                        PageRequest.of(0, 20),
                        1));

        when(organizationQueryService.findEmployeeSummaries(
                List.of(10L)))
                .thenReturn(List.of(
                        employee(10L, "Employee A")));

        ManagerEducationPageResponse response =
                service.getEmployeeCourses(10L, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).employeeId())
                .isEqualTo(10L);
        assertThat(response.content().get(0).employeeName())
                .isEqualTo("Employee A");
        assertThat(response.content().get(0).courseName())
                .isEqualTo("Security basics");
        assertThat(response.content().get(0).status())
                .isEqualTo(EnrollmentStatus.NOT_STARTED);

        verify(organizationQueryService, times(1))
                .findEmployeeSummaries(List.of(10L));
    }

    @Test
    void rejectsInvalidPaginationBeforeAccessingDependencies() {
        assertThatThrownBy(() ->
                service.getTeamEducation(-1, 20))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(
                                        ErrorCode.INVALID_REQUEST));

        verifyNoInteractions(currentUserProvider);
        verifyNoInteractions(courseEnrollmentRepository);
        verifyNoInteractions(organizationQueryService);
    }

    private void stubManager() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(
                        MANAGER_EMPLOYEE_ID,
                        Set.of(
                                RoleType.EMPLOYEE,
                                RoleType.MANAGER)));
    }

    private CurrentUserContext currentUser(
            Long employeeId,
            Set<RoleType> roles
    ) {
        return new CurrentUserContext(
                1L,
                employeeId,
                roles,
                null,
                null,
                null);
    }

    private EmployeeSummary employee(
            Long employeeId,
            String employeeName
    ) {
        return new EmployeeSummary(
                employeeId,
                employeeName,
                100L,
                "Development",
                200L,
                "Junior");
    }

    private CourseEnrollment enrollment(
            Long enrollmentId,
            Long employeeId,
            String courseName,
            EnrollmentStatus status,
            LocalDate dueDate
    ) {
        Course course = Course.create(
                courseName,
                "Course description",
                true,
                TODAY.minusDays(10),
                TODAY.plusDays(30),
                1L);

        ReflectionTestUtils.setField(
                course,
                "courseId",
                enrollmentId + 1000);

        CourseEnrollment enrollment =
                CourseEnrollment.create(
                        course,
                        employeeId,
                        null,
                        "1",
                        TODAY.minusDays(5),
                        dueDate);

        ReflectionTestUtils.setField(
                enrollment,
                "courseEnrollmentId",
                enrollmentId);
        ReflectionTestUtils.setField(
                enrollment,
                "enrollmentStatus",
                status);
        ReflectionTestUtils.setField(
                enrollment,
                "progressRate",
                new BigDecimal("50.00"));

        return enrollment;
    }
}