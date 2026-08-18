package com.teamproject.japan_newhire_rag_backend.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.DepartmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmploymentStatus;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false"
})
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class EvaluationRepositoryIntegrationTest {

    @Autowired EntityManager entityManager;
    @Autowired EvaluationCycleRepository cycleRepository;
    @Autowired EvaluationTemplateRepository templateRepository;
    @Autowired EvaluationItemRepository itemRepository;
    @Autowired EvaluationRepository evaluationRepository;
    @Autowired EvaluationScoreRepository scoreRepository;
    @Autowired EvaluationFeedbackRepository feedbackRepository;
    @Autowired EvaluationPublishHistoryRepository historyRepository;

    private AppUser actor;
    private Employee target;
    private Employee manager;

    @BeforeEach
    void setUpOrganizationFixtures() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Department department = newEntity(Department.class);
        set(department, "departmentCode", "D-" + suffix);
        set(department, "departmentName", "D integration department");
        set(department, "departmentStatus", DepartmentStatus.ACTIVE);
        set(department, "displayOrder", 1);
        entityManager.persist(department);

        JobGrade grade = newEntity(JobGrade.class);
        set(grade, "gradeCode", "D-" + suffix);
        set(grade, "gradeName", "D integration grade");
        set(grade, "gradeLevel", 2_000_000 + Math.abs(suffix.hashCode() % 1_000_000));
        set(grade, "isActive", true);
        entityManager.persist(grade);

        actor = persistUser("d-actor-" + suffix + "@example.com");
        AppUser targetUser = persistUser("d-target-" + suffix + "@example.com");
        target = persistEmployee(targetUser, department, grade, "D-T-" + suffix);
        manager = persistEmployee(actor, department, grade, "D-M-" + suffix);
        entityManager.flush();
    }

    @Test
    void allRepositoriesPersistMapAndQueryAgainstMySql() {
        EvaluationCycle cycle = cycleRepository.saveAndFlush(new EvaluationCycle(
                "2026 integration", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 7, 1), EvaluationCycleStatus.CLOSED, actor.getAppUserId()));
        EvaluationTemplate selfTemplate = templateRepository.saveAndFlush(new EvaluationTemplate(
                cycle.getEvaluationCycleId(), "Self", EvaluationType.SELF, "self template", true,
                actor.getAppUserId()));
        EvaluationTemplate managerTemplate = templateRepository.saveAndFlush(new EvaluationTemplate(
                cycle.getEvaluationCycleId(), "Manager", EvaluationType.MANAGER, null, true,
                actor.getAppUserId()));
        EvaluationItem second = itemRepository.saveAndFlush(new EvaluationItem(
                selfTemplate.getEvaluationTemplateId(), "Second", null, 2, new BigDecimal("40.00"),
                true, 1, 5));
        EvaluationItem first = itemRepository.saveAndFlush(new EvaluationItem(
                selfTemplate.getEvaluationTemplateId(), "First", "mapped", 1,
                new BigDecimal("60.00"), true, 1, 5));

        LocalDateTime draftSavedAt = LocalDateTime.of(2026, 6, 1, 9, 30);
        Evaluation self = new Evaluation(cycle.getEvaluationCycleId(), selfTemplate.getEvaluationTemplateId(),
                target.getEmployeeId(), target.getEmployeeId(), EvaluationType.SELF,
                EvaluationStatus.DRAFT, null, null, null);
        self.setLastDraftSavedAt(draftSavedAt);
        self = evaluationRepository.saveAndFlush(self);
        Evaluation managerEvaluation = evaluationRepository.saveAndFlush(new Evaluation(
                cycle.getEvaluationCycleId(), managerTemplate.getEvaluationTemplateId(),
                target.getEmployeeId(), manager.getEmployeeId(), EvaluationType.MANAGER,
                EvaluationStatus.SUBMITTED, new BigDecimal("4.50"), draftSavedAt, null));

        EvaluationScore score = scoreRepository.saveAndFlush(new EvaluationScore(
                self.getEvaluationId(), first.getEvaluationItemId(), new BigDecimal("4.0")));
        EvaluationFeedback visible = feedbackRepository.saveAndFlush(new EvaluationFeedback(
                managerEvaluation.getEvaluationId(), first.getEvaluationItemId(), FeedbackType.ITEM,
                "visible", true));
        feedbackRepository.saveAndFlush(new EvaluationFeedback(managerEvaluation.getEvaluationId(), null,
                FeedbackType.OVERALL, "hidden", false));
        LocalDateTime publishedAt = LocalDateTime.of(2026, 7, 2, 10, 0);
        EvaluationPublishHistory history = historyRepository.saveAndFlush(new EvaluationPublishHistory(
                managerEvaluation.getEvaluationId(), actor.getAppUserId(), EvaluationStatus.SUBMITTED,
                EvaluationStatus.PUBLISHED, "integration", publishedAt));
        entityManager.clear();

        EvaluationCycle foundCycle = cycleRepository.findById(cycle.getEvaluationCycleId()).orElseThrow();
        assertThat(foundCycle.getCycleName()).isEqualTo("2026 integration");
        assertThat(foundCycle.getCycleStatus()).isEqualTo(EvaluationCycleStatus.CLOSED);
        assertThat(templateRepository.findByEvaluationCycleIdAndEvaluationTypeAndIsActiveTrue(
                cycle.getEvaluationCycleId(), EvaluationType.SELF)).isPresent();
        assertThat(templateRepository.findByEvaluationCycleIdOrderByEvaluationTypeAsc(cycle.getEvaluationCycleId()))
                .extracting(EvaluationTemplate::getEvaluationType)
                .containsExactly(EvaluationType.MANAGER, EvaluationType.SELF);
        assertThat(itemRepository.findByEvaluationTemplateIdOrderByItemOrderAsc(
                selfTemplate.getEvaluationTemplateId()))
                .extracting(EvaluationItem::getEvaluationItemId)
                .containsExactly(first.getEvaluationItemId(), second.getEvaluationItemId());
        assertThat(evaluationRepository.findByEvaluationCycleId(cycle.getEvaluationCycleId())).hasSize(2);
        assertThat(evaluationRepository.findByEvaluationCycleIdAndTargetEmployeeIdAndEvaluationTypeIn(
                cycle.getEvaluationCycleId(), target.getEmployeeId(), List.of(EvaluationType.SELF)))
                .singleElement().satisfies(row -> assertThat(row.getLastDraftSavedAt()).isEqualTo(draftSavedAt));
        assertThat(evaluationRepository.findByEvaluatorEmployeeIdAndEvaluationType(
                manager.getEmployeeId(), EvaluationType.MANAGER)).hasSize(1);
        assertThat(scoreRepository.findByEvaluationIdAndEvaluationItemId(
                self.getEvaluationId(), first.getEvaluationItemId()))
                .hasValueSatisfying(found -> {
                    assertThat(found.getEvaluationScoreId()).isEqualTo(score.getEvaluationScoreId());
                    assertThat(found.getScore()).isEqualByComparingTo("4.0");
                });
        assertThat(scoreRepository.findByEvaluationId(self.getEvaluationId())).hasSize(1);
        assertThat(feedbackRepository.findByEvaluationIdAndIsVisibleToEmployeeTrue(
                managerEvaluation.getEvaluationId()))
                .extracting(EvaluationFeedback::getEvaluationFeedbackId)
                .containsExactly(visible.getEvaluationFeedbackId());
        EvaluationPublishHistory foundHistory = historyRepository.findById(
                history.getEvaluationPublishHistoryId()).orElseThrow();
        assertThat(foundHistory.getPublishedBy()).isEqualTo(actor.getAppUserId());
        assertThat(foundHistory.getPreviousStatus()).isEqualTo(EvaluationStatus.SUBMITTED);
        assertThat(foundHistory.getPublishedStatus()).isEqualTo(EvaluationStatus.PUBLISHED);
        assertThat(foundHistory.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    void mysqlEnforcesRepresentativeForeignKeyUniqueAndCheckConstraints() {
        EvaluationCycle cycle = cycleRepository.saveAndFlush(new EvaluationCycle(
                "constraints", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 7, 1), EvaluationCycleStatus.CLOSED, actor.getAppUserId()));
        EvaluationTemplate template = templateRepository.saveAndFlush(new EvaluationTemplate(
                cycle.getEvaluationCycleId(), "Self", EvaluationType.SELF, null, true,
                actor.getAppUserId()));
        EvaluationItem item = itemRepository.saveAndFlush(new EvaluationItem(
                template.getEvaluationTemplateId(), "Item", null, 1, BigDecimal.ONE, true, 1, 5));
        Evaluation evaluation = evaluationRepository.saveAndFlush(new Evaluation(
                cycle.getEvaluationCycleId(), template.getEvaluationTemplateId(), target.getEmployeeId(),
                target.getEmployeeId(), EvaluationType.SELF, EvaluationStatus.DRAFT, null, null, null));
        scoreRepository.saveAndFlush(new EvaluationScore(
                evaluation.getEvaluationId(), item.getEvaluationItemId(), new BigDecimal("4.0")));

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                insert into evaluation_template
                    (evaluation_cycle_id, template_name, evaluation_type, is_active, created_by)
                values (:cycleId, 'duplicate', 'SELF', true, :actorId)
                """).setParameter("cycleId", cycle.getEvaluationCycleId())
                .setParameter("actorId", actor.getAppUserId()).executeUpdate())
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(java.sql.SQLIntegrityConstraintViolationException.class);

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                insert into evaluation_item
                    (evaluation_template_id, item_name, item_order, weight, is_required, minimum_score, maximum_score)
                values (:templateId, 'duplicate order', 1, 1.00, true, 1, 5)
                """).setParameter("templateId", template.getEvaluationTemplateId()).executeUpdate())
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(java.sql.SQLIntegrityConstraintViolationException.class);

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                insert into evaluation_score (evaluation_id, evaluation_item_id, score)
                values (:evaluationId, :itemId, 3.0)
                """).setParameter("evaluationId", evaluation.getEvaluationId())
                .setParameter("itemId", item.getEvaluationItemId()).executeUpdate())
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(java.sql.SQLIntegrityConstraintViolationException.class);

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                insert into evaluation_score (evaluation_id, evaluation_item_id, score)
                values (:evaluationId, :itemId, 6.0)
                """).setParameter("evaluationId", evaluation.getEvaluationId())
                .setParameter("itemId", item.getEvaluationItemId()).executeUpdate())
                .isInstanceOf(RuntimeException.class);

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                insert into evaluation_item
                    (evaluation_template_id, item_name, item_order, weight, is_required, minimum_score, maximum_score)
                values (999999999, 'missing parent', 99, 1.00, true, 1, 5)
                """).executeUpdate())
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(java.sql.SQLIntegrityConstraintViolationException.class);

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                insert into evaluation_cycle
                    (cycle_name, start_date, end_date, planned_publish_date, cycle_status, created_by,
                     created_at, updated_at)
                values ('invalid dates', '2026-06-01', '2026-05-01', '2026-06-02', 'PLANNED',
                        :actorId, now(), now())
                """).setParameter("actorId", actor.getAppUserId()).executeUpdate())
                .isInstanceOf(RuntimeException.class);

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                insert into evaluation
                    (evaluation_cycle_id, evaluation_template_id, target_employee_id,
                     evaluator_employee_id, evaluation_type, evaluation_status)
                values (:cycleId, :templateId, :targetId, :managerId, 'SELF', 'DRAFT')
                """).setParameter("cycleId", cycle.getEvaluationCycleId())
                .setParameter("templateId", template.getEvaluationTemplateId())
                .setParameter("targetId", target.getEmployeeId())
                .setParameter("managerId", manager.getEmployeeId()).executeUpdate())
                .isInstanceOf(RuntimeException.class);

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                insert into evaluation_item
                    (evaluation_template_id, item_name, item_order, weight, is_required, minimum_score, maximum_score)
                values (:templateId, 'invalid weight', 2, 0.00, true, 1, 5)
                """).setParameter("templateId", template.getEvaluationTemplateId()).executeUpdate())
                .isInstanceOf(RuntimeException.class);
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
        set(employee, "employeeName", "D integration employee");
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
}
