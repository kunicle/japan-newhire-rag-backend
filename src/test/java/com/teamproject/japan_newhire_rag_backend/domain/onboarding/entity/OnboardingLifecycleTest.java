package com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingAssignmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingCompletionStatus;

class OnboardingLifecycleTest {

    @Test
    void createsAssignmentAndProgressWithInitialStates() {
        OnboardingAssignment assignment =
                createAssignment();

        OnboardingProgress progress =
                OnboardingProgress.create(assignment);

        assertThat(assignment.getEmployeeId())
                .isEqualTo(200L);
        assertThat(assignment.getAssignedBy())
                .isEqualTo(100L);
        assertThat(assignment.getAssignedDate())
                .isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(assignment.getDueDate())
                .isEqualTo(LocalDate.of(2026, 8, 27));
        assertThat(assignment.getAssignmentStatus())
                .isEqualTo(
                        OnboardingAssignmentStatus.ASSIGNED);

        assertThat(progress.getOnboardingAssignment())
                .isSameAs(assignment);
        assertThat(progress.getCompletionStatus())
                .isEqualTo(
                        OnboardingCompletionStatus.NOT_STARTED);
        assertThat(progress.getCompletionNote()).isNull();
        assertThat(progress.getCompletedAt()).isNull();
    }

    @Test
    void cancelsAssignedAssignmentIdempotently() {
        OnboardingAssignment assignment =
                createAssignment();

        assertThat(assignment.cancel()).isTrue();
        assertThat(assignment.getAssignmentStatus())
                .isEqualTo(
                        OnboardingAssignmentStatus.CANCELLED);

        assertThat(assignment.cancel()).isFalse();
        assertThat(assignment.getAssignmentStatus())
                .isEqualTo(
                        OnboardingAssignmentStatus.CANCELLED);
    }

    @Test
    void completedAssignmentCannotBeCancelled() {
        OnboardingAssignment assignment =
                createAssignment();

        assignment.complete();

        assertThatThrownBy(assignment::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Completed assignment cannot be cancelled");
    }

    @Test
    void cancelledAssignmentCannotBeCompleted() {
        OnboardingAssignment assignment =
                createAssignment();

        assignment.cancel();

        assertThatThrownBy(assignment::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Cancelled assignment cannot be completed");
    }

    @Test
    void startsProgressIdempotently() {
        OnboardingProgress progress =
                OnboardingProgress.create(
                        createAssignment());

        assertThat(progress.start()).isTrue();
        assertThat(progress.getCompletionStatus())
                .isEqualTo(
                        OnboardingCompletionStatus.IN_PROGRESS);

        assertThat(progress.start()).isFalse();
        assertThat(progress.getCompletionStatus())
                .isEqualTo(
                        OnboardingCompletionStatus.IN_PROGRESS);
    }

    @Test
    void completesStartedProgressIdempotently() {
        OnboardingProgress progress =
                OnboardingProgress.create(
                        createAssignment());
        LocalDateTime completionTime =
                LocalDateTime.of(
                        2026, 8, 20, 10, 30);

        progress.start();

        assertThat(progress.complete(
                "Finished",
                completionTime)).isTrue();

        assertThat(progress.getCompletionStatus())
                .isEqualTo(
                        OnboardingCompletionStatus.COMPLETED);
        assertThat(progress.getCompletionNote())
                .isEqualTo("Finished");
        assertThat(progress.getCompletedAt())
                .isEqualTo(completionTime);

        assertThat(progress.complete(
                "Changed note",
                completionTime.plusHours(1))).isFalse();

        assertThat(progress.getCompletionNote())
                .isEqualTo("Finished");
        assertThat(progress.getCompletedAt())
                .isEqualTo(completionTime);
    }

    @Test
    void progressCannotCompleteBeforeStart() {
        OnboardingProgress progress =
                OnboardingProgress.create(
                        createAssignment());

        assertThatThrownBy(() -> progress.complete(
                "Finished",
                LocalDateTime.of(
                        2026, 8, 20, 10, 30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Onboarding progress must be started before completion");
    }

    @Test
    void rejectsDueDateBeforeAssignedDate() {
        OnboardingTask task = createTask();

        assertThatThrownBy(() ->
                OnboardingAssignment.create(
                        task,
                        200L,
                        100L,
                        LocalDate.of(2026, 8, 20),
                        LocalDate.of(2026, 8, 19)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Due date must not be before assigned date");
    }

    private OnboardingAssignment createAssignment() {
        return OnboardingAssignment.create(
                createTask(),
                200L,
                100L,
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 27));
    }

    private OnboardingTask createTask() {
        return OnboardingTask.create(
                10L,
                "Submit documents",
                "Submit required documents.",
                7,
                100L);
    }
}