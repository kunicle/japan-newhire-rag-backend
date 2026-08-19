package com.teamproject.japan_newhire_rag_backend.domain.education.entity;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.LearningCompletionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "learning_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_learning_progress", columnNames = {
                "course_enrollment_id", "course_module_id"
        })
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LearningProgress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_progress_id")
    private Long learningProgressId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_enrollment_id", nullable = false)
    private CourseEnrollment courseEnrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_module_id", nullable = false)
    private CourseModule courseModule;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status", nullable = false, length = 20)
    private LearningCompletionStatus completionStatus = LearningCompletionStatus.NOT_STARTED;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static LearningProgress create(
        CourseEnrollment courseEnrollment,
        CourseModule courseModule
    ) {
        LearningProgress progress = new LearningProgress();
        progress.courseEnrollment = courseEnrollment;
        progress.courseModule = courseModule;
        progress.completionStatus = LearningCompletionStatus.NOT_STARTED;
        progress.startedAt = null;
        progress.completedAt = null;
        return progress;
    }
}
