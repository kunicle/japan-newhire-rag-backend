package com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    @EntityGraph(attributePaths = {
            "onboardingAssignment",
            "onboardingAssignment.onboardingTask"
    })
    @Query("select progress from OnboardingProgress progress")
    Page<OnboardingProgress> findAllWithAssignment(
            Pageable pageable);

    @EntityGraph(attributePaths = {
            "onboardingAssignment",
            "onboardingAssignment.onboardingTask"
    })
    Page<OnboardingProgress>
            findAllByOnboardingAssignment_EmployeeId(
                    Long employeeId,
                    Pageable pageable);

    @EntityGraph(attributePaths = {
            "onboardingAssignment",
            "onboardingAssignment.onboardingTask"
    })
    Page<OnboardingProgress>
            findAllByOnboardingAssignment_EmployeeIdIn(
                    Collection<Long> employeeIds,
                    Pageable pageable);
}
