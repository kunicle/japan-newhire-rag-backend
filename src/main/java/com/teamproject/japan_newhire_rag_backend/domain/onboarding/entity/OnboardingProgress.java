package com.teamproject.japan_newhire_rag_backend.domain.onboarding.entity;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingCompletionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "onboarding_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_onboarding_progress_assignment", columnNames = "onboarding_assignment_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingProgress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "onboarding_progress_id")
    private Long onboardingProgressId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "onboarding_assignment_id", nullable = false)
    private OnboardingAssignment onboardingAssignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status", nullable = false, length = 20)
    private OnboardingCompletionStatus completionStatus = OnboardingCompletionStatus.NOT_STARTED;

    @Column(name = "completion_note", length = 1000)
    private String completionNote;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
