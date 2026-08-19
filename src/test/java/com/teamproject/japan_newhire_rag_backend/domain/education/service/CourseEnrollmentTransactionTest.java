package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseEnrollmentCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseModule;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.AssignmentTargetType;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.CoursePublicationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseModuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.LearningProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@SpringBootTest
@Import(CourseEnrollmentTransactionTest.FailureTestConfig.class)
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class CourseEnrollmentTransactionTest {

    @Autowired
    private CourseEnrollmentService courseEnrollmentService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseModuleRepository courseModuleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private OrganizationQueryService organizationQueryService;

    @Autowired
    private LearningProgressRepository learningProgressRepository;

    private Long appUserId;
    private Long employeeId;
    private Long departmentId;
    private Long jobGradeId;
    private Long courseId;

    @BeforeEach
    void setUp() {
        reset(
                currentUserProvider,
                organizationQueryService,
                learningProgressRepository);

        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        createOrganizationFixture(suffix);
        createCourseFixture();

        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(
                        appUserId,
                        employeeId,
                        Set.of(RoleType.HR_MANAGER),
                        departmentId,
                        100,
                        EmployeeType.GENERAL));

        when(organizationQueryService.isValidEmployee(employeeId))
                .thenReturn(true);

        when(learningProgressRepository.saveAll(any()))
                .thenThrow(new IllegalStateException(
                        "Forced learning progress save failure"));
    }

    @AfterEach
    void cleanUp() {
        if (courseId != null) {
            jdbcTemplate.update("""
                    delete lp
                    from learning_progress lp
                    join course_enrollment ce
                      on ce.course_enrollment_id = lp.course_enrollment_id
                    where ce.course_id = ?
                    """, courseId);

            jdbcTemplate.update(
                    "delete from course_enrollment where course_id = ?",
                    courseId);

            jdbcTemplate.update(
                    "delete from course_assignment where course_id = ?",
                    courseId);

            jdbcTemplate.update(
                    "delete from course_module where course_id = ?",
                    courseId);

            jdbcTemplate.update(
                    "delete from course where course_id = ?",
                    courseId);
        }

        if (employeeId != null) {
            jdbcTemplate.update(
                    "delete from employee where employee_id = ?",
                    employeeId);
        }

        if (appUserId != null) {
            jdbcTemplate.update(
                    "delete from app_user where app_user_id = ?",
                    appUserId);
        }

        if (departmentId != null) {
            jdbcTemplate.update(
                    "delete from department where department_id = ?",
                    departmentId);
        }

        if (jobGradeId != null) {
            jdbcTemplate.update(
                    "delete from job_grade where job_grade_id = ?",
                    jobGradeId);
        }
    }

    @Test
    void rollsBackAssignmentAndEnrollmentsWhenProgressSaveFails() {
        CourseEnrollmentCreateRequest request =
                new CourseEnrollmentCreateRequest(
                        AssignmentTargetType.EMPLOYEE,
                        employeeId,
                        null,
                        null,
                        "rollback-test",
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30));

        assertThat(countAssignments()).isZero();
        assertThat(countEnrollments()).isZero();
        assertThat(countLearningProgresses()).isZero();

        assertThatThrownBy(() ->
                courseEnrollmentService.createEnrollments(
                        courseId,
                        request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Forced learning progress save failure");

        assertThat(countAssignments()).isZero();
        assertThat(countEnrollments()).isZero();
        assertThat(countLearningProgresses()).isZero();
    }

    private void createOrganizationFixture(String suffix) {
        jdbcTemplate.update("""
                insert into department
                    (department_code, department_name,
                     department_status, display_order)
                values (?, ?, 'ACTIVE', 0)
                """,
                "C-ROLLBACK-D-" + suffix,
                "Course rollback department");

        departmentId = jdbcTemplate.queryForObject(
                """
                select department_id
                from department
                where department_code = ?
                """,
                Long.class,
                "C-ROLLBACK-D-" + suffix);

        jdbcTemplate.update("""
                insert into job_grade
                    (grade_code, grade_name, grade_level, is_active)
                values (?, ?, ?, true)
                """,
                "C-ROLLBACK-G-" + suffix,
                "Course rollback grade",
                200_000 + Math.abs(suffix.hashCode() % 700_000));

        jobGradeId = jdbcTemplate.queryForObject(
                """
                select job_grade_id
                from job_grade
                where grade_code = ?
                """,
                Long.class,
                "C-ROLLBACK-G-" + suffix);

        jdbcTemplate.update("""
                insert into app_user
                    (email, password_hash,
                     account_status, failed_login_count)
                values (?, ?, 'ACTIVE', 0)
                """,
                "course-rollback-" + suffix + "@example.com",
                "test-password-hash");

        appUserId = jdbcTemplate.queryForObject(
                """
                select app_user_id
                from app_user
                where email = ?
                """,
                Long.class,
                "course-rollback-" + suffix + "@example.com");

        jdbcTemplate.update("""
                insert into employee
                    (app_user_id, department_id, job_grade_id,
                     employee_number, employee_name, employee_type,
                     hire_date, employment_status)
                values (?, ?, ?, ?, ?, 'GENERAL',
                        current_date, 'EMPLOYED')
                """,
                appUserId,
                departmentId,
                jobGradeId,
                "C-ROLLBACK-E-" + suffix,
                "Course rollback employee");

        employeeId = jdbcTemplate.queryForObject(
                """
                select employee_id
                from employee
                where employee_number = ?
                """,
                Long.class,
                "C-ROLLBACK-E-" + suffix);
    }

    private void createCourseFixture() {
        Course course = newEntity(Course.class);
        set(course, "courseName", "Transaction rollback course");
        set(course, "courseDescription", "Rollback integration test");
        set(course, "required", true);
        set(course, "trainingStartDate", LocalDate.of(2026, 9, 1));
        set(course, "trainingEndDate", LocalDate.of(2026, 9, 30));
        set(course, "publicationStatus", CoursePublicationStatus.PUBLIC);
        set(course, "createdBy", appUserId);

        Course savedCourse = courseRepository.saveAndFlush(course);
        courseId = savedCourse.getCourseId();

        CourseModule module = CourseModule.create(
                savedCourse,
                "Required rollback module",
                "Rollback test module content",
                null,
                1,
                true);

        courseModuleRepository.saveAndFlush(module);
    }

    private int countAssignments() {
        return jdbcTemplate.queryForObject(
                """
                select count(*)
                from course_assignment
                where course_id = ?
                """,
                Integer.class,
                courseId);
    }

    private int countEnrollments() {
        return jdbcTemplate.queryForObject(
                """
                select count(*)
                from course_enrollment
                where course_id = ?
                """,
                Integer.class,
                courseId);
    }

    private int countLearningProgresses() {
        return jdbcTemplate.queryForObject(
                """
                select count(*)
                from learning_progress lp
                join course_enrollment ce
                  on ce.course_enrollment_id =
                     lp.course_enrollment_id
                where ce.course_id = ?
                """,
                Integer.class,
                courseId);
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

    @TestConfiguration
    static class FailureTestConfig {

        @Bean
        @Primary
        CurrentUserProvider rollbackCurrentUserProvider() {
            return mock(CurrentUserProvider.class);
        }

        @Bean
        @Primary
        OrganizationQueryService rollbackOrganizationQueryService() {
            return mock(OrganizationQueryService.class);
        }

        @Bean
        @Primary
        LearningProgressRepository rollbackLearningProgressRepository() {
            return mock(LearningProgressRepository.class);
        }
    }
}