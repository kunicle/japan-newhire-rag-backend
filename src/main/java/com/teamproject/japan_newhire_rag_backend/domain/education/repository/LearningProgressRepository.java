package com.teamproject.japan_newhire_rag_backend.domain.education.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.LearningProgress;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.LearningCompletionStatus;

public interface LearningProgressRepository
        extends JpaRepository<LearningProgress, Long> {

    @EntityGraph(attributePaths = "courseModule")
    List<LearningProgress>
    findAllByCourseEnrollment_CourseEnrollmentIdAndCourseModule_ActiveTrueOrderByCourseModule_ModuleOrderAsc(
            Long courseEnrollmentId);

    @EntityGraph(attributePaths = {
            "courseEnrollment",
            "courseEnrollment.course",
            "courseModule"
    })
    Optional<LearningProgress> findByLearningProgressId(
            Long learningProgressId);

    long countByCourseEnrollment_CourseEnrollmentIdAndCourseModule_RequiredTrueAndCourseModule_ActiveTrueAndCompletionStatus(
            Long courseEnrollmentId,
            LearningCompletionStatus completionStatus);
}