package com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingProgress;

public interface OnboardingProgressRepository
        extends JpaRepository<OnboardingProgress, Long> {

    @EntityGraph(attributePaths = {
            "onboardingAssignment",
            "onboardingAssignment.onboardingTask"
    })
    List<OnboardingProgress>
            findByOnboardingAssignment_EmployeeIdOrderByOnboardingAssignment_DueDateAsc(
                    Long employeeId);

    @EntityGraph(attributePaths = {
            "onboardingAssignment",
            "onboardingAssignment.onboardingTask"
    })
    Optional<OnboardingProgress>
            findByOnboardingAssignment_OnboardingAssignmentId(
                    Long onboardingAssignmentId);
}