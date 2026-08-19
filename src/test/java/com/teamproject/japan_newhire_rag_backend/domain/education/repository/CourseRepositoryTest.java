package com.teamproject.japan_newhire_rag_backend.domain.education.repository;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.config.JpaAuditingConfig;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseModule;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.LearningProgress;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.AssignmentTargetType;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.CoursePublicationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.LearningCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingProgress;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingTask;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingAssignmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingAssignmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingTaskRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.DepartmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmploymentStatus;

import jakarta.persistence.EntityManager;

@SpringBootTest(
        classes = CourseRepositoryTest.JpaTestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false"
        }
)
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class CourseRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseModuleRepository courseModuleRepository;

    @Autowired
    private CourseAssignmentRepository courseAssignmentRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private LearningProgressRepository learningProgressRepository;

    @Autowired
    private OnboardingTaskRepository onboardingTaskRepository;

    @Autowired
    private OnboardingAssignmentRepository onboardingAssignmentRepository;

    @Autowired
    private OnboardingProgressRepository onboardingProgressRepository;

    private AppUser appUser;
    private Employee employee;
    private Department department;

    @BeforeEach
    void setUpOrganizationFixtures() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        appUser = newEntity(AppUser.class);
        set(appUser, "email", "c-test-" + suffix + "@example.com");
        set(appUser, "passwordHash", "test-password-hash");
        set(appUser, "accountStatus", AccountStatus.ACTIVE);
        entityManager.persist(appUser);

        department = newEntity(Department.class);
        set(department, "departmentCode", "C-" + suffix);
        set(department, "departmentName", "C test department");
        set(department, "departmentStatus", DepartmentStatus.ACTIVE);
        set(department, "displayOrder", 1);
        entityManager.persist(department);

        JobGrade jobGrade = newEntity(JobGrade.class);
        set(jobGrade, "gradeCode", "C-" + suffix);
        set(jobGrade, "gradeName", "C test grade");
        set(jobGrade, "gradeLevel", 1000 + Math.abs(suffix.hashCode() % 1000000));
        set(jobGrade, "isActive", true);
        entityManager.persist(jobGrade);

        employee = newEntity(Employee.class);
        set(employee, "appUser", appUser);
        set(employee, "department", department);
        set(employee, "jobGrade", jobGrade);
        set(employee, "employeeNumber", "C-EMP-" + suffix);
        set(employee, "employeeName", "C test employee");
        set(employee, "employeeType", EmployeeType.NEW_HIRE);
        set(employee, "hireDate", LocalDate.of(2026, 8, 1));
        set(employee, "employmentStatus", EmploymentStatus.EMPLOYED);
        entityManager.persist(employee);
        entityManager.flush();
    }

    @Test
    void savesAndFindsCourseWithStringEnumAndDates() {
        Course course = createCourse();

        Course saved = courseRepository.saveAndFlush(course);
        entityManager.clear();

        Course found = courseRepository.findById(saved.getCourseId()).orElseThrow();
        String storedStatus = (String) entityManager.createNativeQuery(
                        "select publication_status from course where course_id = :courseId")
                .setParameter("courseId", saved.getCourseId())
                .getSingleResult();

        assertThat(found.getCourseName()).isEqualTo("New hire basic course");
        assertThat(found.getTrainingStartDate()).isEqualTo(LocalDate.of(2026, 8, 11));
        assertThat(found.getTrainingEndDate()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(found.getPublicationStatus()).isEqualTo(CoursePublicationStatus.DRAFT);
        assertThat(storedStatus).isEqualTo("DRAFT");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void savesCourseModuleAndItsCourseForeignKey() {
        Course course = courseRepository.saveAndFlush(createCourse());
        CourseModule module = newEntity(CourseModule.class);
        set(module, "course", course);
        set(module, "moduleTitle", "Company rules");
        set(module, "moduleContent", "Read the company rules.");
        set(module, "moduleOrder", 1);

        CourseModule saved = courseModuleRepository.saveAndFlush(module);
        entityManager.clear();

        CourseModule found = courseModuleRepository.findById(saved.getCourseModuleId()).orElseThrow();
        assertThat(found.getCourse().getCourseId()).isEqualTo(course.getCourseId());
        assertThat(found.isRequired()).isTrue();
        assertThat(found.isActive()).isTrue();
    }

    @Test
    void findsCourseModulesOrderedByModuleOrderIncludingInactive() {
        Course course = courseRepository.saveAndFlush(createCourse());

        CourseModule third = createModule(course, 3);
        CourseModule first = createModule(course, 1);
        CourseModule second = createModule(course, 2);
        set(second, "active", false);

        courseModuleRepository.saveAllAndFlush(
                List.of(third, first, second));
        entityManager.clear();

        List<CourseModule> found = courseModuleRepository
                .findAllByCourse_CourseIdOrderByModuleOrderAsc(
                        course.getCourseId());

        assertThat(found)
                .extracting(CourseModule::getModuleOrder)
                .containsExactly(1, 2, 3);
        assertThat(found)
                .extracting(CourseModule::isActive)
                .containsExactly(true, false, true);
    }

    @Test
    void findsOnlyActiveCourseModulesOrderedByModuleOrder() {
        Course course = courseRepository.saveAndFlush(createCourse());

        CourseModule third = createModule(course, 3);
        CourseModule first = createModule(course, 1);
        CourseModule second = createModule(course, 2);
        set(second, "active", false);

        courseModuleRepository.saveAllAndFlush(
                List.of(third, first, second));
        entityManager.clear();

        List<CourseModule> found = courseModuleRepository
                .findAllByCourse_CourseIdAndActiveTrueOrderByModuleOrderAsc(
                        course.getCourseId());

        assertThat(found)
                .extracting(CourseModule::getModuleOrder)
                .containsExactly(1, 3);

        assertThat(found)
                .allMatch(CourseModule::isActive);
    }

    @Test
    void findsExistingEnrollmentsByEmployeeIdsAndEnrollmentRound() {
        Course course = courseRepository.saveAndFlush(createCourse());

        CourseAssignment firstRoundAssignment = createAssignment(course);
        CourseAssignment secondRoundAssignment = createAssignment(course);
        set(secondRoundAssignment, "enrollmentRound", "2");

        courseAssignmentRepository.saveAllAndFlush(
                List.of(firstRoundAssignment, secondRoundAssignment));

        CourseEnrollment firstRoundEnrollment = createEnrollment(
                course,
                firstRoundAssignment,
                "1");

        CourseEnrollment secondRoundEnrollment = createEnrollment(
                course,
                secondRoundAssignment,
                "2");

        courseEnrollmentRepository.saveAllAndFlush(
                List.of(firstRoundEnrollment, secondRoundEnrollment));
        entityManager.clear();

        List<CourseEnrollment> found = courseEnrollmentRepository
                .findAllByCourse_CourseIdAndEmployeeIdInAndEnrollmentRound(
                        course.getCourseId(),
                        List.of(employee.getEmployeeId(), Long.MAX_VALUE),
                        "1");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getEmployeeId())
                .isEqualTo(employee.getEmployeeId());
        assertThat(found.get(0).getEnrollmentRound())
                .isEqualTo("1");
    }

    @Test
    void rejectsDuplicateEnrollmentForSameCourseEmployeeAndRound() {
        Course course = courseRepository.saveAndFlush(createCourse());
        CourseAssignment assignment =
                courseAssignmentRepository.saveAndFlush(createAssignment(course));

        CourseEnrollment firstEnrollment = createEnrollment(
                course,
                assignment,
                "1");

        courseEnrollmentRepository.saveAndFlush(firstEnrollment);

        CourseEnrollment duplicateEnrollment = createEnrollment(
                course,
                assignment,
                "1");

        assertThatThrownBy(() ->
                courseEnrollmentRepository.saveAndFlush(duplicateEnrollment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }


    @Test
    void savesEnrollmentProgressAndAllEducationRepositories() {
        Course course = courseRepository.saveAndFlush(createCourse());
        CourseModule module = courseModuleRepository.saveAndFlush(createModule(course));
        CourseAssignment assignment = courseAssignmentRepository.saveAndFlush(createAssignment(course));

        CourseEnrollment enrollment = newEntity(CourseEnrollment.class);
        set(enrollment, "course", course);
        set(enrollment, "employeeId", employee.getEmployeeId());
        set(enrollment, "courseAssignment", assignment);
        set(enrollment, "enrollmentRound", "1");
        set(enrollment, "enrollmentStatus", EnrollmentStatus.IN_PROGRESS);
        set(enrollment, "progressRate", new BigDecimal("25.50"));
        set(enrollment, "enrollmentStartDate", LocalDate.of(2026, 8, 11));
        set(enrollment, "enrollmentDueDate", LocalDate.of(2026, 8, 31));
        CourseEnrollment savedEnrollment = courseEnrollmentRepository.saveAndFlush(enrollment);

        LearningProgress progress = newEntity(LearningProgress.class);
        set(progress, "courseEnrollment", savedEnrollment);
        set(progress, "courseModule", module);
        set(progress, "completionStatus", LearningCompletionStatus.IN_PROGRESS);
        LearningProgress savedProgress = learningProgressRepository.saveAndFlush(progress);
        entityManager.clear();

        CourseEnrollment foundEnrollment = courseEnrollmentRepository
                .findById(savedEnrollment.getCourseEnrollmentId()).orElseThrow();
        assertThat(foundEnrollment.getProgressRate()).isEqualByComparingTo("25.50");
        assertThat(learningProgressRepository.findById(savedProgress.getLearningProgressId())).isPresent();
    }

    @Test
    void savesAndFindsOnboardingAssignmentAndProgress() {
        OnboardingTask task = newEntity(OnboardingTask.class);
        set(task, "departmentId", department.getDepartmentId());
        set(task, "taskTitle", "Submit profile");
        set(task, "taskDescription", "Submit the new-hire profile.");
        set(task, "defaultDueDays", 7);
        set(task, "createdBy", appUser.getAppUserId());
        OnboardingTask savedTask = onboardingTaskRepository.saveAndFlush(task);

        OnboardingAssignment assignment = newEntity(OnboardingAssignment.class);
        set(assignment, "onboardingTask", savedTask);
        set(assignment, "employeeId", employee.getEmployeeId());
        set(assignment, "assignedBy", appUser.getAppUserId());
        set(assignment, "assignedDate", LocalDate.of(2026, 8, 11));
        set(assignment, "dueDate", LocalDate.of(2026, 8, 18));
        set(assignment, "assignmentStatus", OnboardingAssignmentStatus.ASSIGNED);
        OnboardingAssignment savedAssignment = onboardingAssignmentRepository.saveAndFlush(assignment);

        OnboardingProgress progress = newEntity(OnboardingProgress.class);
        set(progress, "onboardingAssignment", savedAssignment);
        set(progress, "completionStatus", OnboardingCompletionStatus.NOT_STARTED);
        OnboardingProgress savedProgress = onboardingProgressRepository.saveAndFlush(progress);
        entityManager.clear();

        assertThat(onboardingAssignmentRepository.findById(savedAssignment.getOnboardingAssignmentId()))
                .isPresent();
        assertThat(onboardingProgressRepository.findById(savedProgress.getOnboardingProgressId()))
                .isPresent();
    }

    private Course createCourse() {
        Course course = newEntity(Course.class);
        set(course, "courseName", "New hire basic course");
        set(course, "courseDescription", "A basic onboarding course.");
        set(course, "required", true);
        set(course, "trainingStartDate", LocalDate.of(2026, 8, 11));
        set(course, "trainingEndDate", LocalDate.of(2026, 8, 31));
        set(course, "publicationStatus", CoursePublicationStatus.DRAFT);
        set(course, "createdBy", appUser.getAppUserId());
        return course;
    }

    private CourseModule createModule(Course course) {
        return createModule(course, 1);
    }

    private CourseModule createModule(Course course, int moduleOrder) {
        CourseModule module = newEntity(CourseModule.class);
        set(module, "course", course);
        set(module, "moduleTitle", "Module " + moduleOrder);
        set(module, "moduleContent", "Module content " + moduleOrder);
        set(module, "moduleOrder", moduleOrder);
        return module;
    }

    private CourseAssignment createAssignment(Course course) {
        CourseAssignment assignment = newEntity(CourseAssignment.class);
        set(assignment, "course", course);
        set(assignment, "targetType", AssignmentTargetType.EMPLOYEE);
        set(assignment, "employeeId", employee.getEmployeeId());
        set(assignment, "enrollmentRound", "1");
        set(assignment, "enrollmentStartDate", LocalDate.of(2026, 8, 11));
        set(assignment, "enrollmentDueDate", LocalDate.of(2026, 8, 31));
        set(assignment, "assignedBy", appUser.getAppUserId());
        return assignment;
    }

    private CourseEnrollment createEnrollment(
            Course course,
            CourseAssignment assignment,
            String enrollmentRound
    ) {
        CourseEnrollment enrollment = newEntity(CourseEnrollment.class);
        set(enrollment, "course", course);
        set(enrollment, "employeeId", employee.getEmployeeId());
        set(enrollment, "courseAssignment", assignment);
        set(enrollment, "enrollmentRound", enrollmentRound);
        set(enrollment, "enrollmentStatus", EnrollmentStatus.NOT_STARTED);
        set(enrollment, "progressRate", BigDecimal.ZERO);
        set(enrollment, "enrollmentStartDate", LocalDate.of(2026, 8, 11));
        set(enrollment, "enrollmentDueDate", LocalDate.of(2026, 8, 31));
        return enrollment;
    }

    private static <T> T newEntity(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not create test entity: " + type.getSimpleName(), exception);
        }
    }

    private static void set(Object target, String fieldName, Object value) {
        ReflectionTestUtils.setField(target, fieldName, value);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = {
            "com.teamproject.japan_newhire_rag_backend.domain.auth.entity",
            "com.teamproject.japan_newhire_rag_backend.domain.organization.entity",
            "com.teamproject.japan_newhire_rag_backend.domain.education.entity",
            "com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity"
    })
    @EnableJpaRepositories(basePackages = {
            "com.teamproject.japan_newhire_rag_backend.domain.education.repository",
            "com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository"
    })
    @Import(JpaAuditingConfig.class)
    static class JpaTestApplication {
    }
}
