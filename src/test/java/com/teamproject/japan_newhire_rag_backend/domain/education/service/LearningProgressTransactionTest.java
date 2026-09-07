package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseModuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@SpringBootTest
@Import(LearningProgressTransactionTest.FailureTestConfig.class)
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class LearningProgressTransactionTest {

    @Autowired
    private LearningProgressService learningProgressService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private CourseModuleRepository courseModuleRepository;

    private Long appUserId;
    private Long employeeId;
    private Long departmentId;
    private Long jobGradeId;
    private Long courseId;
    private Long courseModuleId;
    private Long courseAssignmentId;
    private Long courseEnrollmentId;
    private Long learningProgressId;

    @BeforeEach
    void setUp() {
        reset(currentUserProvider, courseModuleRepository);

        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        createOrganizationFixture(suffix);
        createLearningProgressFixture(suffix);

        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(
                        appUserId,
                        employeeId,
                        Set.of(RoleType.EMPLOYEE),
                        departmentId,
                        100,
                        EmployeeType.GENERAL));

        when(courseModuleRepository
                .countByCourse_CourseIdAndRequiredTrueAndActiveTrue(
                        courseId))
                .thenThrow(new IllegalStateException(
                        "Forced required module count failure"));
    }

    @AfterEach
    void cleanUp() {
        if (learningProgressId != null) {
            jdbcTemplate.update(
                    """
                    delete from learning_progress
                    where learning_progress_id = ?
                    """,
                    learningProgressId);
        }

        if (courseEnrollmentId != null) {
            jdbcTemplate.update(
                    """
                    delete from course_enrollment
                    where course_enrollment_id = ?
                    """,
                    courseEnrollmentId);
        }

        if (courseAssignmentId != null) {
            jdbcTemplate.update(
                    """
                    delete from course_assignment
                    where course_assignment_id = ?
                    """,
                    courseAssignmentId);
        }

        if (courseModuleId != null) {
            jdbcTemplate.update(
                    """
                    delete from course_module
                    where course_module_id = ?
                    """,
                    courseModuleId);
        }

        if (courseId != null) {
            jdbcTemplate.update(
                    """
                    delete from course
                    where course_id = ?
                    """,
                    courseId);
        }

        if (employeeId != null) {
            jdbcTemplate.update(
                    """
                    delete from employee
                    where employee_id = ?
                    """,
                    employeeId);
        }

        if (appUserId != null) {
            jdbcTemplate.update(
                    """
                    delete from app_user
                    where app_user_id = ?
                    """,
                    appUserId);
        }

        if (departmentId != null) {
            jdbcTemplate.update(
                    """
                    delete from department
                    where department_id = ?
                    """,
                    departmentId);
        }

        if (jobGradeId != null) {
            jdbcTemplate.update(
                    """
                    delete from job_grade
                    where job_grade_id = ?
                    """,
                    jobGradeId);
        }
    }

    @Test
    void rollsBackProgressAndEnrollmentWhenModuleCountFails() {
        assertInitialDatabaseState();

        assertThatThrownBy(() ->
                learningProgressService.completeProgress(
                        learningProgressId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Forced required module count failure");

        assertInitialDatabaseState();
    }

    private void assertInitialDatabaseState() {
        String completionStatus = jdbcTemplate.queryForObject(
                """
                select completion_status
                from learning_progress
                where learning_progress_id = ?
                """,
                String.class,
                learningProgressId);

        LocalDateTime progressStartedAt =
                jdbcTemplate.queryForObject(
                        """
                        select started_at
                        from learning_progress
                        where learning_progress_id = ?
                        """,
                        LocalDateTime.class,
                        learningProgressId);

        LocalDateTime progressCompletedAt =
                jdbcTemplate.queryForObject(
                        """
                        select completed_at
                        from learning_progress
                        where learning_progress_id = ?
                        """,
                        LocalDateTime.class,
                        learningProgressId);

        String enrollmentStatus = jdbcTemplate.queryForObject(
                """
                select enrollment_status
                from course_enrollment
                where course_enrollment_id = ?
                """,
                String.class,
                courseEnrollmentId);

        BigDecimal progressRate = jdbcTemplate.queryForObject(
                """
                select progress_rate
                from course_enrollment
                where course_enrollment_id = ?
                """,
                BigDecimal.class,
                courseEnrollmentId);

        LocalDateTime enrollmentCompletedAt =
                jdbcTemplate.queryForObject(
                        """
                        select completed_at
                        from course_enrollment
                        where course_enrollment_id = ?
                        """,
                        LocalDateTime.class,
                        courseEnrollmentId);

        assertThat(completionStatus)
                .isEqualTo("NOT_STARTED");
        assertThat(progressStartedAt).isNull();
        assertThat(progressCompletedAt).isNull();

        assertThat(enrollmentStatus)
                .isEqualTo("NOT_STARTED");
        assertThat(progressRate)
                .isEqualByComparingTo("0.00");
        assertThat(enrollmentCompletedAt).isNull();
    }

    private void createOrganizationFixture(String suffix) {
        String departmentCode = "C-PROGRESS-D-" + suffix;
        String gradeCode = "C-PROGRESS-G-" + suffix;
        String email =
                "progress-rollback-" + suffix + "@example.com";
        String employeeNumber = "C-PROGRESS-E-" + suffix;

        jdbcTemplate.update(
                """
                insert into department
                    (department_code, department_name,
                     department_status, display_order)
                values (?, ?, 'ACTIVE', 0)
                """,
                departmentCode,
                "Progress rollback department");

        departmentId = jdbcTemplate.queryForObject(
                """
                select department_id
                from department
                where department_code = ?
                """,
                Long.class,
                departmentCode);

        jdbcTemplate.update(
                """
                insert into job_grade
                    (grade_code, grade_name,
                     grade_level, is_active)
                values (?, ?, ?, true)
                """,
                gradeCode,
                "Progress rollback grade",
                200_000 + Math.abs(
                        suffix.hashCode() % 700_000));

        jobGradeId = jdbcTemplate.queryForObject(
                """
                select job_grade_id
                from job_grade
                where grade_code = ?
                """,
                Long.class,
                gradeCode);

        jdbcTemplate.update(
                """
                insert into app_user
                    (email, password_hash,
                     account_status, failed_login_count)
                values (?, ?, 'ACTIVE', 0)
                """,
                email,
                "test-password-hash");

        appUserId = jdbcTemplate.queryForObject(
                """
                select app_user_id
                from app_user
                where email = ?
                """,
                Long.class,
                email);

        jdbcTemplate.update(
                """
                insert into employee
                    (app_user_id, department_id, job_grade_id,
                     employee_number, employee_name,
                     employee_type, hire_date,
                     employment_status)
                values (?, ?, ?, ?, ?, 'GENERAL',
                        current_date, 'EMPLOYED')
                """,
                appUserId,
                departmentId,
                jobGradeId,
                employeeNumber,
                "Progress rollback employee");

        employeeId = jdbcTemplate.queryForObject(
                """
                select employee_id
                from employee
                where employee_number = ?
                """,
                Long.class,
                employeeNumber);
    }

    private void createLearningProgressFixture(String suffix) {
        String courseName =
                "Progress rollback course " + suffix;

        jdbcTemplate.update(
                """
                insert into course
                    (course_name, course_description,
                     is_required, training_start_date,
                     training_end_date, publication_status,
                     created_by)
                values (?, ?, true,
                        '2026-09-01', '2026-09-30',
                        'PUBLIC', ?)
                """,
                courseName,
                "Learning progress rollback test",
                appUserId);

        courseId = jdbcTemplate.queryForObject(
                """
                select course_id
                from course
                where course_name = ?
                  and created_by = ?
                """,
                Long.class,
                courseName,
                appUserId);

        jdbcTemplate.update(
                """
                insert into course_module
                    (course_id, module_title,
                     module_content, module_order,
                     is_required, is_active)
                values (?, ?, ?, 1, true, true)
                """,
                courseId,
                "Required rollback module",
                "Rollback module content");

        courseModuleId = jdbcTemplate.queryForObject(
                """
                select course_module_id
                from course_module
                where course_id = ?
                  and module_order = 1
                """,
                Long.class,
                courseId);

        jdbcTemplate.update(
                """
                insert into course_assignment
                    (course_id, target_type,
                     employee_id, is_new_employee_target,
                     enrollment_round,
                     enrollment_start_date,
                     enrollment_due_date, assigned_by)
                values (?, 'EMPLOYEE', ?, false,
                        'rollback-test',
                        '2026-09-01', '2026-09-30', ?)
                """,
                courseId,
                employeeId,
                appUserId);

        courseAssignmentId = jdbcTemplate.queryForObject(
                """
                select course_assignment_id
                from course_assignment
                where course_id = ?
                  and employee_id = ?
                  and enrollment_round = 'rollback-test'
                """,
                Long.class,
                courseId,
                employeeId);

        jdbcTemplate.update(
                """
                insert into course_enrollment
                    (course_id, employee_id,
                     course_assignment_id, enrollment_round,
                     enrollment_status, progress_rate,
                     enrollment_start_date,
                     enrollment_due_date)
                values (?, ?, ?, 'rollback-test',
                        'NOT_STARTED', 0.00,
                        '2026-09-01', '2026-09-30')
                """,
                courseId,
                employeeId,
                courseAssignmentId);

        courseEnrollmentId = jdbcTemplate.queryForObject(
                """
                select course_enrollment_id
                from course_enrollment
                where course_id = ?
                  and employee_id = ?
                  and enrollment_round = 'rollback-test'
                """,
                Long.class,
                courseId,
                employeeId);

        jdbcTemplate.update(
                """
                insert into learning_progress
                    (course_enrollment_id, course_module_id,
                     completion_status)
                values (?, ?, 'NOT_STARTED')
                """,
                courseEnrollmentId,
                courseModuleId);

        learningProgressId = jdbcTemplate.queryForObject(
                """
                select learning_progress_id
                from learning_progress
                where course_enrollment_id = ?
                  and course_module_id = ?
                """,
                Long.class,
                courseEnrollmentId,
                courseModuleId);
    }

    @TestConfiguration
    static class FailureTestConfig {

        @Bean
        @Primary
        CurrentUserProvider progressCurrentUserProvider() {
            return mock(CurrentUserProvider.class);
        }

        @Bean
        @Primary
        CourseModuleRepository progressCourseModuleRepository() {
            return mock(CourseModuleRepository.class);
        }
    }
}