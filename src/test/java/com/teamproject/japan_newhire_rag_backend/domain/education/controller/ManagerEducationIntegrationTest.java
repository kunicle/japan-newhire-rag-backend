package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManagerEducationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessTokenService accessTokenService;

    private final List<Long> createdRoleIds = new ArrayList<>();
    private final List<TestEmployee> employees = new ArrayList<>();

    private Long employeeRoleId;
    private Long managerRoleId;
    private Long departmentId;
    private Long jobGradeId;
    private Long courseId;

    private TestEmployee manager;
    private TestEmployee managedOne;
    private TestEmployee managedTwo;
    private TestEmployee regularEmployee;
    private TestEmployee emptyManager;

    private String suffix;

    @BeforeAll
    void prepareFixture() {
        suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        employeeRoleId = ensureRole(
                "EMPLOYEE",
                "Employee");
        managerRoleId = ensureRole(
                "MANAGER",
                "Manager");

        createOrganizationFixture();

        manager = createEmployee("manager", "Integration Manager");
        managedOne = createEmployee("managed-one", "Managed Employee One");
        managedTwo = createEmployee("managed-two", "Managed Employee Two");
        regularEmployee = createEmployee("regular", "Regular Employee");
        emptyManager = createEmployee("empty-manager", "Empty Manager");

        grantRole(manager, managerRoleId);
        grantRole(managedOne, employeeRoleId);
        grantRole(managedTwo, employeeRoleId);
        grantRole(regularEmployee, employeeRoleId);
        grantRole(emptyManager, managerRoleId);

        createManagerRelation(manager, managedOne);
        createManagerRelation(manager, managedTwo);

        createEducationFixture();
    }

    @AfterAll
    void cleanFixture() {
        if (courseId != null) {
            jdbcTemplate.update(
                    "delete from learning_progress "
                            + "where course_enrollment_id in "
                            + "(select course_enrollment_id "
                            + "from course_enrollment where course_id = ?)",
                    courseId);

            jdbcTemplate.update(
                    "delete from course_enrollment where course_id = ?",
                    courseId);

            jdbcTemplate.update(
                    "delete from course_assignment where course_id = ?",
                    courseId);

            jdbcTemplate.update(
                    "delete from course where course_id = ?",
                    courseId);
        }

        if (manager != null) {
            jdbcTemplate.update(
                    "delete from manager_relation where created_by = ?",
                    manager.appUserId());
        }

        for (TestEmployee employee : employees) {
            jdbcTemplate.update(
                    "delete from refresh_token where app_user_id = ?",
                    employee.appUserId());

            jdbcTemplate.update(
                    "delete from login_attempt where app_user_id = ?",
                    employee.appUserId());

            jdbcTemplate.update(
                    "delete from user_role where app_user_id = ?",
                    employee.appUserId());
        }

        for (int index = employees.size() - 1; index >= 0; index--) {
            TestEmployee employee = employees.get(index);

            jdbcTemplate.update(
                    "delete from employee where employee_id = ?",
                    employee.employeeId());

            jdbcTemplate.update(
                    "delete from app_user where app_user_id = ?",
                    employee.appUserId());
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

        for (Long roleId : createdRoleIds) {
            jdbcTemplate.update(
                    "delete from role where role_id = ?",
                    roleId);
        }
    }

    @Test
    void managerSeesOnlyManagedEmployeesThroughRealIntegration()
            throws Exception {
        mockMvc.perform(get("/api/manager/team-education")
                        .header(
                                "Authorization",
                                bearerToken(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content[*].employeeId").value(
                        containsInAnyOrder(
                                managedOne.employeeId().intValue(),
                                managedTwo.employeeId().intValue())))
                .andExpect(jsonPath("$.content[*].employeeName").value(
                        containsInAnyOrder(
                                managedOne.employeeName(),
                                managedTwo.employeeName())))
                .andExpect(jsonPath("$.content[*].overdue").value(
                        containsInAnyOrder(true, false)))
                .andExpect(jsonPath("$.content[*].status").value(
                        containsInAnyOrder(
                                "OVERDUE",
                                "COMPLETED")));
    }

    @Test
    void managerCannotReadEmployeeOutsideRealRelationship()
            throws Exception {
        mockMvc.perform(get(
                        "/api/manager/employees/{employeeId}/courses",
                        regularEmployee.employeeId())
                        .header(
                                "Authorization",
                                bearerToken(manager)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void regularEmployeeCannotUseManagerEducationApi()
            throws Exception {
        mockMvc.perform(get("/api/manager/team-education")
                        .header(
                                "Authorization",
                                bearerToken(regularEmployee)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void managerWithoutRelationshipsGetsEmptyPage()
            throws Exception {
        mockMvc.perform(get("/api/manager/team-education")
                        .header(
                                "Authorization",
                                bearerToken(emptyManager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    private void createOrganizationFixture() {
        String departmentCode = "MEIT-D-" + suffix;
        String gradeCode = "MEIT-G-" + suffix;

        Integer gradeLevel = jdbcTemplate.queryForObject(
                "select coalesce(max(grade_level), 0) + 1000 "
                        + "from job_grade",
                Integer.class);

        jdbcTemplate.update("""
                insert into department
                    (department_code, department_name,
                     department_status, display_order)
                values (?, ?, 'ACTIVE', 0)
                """,
                departmentCode,
                "Manager Education Integration Department");

        departmentId = queryLong(
                "select department_id from department "
                        + "where department_code = ?",
                departmentCode);

        jdbcTemplate.update("""
                insert into job_grade
                    (grade_code, grade_name,
                     grade_level, is_active)
                values (?, ?, ?, true)
                """,
                gradeCode,
                "Manager Education Integration Grade",
                gradeLevel);

        jobGradeId = queryLong(
                "select job_grade_id from job_grade "
                        + "where grade_code = ?",
                gradeCode);
    }

    private TestEmployee createEmployee(
            String label,
            String employeeName
    ) {
        String email = "manager-education-it-"
                + label
                + "-"
                + suffix
                + "@example.com";

        String employeeNumber = "MEIT-"
                + employees.size()
                + "-"
                + suffix;

        jdbcTemplate.update("""
                insert into app_user
                    (email, password_hash,
                     account_status, failed_login_count)
                values (?, ?, 'ACTIVE', 0)
                """,
                email,
                "integration-test-password-not-used");

        Long appUserId = queryLong(
                "select app_user_id from app_user where email = ?",
                email);

        jdbcTemplate.update("""
                insert into employee
                    (app_user_id, department_id, job_grade_id,
                     employee_number, employee_name,
                     employee_type, hire_date, employment_status)
                values (?, ?, ?, ?, ?,
                        'GENERAL', current_date, 'EMPLOYED')
                """,
                appUserId,
                departmentId,
                jobGradeId,
                employeeNumber,
                employeeName);

        Long employeeId = queryLong(
                "select employee_id from employee "
                        + "where app_user_id = ?",
                appUserId);

        TestEmployee employee = new TestEmployee(
                appUserId,
                employeeId,
                employeeName);

        employees.add(employee);
        return employee;
    }

    private void grantRole(
            TestEmployee employee,
            Long roleId
    ) {
        jdbcTemplate.update("""
                insert into user_role
                    (app_user_id, role_id, granted_by)
                values (?, ?, ?)
                """,
                employee.appUserId(),
                roleId,
                employee.appUserId());
    }

    private void createManagerRelation(
            TestEmployee managerEmployee,
            TestEmployee managedEmployee
    ) {
        jdbcTemplate.update("""
                insert into manager_relation
                    (employee_id, manager_employee_id,
                     relation_type, started_at,
                     ended_at, relation_status, created_by)
                values (?, ?, 'DIRECT', ?, null, 'ACTIVE', ?)
                """,
                managedEmployee.employeeId(),
                managerEmployee.employeeId(),
                LocalDateTime.now().minusDays(30),
                managerEmployee.appUserId());
    }

    private void createEducationFixture() {
        LocalDate today = LocalDate.now();

        jdbcTemplate.update("""
                insert into course
                    (course_name, course_description,
                     is_required,
                     training_start_date, training_end_date,
                     publication_status, created_by)
                values (?, ?, true, ?, ?, 'PUBLIC', ?)
                """,
                "Manager Education Integration Course " + suffix,
                "Manager education integration test course",
                today.minusDays(30),
                today.plusDays(30),
                manager.appUserId());

        courseId = queryLong(
                "select course_id from course where course_name = ?",
                "Manager Education Integration Course " + suffix);

        createEnrollment(
                managedOne,
                "IN_PROGRESS",
                new BigDecimal("40.00"),
                today.minusDays(10),
                today.minusDays(1),
                null);

        createEnrollment(
                managedTwo,
                "COMPLETED",
                new BigDecimal("100.00"),
                today.minusDays(10),
                today.plusDays(10),
                LocalDateTime.now().minusDays(1));

        createEnrollment(
                regularEmployee,
                "IN_PROGRESS",
                new BigDecimal("60.00"),
                today.minusDays(10),
                today.plusDays(10),
                null);
    }

    private void createEnrollment(
            TestEmployee employee,
            String status,
            BigDecimal progressRate,
            LocalDate startDate,
            LocalDate dueDate,
            LocalDateTime completedAt
    ) {
        String enrollmentRound = "MEIT-" + suffix;

        jdbcTemplate.update("""
                insert into course_assignment
                    (course_id, target_type,
                     employee_id, department_id, job_grade_id,
                     is_new_employee_target,
                     enrollment_round,
                     enrollment_start_date, enrollment_due_date,
                     assigned_by)
                values (?, 'EMPLOYEE',
                        ?, null, null,
                        false,
                        ?, ?, ?, ?)
                """,
                courseId,
                employee.employeeId(),
                enrollmentRound,
                startDate,
                dueDate,
                manager.appUserId());

        Long assignmentId = queryLong("""
                select course_assignment_id
                from course_assignment
                where course_id = ?
                  and employee_id = ?
                  and enrollment_round = ?
                """,
                courseId,
                employee.employeeId(),
                enrollmentRound);

        jdbcTemplate.update("""
                insert into course_enrollment
                    (course_id, employee_id,
                     course_assignment_id, enrollment_round,
                     enrollment_status, progress_rate,
                     enrollment_start_date, enrollment_due_date,
                     completed_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                courseId,
                employee.employeeId(),
                assignmentId,
                enrollmentRound,
                status,
                progressRate,
                startDate,
                dueDate,
                completedAt);
    }

    private Long ensureRole(
            String roleCode,
            String roleName
    ) {
        List<Long> roleIds = jdbcTemplate.queryForList(
                "select role_id from role where role_code = ?",
                Long.class,
                roleCode);

        if (roleIds.isEmpty()) {
            jdbcTemplate.update("""
                    insert into role
                        (role_code, role_name,
                         role_description, is_active)
                    values (?, ?, ?, true)
                    """,
                    roleCode,
                    roleName,
                    "Manager education integration test role");

            Long createdRoleId = queryLong(
                    "select role_id from role where role_code = ?",
                    roleCode);

            createdRoleIds.add(createdRoleId);
            return createdRoleId;
        }

        Long roleId = roleIds.get(0);
        Boolean active = jdbcTemplate.queryForObject(
                "select is_active from role where role_id = ?",
                Boolean.class,
                roleId);

        assertTrue(
                Boolean.TRUE.equals(active),
                roleCode + " role must be active");

        return roleId;
    }

    private String bearerToken(TestEmployee employee) {
        return "Bearer "
                + accessTokenService.issue(employee.appUserId());
    }

    private Long queryLong(
            String sql,
            Object... arguments
    ) {
        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                arguments);
    }

    private record TestEmployee(
            Long appUserId,
            Long employeeId,
            String employeeName
    ) {
    }
}
