package com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class OnboardingTaskTest {

    @Test
    void createsActiveOnboardingTask() {
        OnboardingTask task = OnboardingTask.create(
                10L,
                "Submit employee documents",
                "Submit all required employee documents.",
                7,
                100L);

        assertThat(task.getDepartmentId()).isEqualTo(10L);
        assertThat(task.getTaskTitle())
                .isEqualTo("Submit employee documents");
        assertThat(task.getTaskDescription())
                .isEqualTo("Submit all required employee documents.");
        assertThat(task.getDefaultDueDays()).isEqualTo(7);
        assertThat(task.isActive()).isTrue();
        assertThat(task.getCreatedBy()).isEqualTo(100L);
    }

    @Test
    void updatesOnboardingTaskInformation() {
        OnboardingTask task = OnboardingTask.create(
                10L,
                "Original title",
                "Original description",
                7,
                100L);

        task.update(
                20L,
                "Updated title",
                "Updated description",
                14);

        assertThat(task.getDepartmentId()).isEqualTo(20L);
        assertThat(task.getTaskTitle())
                .isEqualTo("Updated title");
        assertThat(task.getTaskDescription())
                .isEqualTo("Updated description");
        assertThat(task.getDefaultDueDays()).isEqualTo(14);
        assertThat(task.getCreatedBy()).isEqualTo(100L);
    }

    @Test
    void changesActivationIdempotently() {
        OnboardingTask task = OnboardingTask.create(
                10L,
                "Task title",
                "Task description",
                7,
                100L);

        assertThat(task.changeActivation(false)).isTrue();
        assertThat(task.isActive()).isFalse();

        assertThat(task.changeActivation(false)).isFalse();
        assertThat(task.isActive()).isFalse();

        assertThat(task.changeActivation(true)).isTrue();
        assertThat(task.isActive()).isTrue();
    }

    @Test
    void rejectsNonPositiveDefaultDueDays() {
        assertThatThrownBy(() -> OnboardingTask.create(
                10L,
                "Task title",
                "Task description",
                0,
                100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Default due days must be positive");
    }

    @Test
    void rejectsMissingRequiredValues() {
        assertThatThrownBy(() -> OnboardingTask.create(
                null,
                "Task title",
                "Task description",
                7,
                100L))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Department ID is required");

        assertThatThrownBy(() -> OnboardingTask.create(
                10L,
                null,
                "Task description",
                7,
                100L))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Task title is required");

        assertThatThrownBy(() -> OnboardingTask.create(
                10L,
                "Task title",
                "Task description",
                7,
                null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Creator ID is required");
    }
}