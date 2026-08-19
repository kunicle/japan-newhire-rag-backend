package com.teamproject.japan_newhire_rag_backend.domain.education.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseModule;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {

    List<CourseModule> findAllByCourse_CourseIdOrderByModuleOrderAsc(Long courseId);

    List<CourseModule> findAllByCourse_CourseIdAndActiveTrueOrderByModuleOrderAsc(
        Long courseId);

    boolean existsByCourse_CourseIdAndRequiredTrueAndActiveTrue(Long courseId);

    boolean existsByCourse_CourseIdAndModuleOrder(Long courseId, int moduleOrder);

    boolean existsByCourse_CourseIdAndModuleOrderAndCourseModuleIdNot(
            Long courseId,
            int moduleOrder,
            Long courseModuleId);

    Optional<CourseModule> findByCourseModuleIdAndCourse_DeletedAtIsNull(Long courseModuleId);

    boolean existsByCourse_CourseIdAndRequiredTrueAndActiveTrueAndCourseModuleIdNot(
            Long courseId,
            Long courseModuleId);

}
