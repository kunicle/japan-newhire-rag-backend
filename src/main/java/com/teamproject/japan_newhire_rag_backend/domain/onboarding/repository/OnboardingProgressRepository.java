package com.teamproject.japan_newhire_rag_backend.domain.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity.OnboardingProgress;

public interface OnboardingProgressRepository extends JpaRepository<OnboardingProgress, Long> {
}
