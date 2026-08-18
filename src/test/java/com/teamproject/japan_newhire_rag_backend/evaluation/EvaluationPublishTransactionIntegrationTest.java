package com.teamproject.japan_newhire_rag_backend.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.DepartmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmploymentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.internal.AuditLogRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.internal.AuditLogRecordService;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationPublishService;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false"
})
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class EvaluationPublishTransactionIntegrationTest {

    @Autowired EvaluationPublishService publishService;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    @MockitoBean CurrentUserProvider currentUserProvider;
    @MockitoSpyBean AuditLogRecordService auditLogRecordService;
    @MockitoSpyBean EvaluationPublishHistoryRepository historyRepository;

    private Fixture fixture;

    @BeforeEach
    void setUp() {
        fixture = transactionTemplate.execute(status -> createFixture());
        when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUserContext(
                fixture.actorUserId(), fixture.managerEmployeeId(), Set.of(RoleType.HR_MANAGER),
                fixture.departmentId(), 100, EmployeeType.GENERAL));
    }

    @AfterEach
    void cleanUp() {
        reset(auditLogRecordService, historyRepository, currentUserProvider);
        jdbcTemplate.update("delete from audit_log where actor_user_id = ?", fixture.actorUserId());
        jdbcTemplate.update("delete from evaluation_publish_history where evaluation_id in (?, ?)",
                fixture.selfEvaluationId(), fixture.managerEvaluationId());
        jdbcTemplate.update("delete from evaluation_feedback where evaluation_id in (?, ?)",
                fixture.selfEvaluationId(), fixture.managerEvaluationId());
        jdbcTemplate.update("delete from evaluation where evaluation_cycle_id = ?", fixture.cycleId());
        jdbcTemplate.update("delete from evaluation_item where evaluation_template_id in (?, ?)",
                fixture.selfTemplateId(), fixture.managerTemplateId());
        jdbcTemplate.update("delete from evaluation_template where evaluation_cycle_id = ?", fixture.cycleId());
        jdbcTemplate.update("delete from evaluation_cycle where evaluation_cycle_id = ?", fixture.cycleId());
        jdbcTemplate.update("delete from employee where employee_id in (?, ?)",
                fixture.targetEmployeeId(), fixture.managerEmployeeId());
        jdbcTemplate.update("delete from app_user where app_user_id in (?, ?)",
                fixture.targetUserId(), fixture.actorUserId());
        jdbcTemplate.update("delete from department where department_id = ?", fixture.departmentId());
        jdbcTemplate.update("delete from job_grade where job_grade_id = ?", fixture.jobGradeId());
    }

    @Test
    void normalPublishCommitsEvaluationsFeedbackHistoriesAndAuditWithOneTimestamp() {
        EvaluationPublishResponse response = publishService.publish(fixture.selfEvaluationId(),
                new EvaluationPublishRequest("integration publish", List.of(fixture.selectedFeedbackId())));

        List<EvaluationRow> evaluations = jdbcTemplate.query("""
                select evaluation_status, published_at from evaluation
                where evaluation_id in (?, ?) order by evaluation_id
                """, (rs, rowNum) -> new EvaluationRow(
                rs.getString("evaluation_status"), rs.getTimestamp("published_at").toLocalDateTime()),
                fixture.selfEvaluationId(), fixture.managerEvaluationId());
        assertThat(evaluations).extracting(EvaluationRow::status)
                .containsExactly("PUBLISHED", "PUBLISHED");
        LocalDateTime storedPublishedAt = evaluations.get(0).publishedAt();
        assertThat(evaluations).extracting(EvaluationRow::publishedAt).containsOnly(storedPublishedAt);
        assertThat(Math.abs(ChronoUnit.SECONDS.between(response.publishedAt(), storedPublishedAt)))
                .isLessThanOrEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from evaluation_feedback
                where evaluation_id = ? and is_visible_to_employee = true
                """, Integer.class, fixture.selfEvaluationId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from evaluation_feedback
                where evaluation_id = ? and is_visible_to_employee = true
                """, Integer.class, fixture.managerEvaluationId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                select is_visible_to_employee from evaluation_feedback where evaluation_feedback_id = ?
                """, Boolean.class, fixture.unselectedFeedbackId())).isFalse();
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from evaluation_publish_history where evaluation_id in (?, ?)
                """, Integer.class, fixture.selfEvaluationId(), fixture.managerEvaluationId())).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList("""
                select published_at from evaluation_publish_history where evaluation_id in (?, ?)
                """, LocalDateTime.class, fixture.selfEvaluationId(), fixture.managerEvaluationId()))
                .containsOnly(storedPublishedAt);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from audit_log where actor_user_id = ? and action_type = ? and target_id = ?
                """, Integer.class, fixture.actorUserId(),
                AuditActionType.EVALUATION_RESULT_PUBLISHED.name(), fixture.selfEvaluationId())).isEqualTo(1);
    }

    @Test
    void auditFailureRollsBackAllPublishChanges() {
        doThrow(new RuntimeException("forced audit failure"))
                .when(auditLogRecordService).record(any(AuditLogRecordCommand.class));

        assertThatThrownBy(() -> publishService.publish(fixture.selfEvaluationId(),
                new EvaluationPublishRequest("rollback", List.of(fixture.selectedFeedbackId()))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("forced audit failure");

        assertOriginalState();
    }

    @Test
    void historyFailureRollsBackEvaluationsAndFeedbackAndDoesNotCreateAudit() {
        doThrow(new RuntimeException("forced history failure"))
                .when(historyRepository).saveAll(any(Iterable.class));

        assertThatThrownBy(() -> publishService.publish(fixture.selfEvaluationId(),
                new EvaluationPublishRequest("rollback", List.of(fixture.selectedFeedbackId()))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("forced history failure");

        assertOriginalState();
    }

    private void assertOriginalState() {
        assertThat(jdbcTemplate.queryForList("""
                select evaluation_status from evaluation where evaluation_id in (?, ?) order by evaluation_id
                """, String.class, fixture.selfEvaluationId(), fixture.managerEvaluationId()))
                .containsExactly("SUBMITTED", "SUBMITTED");
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from evaluation where evaluation_id in (?, ?) and published_at is not null
                """, Integer.class, fixture.selfEvaluationId(), fixture.managerEvaluationId())).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from evaluation_feedback
                where evaluation_id in (?, ?) and is_visible_to_employee = true
                """, Integer.class, fixture.selfEvaluationId(), fixture.managerEvaluationId())).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from evaluation_publish_history where evaluation_id in (?, ?)
                """, Integer.class, fixture.selfEvaluationId(), fixture.managerEvaluationId())).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from audit_log where actor_user_id = ? and target_id = ?
                """, Integer.class, fixture.actorUserId(), fixture.selfEvaluationId())).isZero();
    }

    private Fixture createFixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Department department = newEntity(Department.class);
        set(department, "departmentCode", "DP-" + suffix);
        set(department, "departmentName", "Publish integration department");
        set(department, "departmentStatus", DepartmentStatus.ACTIVE);
        set(department, "displayOrder", 1);
        entityManager.persist(department);
        JobGrade grade = newEntity(JobGrade.class);
        set(grade, "gradeCode", "PG-" + suffix);
        set(grade, "gradeName", "Publish integration grade");
        set(grade, "gradeLevel", 3_000_000 + Math.abs(suffix.hashCode() % 1_000_000));
        set(grade, "isActive", true);
        entityManager.persist(grade);
        AppUser actor = persistUser("publish-actor-" + suffix + "@example.com");
        AppUser targetUser = persistUser("publish-target-" + suffix + "@example.com");
        Employee target = persistEmployee(targetUser, department, grade, "P-T-" + suffix);
        Employee manager = persistEmployee(actor, department, grade, "P-M-" + suffix);
        entityManager.flush();

        EvaluationCycle cycle = new EvaluationCycle("Publish", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30), LocalDate.of(2026, 7, 1), EvaluationCycleStatus.CLOSED,
                actor.getAppUserId());
        entityManager.persist(cycle);
        entityManager.flush();
        EvaluationTemplate selfTemplate = new EvaluationTemplate(cycle.getEvaluationCycleId(), "Self",
                EvaluationType.SELF, null, true, actor.getAppUserId());
        EvaluationTemplate managerTemplate = new EvaluationTemplate(cycle.getEvaluationCycleId(), "Manager",
                EvaluationType.MANAGER, null, true, actor.getAppUserId());
        entityManager.persist(selfTemplate);
        entityManager.persist(managerTemplate);
        entityManager.flush();
        EvaluationItem selfItem = new EvaluationItem(selfTemplate.getEvaluationTemplateId(), "Self item", null,
                1, java.math.BigDecimal.ONE, true, 1, 5);
        EvaluationItem managerItem = new EvaluationItem(managerTemplate.getEvaluationTemplateId(), "Manager item",
                null, 1, java.math.BigDecimal.ONE, true, 1, 5);
        entityManager.persist(selfItem);
        entityManager.persist(managerItem);
        entityManager.flush();
        LocalDateTime submittedAt = LocalDateTime.of(2026, 6, 30, 12, 0);
        Evaluation self = new Evaluation(cycle.getEvaluationCycleId(), selfTemplate.getEvaluationTemplateId(),
                target.getEmployeeId(), target.getEmployeeId(), EvaluationType.SELF,
                EvaluationStatus.SUBMITTED, null, submittedAt, null);
        Evaluation managerEvaluation = new Evaluation(cycle.getEvaluationCycleId(),
                managerTemplate.getEvaluationTemplateId(), target.getEmployeeId(), manager.getEmployeeId(),
                EvaluationType.MANAGER, EvaluationStatus.SUBMITTED, null, submittedAt, null);
        entityManager.persist(self);
        entityManager.persist(managerEvaluation);
        entityManager.flush();
        EvaluationFeedback selfFeedback = new EvaluationFeedback(self.getEvaluationId(), selfItem.getEvaluationItemId(),
                FeedbackType.ITEM, "self", false);
        EvaluationFeedback selected = new EvaluationFeedback(managerEvaluation.getEvaluationId(),
                managerItem.getEvaluationItemId(), FeedbackType.ITEM, "selected", false);
        EvaluationFeedback unselected = new EvaluationFeedback(managerEvaluation.getEvaluationId(), null,
                FeedbackType.OVERALL, "unselected", false);
        entityManager.persist(selfFeedback);
        entityManager.persist(selected);
        entityManager.persist(unselected);
        entityManager.flush();
        return new Fixture(actor.getAppUserId(), targetUser.getAppUserId(), department.getDepartmentId(),
                grade.getJobGradeId(), target.getEmployeeId(), manager.getEmployeeId(), cycle.getEvaluationCycleId(),
                selfTemplate.getEvaluationTemplateId(), managerTemplate.getEvaluationTemplateId(),
                self.getEvaluationId(), managerEvaluation.getEvaluationId(), selected.getEvaluationFeedbackId(),
                unselected.getEvaluationFeedbackId());
    }

    private AppUser persistUser(String email) {
        AppUser user = newEntity(AppUser.class);
        set(user, "email", email);
        set(user, "passwordHash", "integration-test-hash");
        set(user, "accountStatus", AccountStatus.ACTIVE);
        entityManager.persist(user);
        return user;
    }

    private Employee persistEmployee(AppUser user, Department department, JobGrade grade, String number) {
        Employee employee = newEntity(Employee.class);
        set(employee, "appUser", user);
        set(employee, "department", department);
        set(employee, "jobGrade", grade);
        set(employee, "employeeNumber", number);
        set(employee, "employeeName", "Publish integration employee");
        set(employee, "employeeType", EmployeeType.GENERAL);
        set(employee, "hireDate", LocalDate.of(2025, 1, 1));
        set(employee, "employmentStatus", EmploymentStatus.EMPLOYED);
        entityManager.persist(employee);
        return employee;
    }

    private static <T> T newEntity(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void set(Object target, String field, Object value) {
        ReflectionTestUtils.setField(target, field, value);
    }

    private record EvaluationRow(String status, LocalDateTime publishedAt) {}

    private record Fixture(Long actorUserId, Long targetUserId, Long departmentId, Long jobGradeId,
                           Long targetEmployeeId, Long managerEmployeeId, Long cycleId,
                           Long selfTemplateId, Long managerTemplateId, Long selfEvaluationId,
                           Long managerEvaluationId, Long selectedFeedbackId, Long unselectedFeedbackId) {}
}
