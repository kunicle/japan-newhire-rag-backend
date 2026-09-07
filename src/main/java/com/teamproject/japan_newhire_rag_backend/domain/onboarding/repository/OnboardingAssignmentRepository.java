package com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingAssignment;

public interface OnboardingAssignmentRepository
    extends JpaRepository<OnboardingAssignment, Long> {

    List<OnboardingAssignment>
            findByOnboardingTask_OnboardingTaskIdAndEmployeeIdIn(
                    Long onboardingTaskId,
                    Collection<Long> employeeIds);
}