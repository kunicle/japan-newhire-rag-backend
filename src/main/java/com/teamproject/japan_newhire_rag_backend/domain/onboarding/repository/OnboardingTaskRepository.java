package com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingTask;

public interface OnboardingTaskRepository
        extends JpaRepository<OnboardingTask, Long> {

    Page<OnboardingTask> findByActiveTrue(Pageable pageable);
}
