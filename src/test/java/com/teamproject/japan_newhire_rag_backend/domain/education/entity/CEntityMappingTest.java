package com.teamproject.japan_newhire_rag_backend.domain.education.entity;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseAssignmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseModuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.LearningProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingProgress;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingTask;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingAssignmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository.OnboardingTaskRepository;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

class CEntityMappingTest {

    private static final List<Class<?>> ENTITIES = List.of(
            Course.class,
            CourseModule.class,
            CourseAssignment.class,
            CourseEnrollment.class,
            LearningProgress.class,
            OnboardingTask.class,
            OnboardingAssignment.class,
            OnboardingProgress.class);

    @Test
    void allEightEntitiesUseTablesAndColumnsDeclaredInTheCddl() throws IOException {
        String ddl = new ClassPathResource("db/ddl/c-domain-schema.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        for (Class<?> entityType : ENTITIES) {
            Table table = entityType.getAnnotation(Table.class);
            assertThat(table).as(entityType.getSimpleName() + " @Table").isNotNull();
            assertThat(ddl).contains("CREATE TABLE IF NOT EXISTS " + table.name());

            for (Field field : entityType.getDeclaredFields()) {
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    assertThat(ddl).as(entityType.getSimpleName() + "." + field.getName())
                            .contains(column.name());
                }
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                if (joinColumn != null) {
                    assertThat(ddl).as(entityType.getSimpleName() + "." + field.getName())
                            .contains(joinColumn.name());
                }
            }
        }
    }

    @Test
    void everyEnumFieldIsStoredAsAString() {
        for (Class<?> entityType : ENTITIES) {
            for (Field field : entityType.getDeclaredFields()) {
                if (field.getType().isEnum()) {
                    Enumerated enumerated = field.getAnnotation(Enumerated.class);
                    assertThat(enumerated).as(entityType.getSimpleName() + "." + field.getName())
                            .isNotNull();
                    assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
                }
            }
        }
    }

    @Test
    void dateTimeDecimalAndExternalIdsUseTheRequiredJavaTypes() throws NoSuchFieldException {
        assertThat(Course.class.getDeclaredField("trainingStartDate").getType()).isEqualTo(LocalDate.class);
        assertThat(Course.class.getDeclaredField("deletedAt").getType()).isEqualTo(LocalDateTime.class);
        assertThat(CourseEnrollment.class.getDeclaredField("progressRate").getType()).isEqualTo(BigDecimal.class);

        assertThat(Course.class.getDeclaredField("createdBy").getType()).isEqualTo(Long.class);
        assertThat(CourseAssignment.class.getDeclaredField("employeeId").getType()).isEqualTo(Long.class);
        assertThat(CourseAssignment.class.getDeclaredField("departmentId").getType()).isEqualTo(Long.class);
        assertThat(CourseAssignment.class.getDeclaredField("jobGradeId").getType()).isEqualTo(Long.class);
        assertThat(CourseAssignment.class.getDeclaredField("assignedBy").getType()).isEqualTo(Long.class);
        assertThat(CourseEnrollment.class.getDeclaredField("employeeId").getType()).isEqualTo(Long.class);
        assertThat(OnboardingTask.class.getDeclaredField("departmentId").getType()).isEqualTo(Long.class);
        assertThat(OnboardingTask.class.getDeclaredField("createdBy").getType()).isEqualTo(Long.class);
        assertThat(OnboardingAssignment.class.getDeclaredField("employeeId").getType()).isEqualTo(Long.class);
        assertThat(OnboardingAssignment.class.getDeclaredField("assignedBy").getType()).isEqualTo(Long.class);
    }

    @Test
    void allEightRepositoriesAreJpaRepositories() {
        assertThat(List.of(
                CourseRepository.class,
                CourseModuleRepository.class,
                CourseAssignmentRepository.class,
                CourseEnrollmentRepository.class,
                LearningProgressRepository.class,
                OnboardingTaskRepository.class,
                OnboardingAssignmentRepository.class,
                OnboardingProgressRepository.class))
                .allMatch(JpaRepository.class::isAssignableFrom);
    }
}
